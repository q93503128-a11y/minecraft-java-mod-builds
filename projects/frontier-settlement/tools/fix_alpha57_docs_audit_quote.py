#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).resolve().parent / 'test_alpha57_docs.py'
text = path.read_text(encoding='utf-8')
old = "'actual weapon becomes the soldier's vanilla MAINHAND equipment'"
new = '"actual weapon becomes the soldier\'s vanilla MAINHAND equipment"'
if text.count(old) != 1:
    raise SystemExit(f'expected one alpha.57 docs quote anchor, found {text.count(old)}')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('Fixed Alpha.57 docs audit apostrophe quoting.')
