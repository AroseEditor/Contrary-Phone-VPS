# Contributing to Contrary Phone VPS

Thanks for your interest! Here's how to get started.

## Development Setup

1. Fork the repo and clone it
2. Open in Android Studio (Ladybug 2024.2+)
3. Let Gradle sync and download Chaquopy's Python runtime (~100MB first time)
4. Run on a physical device or emulator with API 29+

## Code Style
- Kotlin: follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Compose: prefer stateless composables, hoist state to ViewModels
- Python: PEP 8, type hints where possible

## Commit Messages
Use [Conventional Commits](https://www.conventionalcommits.org/):
```
feat(terminal): add pip freeze command
fix(service): prevent duplicate wake lock acquisition
docs(readme): add OEM battery killer table
```

## Pull Request Process
1. Branch from `main`: `git checkout -b feat/your-feature`
2. Write a clear PR description
3. Ensure the debug APK builds: `./gradlew assembleDebug`
4. One feature per PR

## Issues
- Bug reports: include device model, Android version, crash log from the terminal
- Feature requests: open a Discussion first

## Areas That Need Help
- More OEM battery killer workarounds
- Better Python package compatibility testing
- UI/UX improvements
- Documentation
