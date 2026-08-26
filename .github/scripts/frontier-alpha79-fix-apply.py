#!/usr/bin/env python3
from pathlib import Path
p = Path('.github/scripts/frontier-alpha79-apply.py')
s = p.read_text(encoding='utf-8')
wrong = 'canReplaceForBlueprint(level, data, placement.pos(), current)'
right = 'canReplaceForBlueprint(level, placement.pos(), current)'
count = s.count(wrong)
if count < 2:
    raise SystemExit(f'expected stale outpost signatures, got {count}')
s = s.replace(wrong, right)
old_helper = '''def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, got {count}')
    return text.replace(old, new, 1)
'''
new_helper = '''def replace_once(text, old, new, label):
    count = text.count(old)
    if label == 'outpost preplaced early skip':
        if count != 2:
            raise SystemExit(f'{label}: expected legacy+physical matches, got {count}')
        before, marker, after = text.rpartition(old)
        if not marker:
            raise SystemExit(f'{label}: physical match missing')
        return before + new + after
    if count != 1:
        raise SystemExit(f'{label}: expected one match, got {count}')
    return text.replace(old, new, 1)
'''
if s.count(old_helper) != 1:
    raise SystemExit('replace_once helper layout changed')
s = s.replace(old_helper, new_helper, 1)
p.write_text(s, encoding='utf-8')
