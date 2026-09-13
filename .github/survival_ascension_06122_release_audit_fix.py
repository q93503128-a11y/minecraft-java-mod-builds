from pathlib import Path

path = Path('projects/survival-ascension/tools/test_release_source.py')
text = path.read_text(encoding='utf-8')
old = 'need(project, ["Mod version: `0.61.21-alpha.1`", "## 0.59 Apex Content Escort Integration"], "historical PROJECT regression docs")'
new = 'need(project, [f"Mod version: `{CURRENT_VERSION}`", "## 0.59 Apex Content Escort Integration"], "current PROJECT identity + historical regression docs")'
if text.count(old) != 1:
    raise SystemExit(f'expected one hard-coded historical project-version audit, got {text.count(old)}')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('0.61.22 release-audit current-version identity patched.')
