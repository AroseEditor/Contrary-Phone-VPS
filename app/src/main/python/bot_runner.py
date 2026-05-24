"""
bot_runner.py — Runs Discord bots within Chaquopy's Python runtime.
Manages an asyncio event loop, captures stdout/stderr, and reports status back to Kotlin.
"""
import sys
import io
import os
import asyncio
import threading
import traceback
import importlib.util
import types


class BotRunnerSession:
    """Manages a single bot's lifecycle: load script → run async loop → handle crash."""

    def __init__(self, bot_id: str, script_path: str, token: str, log_callback):
        self.bot_id = bot_id
        self.script_path = script_path
        self.token = token
        self.log_callback = log_callback  # callable(level, message)
        self._loop = None
        self._thread = None
        self._stop_event = threading.Event()
        self._running = False

    def _emit(self, level: str, message: str):
        try:
            self.log_callback(level, message)
        except Exception:
            pass

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

    def _run_loop(self):
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)

        # Redirect stdout/stderr to log callback
        class LogWriter(io.TextIOBase):
            def __init__(writer_self, level):
                writer_self.level = level
                writer_self.buf = ""

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
            self._emit("INFO", f"[BotRunner] Loading script: {self.script_path}")
            script_globals = {
                "__name__": "__main__",
                "__file__": self.script_path,
                "BOT_TOKEN": self.token,
            }

            with open(self.script_path, "r", encoding="utf-8") as f:
                source = f.read()

            code = compile(source, self.script_path, "exec")

            # Inject token into env so discord.py can pick it via os.environ too
            os.environ["DISCORD_BOT_TOKEN"] = self.token

            self._emit("INFO", "[BotRunner] Starting asyncio event loop")
            exec(code, script_globals)

            # If script defines a main() coroutine, run it
            if "main" in script_globals and asyncio.iscoroutinefunction(script_globals["main"]):
                self._loop.run_until_complete(script_globals["main"]())
            else:
                self._loop.run_forever()

        except SystemExit as e:
            self._emit("INFO", f"[BotRunner] Script exited cleanly (code {e.code})")
        except Exception:
            err = traceback.format_exc()
            self._emit("ERROR", f"[BotRunner] CRASH:\n{err}")
        finally:
            try:
                # Cancel all remaining tasks
                pending = asyncio.all_tasks(self._loop)
                for task in pending:
                    task.cancel()
                if pending:
                    self._loop.run_until_complete(asyncio.gather(*pending, return_exceptions=True))
                self._loop.close()
            except Exception:
                pass
            sys.stdout = old_stdout
            sys.stderr = old_stderr
            self._running = False
            self._emit("SYSTEM", f"[BotRunner] Session ended for bot {self.bot_id}")


# Registry of active sessions — keyed by bot_id
_sessions: dict[str, BotRunnerSession] = {}


def start_bot(bot_id: str, script_path: str, token: str, log_callback) -> bool:
    """Start a bot session. Returns True if started, False if already running."""
    if bot_id in _sessions and _sessions[bot_id].is_running():
        return False
    session = BotRunnerSession(bot_id, script_path, token, log_callback)
    _sessions[bot_id] = session
    session.start()
    return True


def stop_bot(bot_id: str) -> bool:
    """Stop a bot session. Returns True if stopped."""
    if bot_id not in _sessions:
        return False
    _sessions[bot_id].stop()
    return True


def is_bot_running(bot_id: str) -> bool:
    return bot_id in _sessions and _sessions[bot_id].is_running()


def get_running_bots() -> list:
    return [bid for bid, s in _sessions.items() if s.is_running()]
