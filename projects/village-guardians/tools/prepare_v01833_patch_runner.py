#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT / "tools/apply_v01833_playtest_ui_wall.py"

text = PATCH.read_text(encoding="utf-8")
old = '''    text = replace_once(text, ''' + "'''pane.left() + 13, y + 6,''', '''pane.left() + 13, y + 7,''', \"wave title y\")" + '''\n'''
new = '''    text = replace_once(text,\n''' + "'''            graphics.text(font, fit(font, wave.title(), pane.width() - 22), pane.left() + 13, y + 6,\n                    active ? GOLD : TEXT, false);''',\n'''            graphics.text(font, fit(font, wave.title(), pane.width() - 22), pane.left() + 13, y + 7,\n                    active ? GOLD : TEXT, false);''', \"wave title y\")" + '''\n'''
if text.count(old) != 1:
    raise RuntimeError(f"expected one ambiguous wave-title patch statement, found {text.count(old)}")
PATCH.write_text(text.replace(old, new, 1), encoding="utf-8")
print("[PATCH-RUNNER] disambiguated wave title replacement target")
