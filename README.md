# Contrary Phone VPS

> Run Python-based Discord bots directly on your Android phone — no server, no cloud, no cost.

[![Release](https://img.shields.io/github/v/release/AroseEditor/Contrary-Phone-VPS?style=flat-square&color=7C3AED)](https://github.com/AroseEditor/Contrary-Phone-VPS/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-cyan.svg?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B-green?style=flat-square)](https://developer.android.com)
[![Python](https://img.shields.io/badge/Python-3.11-blue?style=flat-square)](https://python.org)

---

## What is this?

**Contrary Phone VPS** turns your Android phone into a persistent Discord bot host. Using [Chaquopy](https://chaquo.com/chaquopy/), it embeds a full Python 3.11 runtime directly in the app — no root required.

**Features:**
- 🐍 Full Python 3.11 runtime (Chaquopy)
- 🤖 Run `discord.py`, `py-cord`, `nextcord`, `aiohttp` bots
- 💻 Built-in interactive terminal with `pip install`, `python eval`, `stop/start/restart`
- 🔐 Encrypted token storage (EncryptedSharedPreferences — never plaintext)
- 📟 Persistent foreground service with wake lock
- 🔁 Auto-restart crashed bots
- 🚀 Boot-on-startup support
- 🎨 Cyberpunk dark UI — VSCode + Discord + Linux terminal aesthetic

---

## Screenshots

> *(Add screenshots here after first build)*

| Dashboard | Bot Editor | Terminal |
|-----------|-----------|----------|
| ![dashboard](docs/screenshots/dashboard.png) | ![editor](docs/screenshots/editor.png) | ![terminal](docs/screenshots/terminal.png) |

---

## Installation

### Download APK
1. Go to [Releases](https://github.com/AroseEditor/Contrary-Phone-VPS/releases)
2. Download the latest `Contrary-Phone-VPS-vX.X.X.apk`
3. Enable "Install from unknown sources" on your device
4. Install and grant all permissions

### Build from Source
See [Building Guide](#building-guide) below.

---

## Permissions Explained

| Permission | Why |
|-----------|-----|
| `SYSTEM_ALERT_WINDOW` | Floating mini-terminal overlay over other apps |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keeps bots running when screen is off |
| `POST_NOTIFICATIONS` | Bot status & crash alerts |
| `FOREGROUND_SERVICE` | Persistent bot runner service |
| `RECEIVE_BOOT_COMPLETED` | Auto-start bots after device reboot |
| `WAKE_LOCK` | Prevents CPU sleep while bots are active |
| `READ_EXTERNAL_STORAGE` | Load your `.py` bot files from storage |
| `INTERNET` | Discord WebSocket connection |

---

## Discord Bot Setup

1. Go to [discord.com/developers/applications](https://discord.com/developers/applications)
2. Create a new application → Bot → Copy token
3. In the app: **Create Bot** → paste token (stored encrypted)
4. Select your `.py` bot file or write it in the built-in editor
5. Tap **Start** or type `start` in the terminal

### Supported Libraries
- `discord.py` (installed by default)
- `aiohttp` (installed by default)
- `py-cord` → terminal: `pip install py-cord`
- `nextcord` → terminal: `pip install nextcord`

---

## Interactive Terminal

The built-in terminal supports:

```
$ pip install discord.py      # Install packages
$ pip list                    # List installed packages
$ pip uninstall package       # Remove package
$ start <bot-id>              # Start a bot
$ stop <bot-id>               # Stop bot (terminal STAYS OPEN)
$ exit <bot-id>               # Alias for stop
$ restart <bot-id>            # Restart a bot
$ status                      # Show bot status & uptime
$ python print("hello")       # Execute Python code
$ eval 1 + 1                  # Evaluate expression
$ logs 100                    # Show last 100 log lines
$ export                      # Export logs to file
$ clear                       # Clear terminal
$ help                        # Show all commands
```

> **Note**: `exit` stops the **bot process** — the terminal stays open and interactive.

---

## Building Guide

### Prerequisites
- Android Studio Ladybug (2024.2+)
- JDK 17
- Android SDK API 35

### Steps
```bash
git clone https://github.com/AroseEditor/Contrary-Phone-VPS.git
cd "Contrary-Phone-VPS"
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
# Set keystore environment variables
export SIGNING_KEYSTORE_PATH=/path/to/release.keystore
export STORE_PASSWORD=yourpassword
export KEY_ALIAS=youralias
export KEY_PASSWORD=yourkeypassword

./gradlew assembleRelease
```

---

## GitHub Actions

The workflow at [`.github/workflows/release.yml`](.github/workflows/release.yml) automatically:

1. Triggers on `git tag v*.*.*` push or manual dispatch
2. Builds a signed release APK
3. Generates a changelog from all commits since last tag
4. Creates a **draft GitHub Release** titled `Latest Release [vX.X.X]`
5. Attaches APK + ProGuard mapping

### Required Secrets
| Secret | Description |
|--------|-------------|
| `SIGNING_KEYSTORE` | Base64-encoded release keystore |
| `STORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

### Creating a release
```bash
git tag v1.0.1
git push origin v1.0.1
# GitHub Actions automatically builds and creates a draft release
```

---

## OEM Battery Killer Workarounds

Some manufacturers aggressively kill background processes:

| OEM | Fix |
|-----|-----|
| **Xiaomi / MIUI** | Settings → Apps → Contrary VPS → Battery → No restrictions |
| **Samsung / One UI** | Battery → Background usage limits → Never sleeping apps |
| **Oppo / ColorOS** | Battery → Energy consumption → No restrictions |
| **Vivo / OriginOS** | iManager → App management → Autostart → Enable |
| **Huawei / EMUI** | Phone Manager → Protected apps → Enable |

The app also has a built-in Settings screen that guides you through these steps.

---

## Troubleshooting

**Bot stops when screen turns off**
→ Disable battery optimization in Settings → Permissions → Battery Optimization → Fix

**`discord.py` not found**
→ Open terminal, run: `pip install discord.py`

**Bot crashes on startup**
→ Check terminal for error output. Common issue: wrong token or missing intents

**Service killed by OEM**
→ See OEM Battery Killer table above

**Pip install fails**
→ Check internet connection. Some packages require native extensions and won't work on Android.

---

## FAQ

**Q: Does this require root?**
A: No. Fully userspace via Chaquopy.

**Q: Will bots run when the app is closed?**
A: Yes — the foreground service keeps running. The persistent notification confirms it.

**Q: Are tokens safe?**
A: Tokens are encrypted using Android Keystore via `EncryptedSharedPreferences`. Never stored plaintext.

**Q: Can I run multiple bots?**
A: Yes, each bot runs in its own isolated Python thread with its own asyncio event loop.

**Q: What Python packages work?**
A: Pure-Python packages work best. Packages requiring native C extensions may fail on some architectures.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Security

See [SECURITY.md](SECURITY.md).

---

## License

MIT — see [LICENSE](LICENSE).
