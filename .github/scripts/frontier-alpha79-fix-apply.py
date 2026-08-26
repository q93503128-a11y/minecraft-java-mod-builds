#!/usr/bin/env python3
from pathlib import Path
p = Path('.github/scripts/frontier-alpha79-apply.py')
s = p.read_text(encoding='utf-8')
wrong = 'canReplaceForBlueprint(level, data, placement.pos(), current)'
right = 'canReplaceForBlueprint(level, placement.pos(), current)'
count = s.count(wrong)
if count != 2:
    raise SystemExit(f'expected 2 stale outpost signatures, got {count}')
p.write_text(s.replace(wrong, right), encoding='utf-8')
