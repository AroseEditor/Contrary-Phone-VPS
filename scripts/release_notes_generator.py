#!/usr/bin/env python3
"""
release_notes_generator.py
Generates formatted release notes from git history between two tags.
Usage: python scripts/release_notes_generator.py [from_tag] [to_tag]
"""
import subprocess
import sys
import re
from datetime import datetime


def get_commits(from_tag: str | None, to_tag: str = "HEAD") -> list[dict]:
    """Get commits between two refs."""
    if from_tag:
        ref = f"{from_tag}..{to_tag}"
    else:
        ref = to_tag

    result = subprocess.run(
        ["git", "log", ref, "--pretty=format:%H|%s|%an|%ae|%ad", "--date=short"],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        print(f"git log failed: {result.stderr}", file=sys.stderr)
        return []

    commits = []
    for line in result.stdout.strip().splitlines():
        if not line:
            continue
        parts = line.split("|", 4)
        if len(parts) >= 4:
            commits.append({
                "hash": parts[0][:7],
                "subject": parts[1],
                "author": parts[2],
                "email": parts[3],
                "date": parts[4] if len(parts) > 4 else "",
            })
    return commits


def categorize_commit(subject: str) -> str:
    """Categorize commit by conventional commit prefix."""
    patterns = {
        r"^feat(\(.+\))?!?:": "✨ Features",
        r"^fix(\(.+\))?!?:": "🐛 Bug Fixes",
        r"^perf(\(.+\))?!?:": "⚡ Performance",
        r"^refactor(\(.+\))?!?:": "♻️ Refactoring",
        r"^docs(\(.+\))?!?:": "📚 Documentation",
        r"^style(\(.+\))?!?:": "💅 Style",
        r"^test(\(.+\))?!?:": "🧪 Tests",
        r"^ci(\(.+\))?!?:": "⚙️ CI/CD",
        r"^chore(\(.+\))?!?:": "🔧 Chores",
        r"^build(\(.+\))?!?:": "📦 Build",
    }
    for pattern, category in patterns.items():
        if re.match(pattern, subject, re.IGNORECASE):
            return category
    return "📝 Changes"


def get_previous_tag() -> str | None:
    result = subprocess.run(
        ["git", "tag", "--sort=-version:refname"],
        capture_output=True, text=True,
    )
    tags = [t for t in result.stdout.strip().splitlines() if t.startswith("v")]
    return tags[1] if len(tags) > 1 else None


def generate(from_tag: str | None, to_tag: str, repo: str) -> str:
    commits = get_commits(from_tag, to_tag)
    if not commits:
        return "No changes found."

    # Group by category
    categories: dict[str, list[dict]] = {}
    for commit in commits:
        cat = categorize_commit(commit["subject"])
        categories.setdefault(cat, []).append(commit)

    lines = [f"## Release Notes — {to_tag}\n", f"*Generated {datetime.now().strftime('%Y-%m-%d')}*\n"]

    for category, cat_commits in sorted(categories.items()):
        lines.append(f"\n### {category}\n")
        for c in cat_commits:
            lines.append(f"- {c['subject']} ([`{c['hash']}`](https://github.com/{repo}/commit/{c['hash']})) — {c['author']}")

    if from_tag:
        lines.append(f"\n---\n**Full changelog**: https://github.com/{repo}/compare/{from_tag}...{to_tag}")

    return "\n".join(lines)


if __name__ == "__main__":
    from_tag = sys.argv[1] if len(sys.argv) > 1 else get_previous_tag()
    to_tag = sys.argv[2] if len(sys.argv) > 2 else "HEAD"
    repo = sys.argv[3] if len(sys.argv) > 3 else "AroseEditor/Contrary-Phone-VPS"

    notes = generate(from_tag, to_tag, repo)
    print(notes)
