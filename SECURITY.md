# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.x.x   | ✅        |

## Reporting a Vulnerability

Please **do not** open public GitHub issues for security vulnerabilities.

Email: *(add your contact email)*

Or use [GitHub Private Vulnerability Reporting](https://github.com/AroseEditor/Contrary-Phone-VPS/security/advisories/new).

**Include:**
- Description of the vulnerability
- Steps to reproduce
- Potential impact

We'll respond within 48 hours.

## Security Design

- **Token storage**: Discord tokens are encrypted using Android Keystore (AES-256-GCM) via `EncryptedSharedPreferences`. Never stored plaintext. Never transmitted to any server.
- **Script validation**: Uploaded bot scripts are scanned for dangerous patterns before execution (`os.system`, `subprocess shell=True`, etc.)
- **Sandboxing**: Bots run in the app's private data directory. They cannot access other apps' files.
- **No telemetry**: The app collects no user data whatsoever.
