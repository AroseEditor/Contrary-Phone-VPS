<div align="center">

<img src="https://raw.githubusercontent.com/AroseEditor/Contrary-Phone-VPS/main/docs/banner.png" width="100%" alt="Contrary Phone VPS Banner"/>

# Contrary Phone VPS

**Run Python Discord bots directly on your Android phone.**  
No server. No cloud. No monthly bill.

[![Release](https://img.shields.io/github/v/release/AroseEditor/Contrary-Phone-VPS?style=for-the-badge&color=7C3AED&label=Latest+Release)](https://github.com/AroseEditor/Contrary-Phone-VPS/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/AroseEditor/Contrary-Phone-VPS/release.yml?style=for-the-badge&color=06B6D4&label=CI%2FCD)](https://github.com/AroseEditor/Contrary-Phone-VPS/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-22C55E?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B-3B82F6?style=for-the-badge&logo=android)](https://developer.android.com)
[![Python](https://img.shields.io/badge/Python-3.11-F59E0B?style=for-the-badge&logo=python)](https://python.org)

</div>

---

## What is Contrary Phone VPS?

Contrary Phone VPS embeds a full **Python 3.11 runtime** (via [Chaquopy](https://chaquo.com/chaquopy/)) directly into an Android app. Your phone becomes a 24/7 Discord bot host — with a full interactive terminal, syntax-highlighted code editor, pip package manager, and persistent background service.

### Core Features

| Feature | Details |
|---------|---------|
| 🐍 **Python 3.11** | Full runtime via Chaquopy — no root needed |
| 🤖 **Discord libraries** | `discord.py` + `aiohttp` pre-installed; `py-cord` / `nextcord` via pip |
| 💻 **Interactive terminal** | `pip install`, `python eval`, `stop` / `exit` (terminal stays open) |
| 🔐 **Encrypted tokens** | Android Keystore AES-256 — never stored plaintext |
| 🔁 **Auto-restart** | Crashed bots automatically restart (configurable max retries) |
| 📟 **Foreground service** | Persistent with wake lock — survives screen off, app swipe |
| 🚀 **Boot auto-start** | Bots start automatically after device reboot |
| 🎨 **Code editor** | Syntax highlighting, line numbers, monospace font — in-app |
| 📦 **Pip manager** | Install/uninstall/list packages from the terminal |
| 🤖 **Multi-bot** | Run multiple bots simultaneously, each isolated |

---

## Terminal — Interactive Console

The built-in terminal stays open even when you stop a bot. Type `exit` or `stop` to kill the process — the console keeps running.

```
❯ pip install py-cord
  Installing py-cord...
  ✓ Successfully installed py-cord

❯ pip list
  discord.py==2.3.2
  aiohttp==3.9.1
  py-cord==2.6.1

❯ start my-bot-id
  Starting bot my-bot-id...

❯ status
  Bot: my-bot-id
    Status: Running
    Uptime: 00:12:34
    Restarts: 0

❯ exit my-bot-id
  Bot my-bot-id stopped. Terminal is still active.
  Type 'start my-bot-id' to restart, or 'help' for options.

❯ python print("still here")
  still here

❯ help
```

### All Terminal Commands

```
BOT CONTROL
  start [bot-id]        Start a bot
  stop  [bot-id]        Stop bot — terminal stays open
  exit  [bot-id]        Alias for stop
  restart [bot-id]      Restart a bot
  status  [bot-id]      Show status, uptime, restarts
  bots                  List all configured bots

PIP
  pip install <pkg>     Install a Python package
  pip uninstall <pkg>   Remove a package
  pip list              List all installed packages
  pip show <pkg>        Show package info
  pip freeze            Same as pip list

PYTHON
  python <code>         Execute Python code inline
  eval <expr>           Evaluate an expression

LOGS
  logs [n]              Show last n log lines (default 50)
  export                Export logs to file

TERMINAL
  clear / cls           Clear the screen
  history               Show command history (↑ ↓ arrow keys work)
  echo <text>           Print text
  version               App version
  help                  This help screen
```

> **Tip:** Use ↑ / ↓ arrow keys to cycle command history. Quick-command chips appear above the input bar.

---

## Installation

### Download APK *(Easiest)*
1. Go to **[Releases](https://github.com/AroseEditor/Contrary-Phone-VPS/releases)**
2. Download `Contrary-Phone-VPS-vX.X.X.apk`
3. On your phone: Settings → Security → Install unknown apps → enable for your file manager
4. Install the APK and complete the permission wizard

### Build from Source
```bash
git clone https://github.com/AroseEditor/Contrary-Phone-VPS.git
cd "Contrary-Phone-VPS"

# Debug build (no signing needed)
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

**Requirements:** Android Studio Ladybug 2024.2+, JDK 17, Android SDK API 35.

---

## GitHub Actions — CI/CD

The workflow at [`.github/workflows/release.yml`](.github/workflows/release.yml) automatically:

1. **Triggers** on `git push` of a `v*.*.*` tag, or manual dispatch from GitHub UI
2. **Sets up** JDK 17, Android SDK, Gradle cache
3. **Builds** a signed release APK
4. **Generates a changelog** from all commits since the previous tag
5. **Creates a draft GitHub Release** titled `Latest Release [vX.X.X]`
6. **Attaches** the APK + ProGuard mapping file

### How to trigger a release
```bash
# Bump version in gradle.properties first, then:
git tag v1.0.1
git push origin v1.0.1
# → GitHub Actions builds and creates a draft release automatically
```

### Required Repository Secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret | Description |
|--------|-------------|
| `SIGNING_KEYSTORE` | Base64-encoded release keystore (`base64 release.keystore`) |
| `STORE_PASSWORD` | Keystore store password |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |

> Without secrets set, the workflow builds an unsigned debug APK for testing.

---

## Permissions Explained

| Permission | Why it's needed |
|-----------|----------------|
| `SYSTEM_ALERT_WINDOW` | Floating mini-terminal overlay over other apps |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keeps bots running with screen off |
| `POST_NOTIFICATIONS` | Bot status & crash alerts (Android 13+) |
| `FOREGROUND_SERVICE` | Persistent bot runner service |
| `FOREGROUND_SERVICE_DATA_SYNC` | Service type declaration |
| `WAKE_LOCK` | Prevents CPU sleep while bots are active |
| `RECEIVE_BOOT_COMPLETED` | Auto-start bots after reboot |
| `INTERNET` | Discord WebSocket connection |
| `READ_EXTERNAL_STORAGE` | Load `.py` files from device storage |

---

## Discord Bot Setup

1. Go to [discord.com/developers/applications](https://discord.com/developers/applications)
2. Create application → **Bot** → **Reset Token** → copy it
3. Enable **Message Content Intent** under Privileged Gateway Intents
4. In the app: tap **Create Bot** → paste your token (stored encrypted instantly)
5. Write or load your `.py` bot script
6. Tap **Run** or type `start <bot-id>` in the terminal

### Example Bot
```python
# BOT_TOKEN is injected automatically by the runtime — never hardcode it
import discord

intents = discord.Intents.default()
intents.message_content = True
client = discord.Client(intents=intents)

@client.event
async def on_ready():
    print(f"Online as {client.user}")

@client.event
async def on_message(message):
    if message.author == client.user:
        return
    if message.content == "!ping":
        await message.channel.send("🏓 Pong! Running on Contrary Phone VPS.")

client.run(BOT_TOKEN)
```

See [`python/example_bot.py`](python/example_bot.py) for the full example.

---

## OEM Battery Killer Workarounds

Some Android manufacturers aggressively kill background processes. Fix:

| Manufacturer | Steps |
|-------------|-------|
| **Xiaomi / MIUI / HyperOS** | Settings → Apps → Contrary VPS → Battery saver → No restrictions |
| **Samsung / One UI** | Settings → Battery → Background usage limits → Never sleeping apps → Add app |
| **Oppo / ColorOS** | Settings → Battery → Energy consumption → No restrictions |
| **Vivo / OriginOS** | iManager → App management → Autostart → Enable |
| **Huawei / EMUI** | Phone Manager → Protected apps → Enable |
| **OnePlus / OxygenOS** | Settings → Battery → Battery optimization → All apps → Don't optimize |

The **Settings screen** inside the app also shows your current permission status with one-tap fixes.

---

## Architecture

```
┌──────────────────────────────────────────┐
│         UI Layer (Jetpack Compose)        │
│  Sidebar · Editor · Terminal · Onboarding │
└──────────────────┬───────────────────────┘
                   │ StateFlow / ViewModel
┌──────────────────▼───────────────────────┐
│          BotViewModel · TerminalViewModel  │
│          BotRepository · PipManager       │
└──────────────────┬───────────────────────┘
                   │ Intent / Binder
┌──────────────────▼───────────────────────┐
│         BotForegroundService              │
│  WakeLock · AutoRestart · BootReceiver   │
└──────────────────┬───────────────────────┘
                   │ Chaquopy JNI bridge
┌──────────────────▼───────────────────────┐
│         Python 3.11 Runtime               │
│  bot_runner.py · pip_helper.py · asyncio  │
└──────────────────────────────────────────┘
```

**Tech stack:** Kotlin · Jetpack Compose · Material 3 · Hilt · Room · Chaquopy · EncryptedSharedPreferences · WorkManager · Coroutines / Flow

---

## Troubleshooting

**Bot stops when screen turns off**
→ Settings → Permissions → Battery Optimization → tap Fix

**`ModuleNotFoundError: discord`**
→ Terminal: `pip install discord.py`

**Bot crashes immediately**
→ Check terminal for full traceback. Common: wrong token, missing `message_content` intent

**Pip install fails / network error**
→ Check internet connection. Ensure the bot isn't blocking the main thread

**Service killed after a few minutes**
→ Follow the OEM battery killer steps above for your device brand

**`exit` closes the whole app?**
→ It shouldn't — `exit` only stops the bot process. The terminal stays open. This is by design.

---

## FAQ

**Q: Root required?**
A: No. Fully userspace.

**Q: Does the bot keep running when I close the app?**
A: Yes — the foreground service + wake lock keeps it alive. You'll see a persistent notification.

**Q: Are my Discord tokens safe?**
A: Yes. Tokens are encrypted with Android Keystore (AES-256-GCM) the moment you type them. They are never written to disk in plaintext.

**Q: Can I run multiple bots at once?**
A: Yes. Each bot runs in its own Python thread with an isolated asyncio event loop.

**Q: What Python packages can I install?**
A: Any pure-Python package works. Packages requiring compiled C extensions may not work on all architectures.

**Q: Can I import my own `.py` files?**
A: Yes — select them from storage in the bot editor, or write code directly in the built-in editor.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). PRs welcome — especially OEM workarounds, package compatibility notes, and UI improvements.

## Security

See [SECURITY.md](SECURITY.md) for vulnerability reporting.

## License

[MIT](LICENSE) © 2025 AroseEditor
