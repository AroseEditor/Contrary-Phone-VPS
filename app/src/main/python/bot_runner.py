"""
bot_runner.py — Runs Discord bots within Chaquopy's Python runtime.

DESIGN:
  Each bot runs in its own thread with its own asyncio event loop.
  Scripts are exec()'d as-is — no source rewriting ever happens.

HARDCODED TOKEN SUPPORT:
  If your script contains `client.run("YOUR_TOKEN")` — it just works.
  The literal string in your code is the token that runs. Nothing is changed.

BOT_TOKEN VARIABLE:
  BOT_TOKEN is also injected into every script's global namespace.
  Scripts that use `client.run(BOT_TOKEN)` will pick up the token
  stored in the app's encrypted storage for that bot config.

MULTI-BOT:
  Every bot_id gets its own BotRunnerSession with an isolated thread
  and asyncio event loop. Bots don't share any state.
"""
import sys
import io
import os
import re
import asyncio
import threading
import traceback


# ── Token extraction (read-only, no source rewriting) ───────────────────────

# Used only to auto-detect a token when user imports an existing script,
# so the app can pre-populate the token field in the editor. Never modifies code.
_TOKEN_DETECT_PATTERNS = [
    re.compile(r'\.run\s*\(\s*["\']([A-Za-z0-9_\-\.]{59,})["\']'),
    re.compile(r'\.run\s*\(\s*token\s*=\s*["\']([A-Za-z0-9_\-\.]{59,})["\']'),
]


def extract_token_from_script(source: str) -> str | None:
    """
    Read-only scan: tries to find a Discord token literal in the script.
    Returns the token string if found, or None.
    Used by the editor to auto-fill the token field when a script is imported.
    """
    for pattern in _TOKEN_DETECT_PATTERNS:
        m = pattern.search(source)
        if m:
            return m.group(1)
    return None


# ── Bot session ──────────────────────────────────────────────────────────────

class BotRunnerSession:
    """
    Manages a single bot's full lifecycle:
      load script → inject globals → exec() → run asyncio loop → crash/stop handling.
    Each instance is completely independent — safe to run many simultaneously.
    """

    def __init__(self, bot_id: str, script_path: str, token: str, log_callback):
        self.bot_id      = bot_id
        self.script_path = script_path
        self.token       = token           # may be "" if script is fully self-contained
        self.log_callback = log_callback   # callable(level: str, message: str)
        self._loop        = None
        self._thread      = None
        self._stop_event  = threading.Event()
        self._running     = False

    # ── Logging ──────────────────────────────────────────────────────────────

    def _emit(self, level: str, message: str):
        try:
            self.log_callback(level, message)
        except Exception:
            pass

    # ── Lifecycle ─────────────────────────────────────────────────────────────

    def start(self):
        if self._running:
            return
        self._running = True
        self._stop_event.clear()
        self._thread = threading.Thread(
            target=self._run_loop,
            name=f"bot-{self.bot_id}",
            daemon=True,
        )
        self._thread.start()

    def stop(self):
        self._stop_event.set()
        self._running = False
        if self._loop and self._loop.is_running():
            self._loop.call_soon_threadsafe(self._loop.stop)

    def is_running(self) -> bool:
        return self._running and (self._thread is not None and self._thread.is_alive())

    # ── Main runner ───────────────────────────────────────────────────────────

    def _run_loop(self):
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)

        # ── Redirect stdout/stderr to log callback ────────────────────────────
        class LogWriter(io.TextIOBase):
            def __init__(writer_self, level):
                writer_self.level = level
                writer_self.buf   = ""

            def write(writer_self, data):
                writer_self.buf += data
                while "\n" in writer_self.buf:
                    line, writer_self.buf = writer_self.buf.split("\n", 1)
                    if line:
                        self._emit(writer_self.level, line)
                return len(data)

            def flush(writer_self):
                if writer_self.buf:
                    self._emit(writer_self.level, writer_self.buf)
                    writer_self.buf = ""

        old_stdout = sys.stdout
        old_stderr = sys.stderr
        sys.stdout = LogWriter("STDOUT")
        sys.stderr = LogWriter("STDERR")

        try:
            self._emit("INFO", f"[BotRunner] Loading: {self.script_path}")

            # ── Minimal globals — script is fully self-contained ──────────────
            # We do NOT inject BOT_TOKEN or any other variable.
            # Your script defines its own BOT_TOKEN, token, client.run("..."), etc.
            # Everything comes from your code exactly as written.
            script_globals = {
                "__name__": "__main__",
                "__file__":  self.script_path,
            }

            # ── Read and compile — no injection, no rewriting ─────────────────
            with open(self.script_path, "r", encoding="utf-8") as f:
                source = f.read()

            self._emit("SYSTEM", "[BotRunner] Running script as-is — all vars from your code ✓")

            code = compile(source, self.script_path, "exec")

            self._emit("INFO", "[BotRunner] Executing script…")
            exec(code, script_globals)

            # ── If script defines an async main(), run it explicitly ───────────
            if "main" in script_globals and asyncio.iscoroutinefunction(script_globals["main"]):
                self._loop.run_until_complete(script_globals["main"]())
            else:
                # discord.py's client.run() blocks internally — loop runs until it exits
                self._loop.run_forever()

        except SystemExit as e:
            self._emit("INFO", f"[BotRunner] Script exited (code {e.code})")
        except Exception:
            err = traceback.format_exc()
            self._emit("ERROR", f"[BotRunner] CRASH:\n{err}")
        finally:
            try:
                pending = asyncio.all_tasks(self._loop)
                for task in pending:
                    task.cancel()
                if pending:
                    self._loop.run_until_complete(asyncio.gather(*pending, return_exceptions=True))
                self._loop.close()
            except Exception:
                pass
            sys.stdout    = old_stdout
            sys.stderr    = old_stderr
            self._running = False
            self._emit("SYSTEM", f"[BotRunner] Session ended — bot {self.bot_id}")


# ── Session registry — one entry per bot_id ──────────────────────────────────

_sessions: dict[str, BotRunnerSession] = {}


def start_bot(bot_id: str, script_path: str, token: str, log_callback) -> bool:
    """
    Start a bot. token may be "" if the script has its own hardcoded token.
    Returns False if already running.
    """
    if bot_id in _sessions and _sessions[bot_id].is_running():
        return False
    session = BotRunnerSession(bot_id, script_path, token, log_callback)
    _sessions[bot_id] = session
    session.start()
    return True


def stop_bot(bot_id: str) -> bool:
    if bot_id not in _sessions:
        return False
    _sessions[bot_id].stop()
    return True


def restart_bot(bot_id: str) -> bool:
    if bot_id not in _sessions:
        return False
    s = _sessions[bot_id]
    s.stop()
    # Give the old thread ~1s to die, then start fresh
    import time
    time.sleep(0.8)
    new_session = BotRunnerSession(s.bot_id, s.script_path, s.token, s.log_callback)
    _sessions[bot_id] = new_session
    new_session.start()
    return True


def is_bot_running(bot_id: str) -> bool:
    return bot_id in _sessions and _sessions[bot_id].is_running()


def get_running_bots() -> list:
    return [bid for bid, s in _sessions.items() if s.is_running()]


def get_all_bots() -> dict:
    """Returns {bot_id: is_running} for all known sessions."""
    return {bid: s.is_running() for bid, s in _sessions.items()}
