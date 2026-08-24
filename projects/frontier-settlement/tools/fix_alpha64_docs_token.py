#!/usr/bin/env python3
from pathlib import Path
p = Path(__file__).resolve().parent / 'test_alpha64_docs.py'
s = p.read_text(encoding='utf-8')
old = "'unexpected food consumption'"
new = "'food consumption unexpectedly fails'"
if new not in s:
    if old not in s:
        raise SystemExit('alpha.64 docs-token patch target missing')
    p.write_text(s.replace(old, new, 1), encoding='utf-8')
print('Aligned Frontier alpha.64 docs audit token.')
