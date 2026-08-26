#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A76 = ROOT / 'tools/test_alpha76_docs.py'
_real_read = Path.read_text

# Validate the Alpha.76 historical delta first, then the current Alpha.77 delta.
def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.77', 'mod_version=0.1.0-alpha.76')
    elif self.name == 'PROJECT.md':
        s = s.replace('Current implementation delta: **0.1.0-alpha.77**',
                      'Current implementation delta: **0.1.0-alpha.76**')
    return s

Path.read_text = legacy_read
try:
    a = _real_read(A76, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.76 current docs audit: PASS')", 'pass')
    ns = {'__file__': str(A76), '__name__': '__main__'}
    exec(compile(a, str(A76), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read


def text(name):
    return (ROOT / name).read_text(encoding='utf-8')


def must(source, tokens, label):
    for token in tokens:
        if token not in source:
            raise SystemExit(f'{label} missing: {token}')

project = text('PROJECT.md')
props = text('gradle.properties')
must(project, (
    'Current implementation delta: **0.1.0-alpha.77**',
    'Alpha.77 makes the DOMAIN stage care about a diversified physical territory',
    'two different roles -> level 1, three -> level 2, four -> level 3',
    'Repeating the same specialization never raises the level',
    'market relic payout gains +1 emerald per level',
    'one-metal workshop repair gains +8 durability per level',
    'advanced forge/reforge selection power gains +1 per level',
    'no research currency, virtual cargo, new menu, worker family or second logistics authority is created'
), 'alpha.77 direction docs')
must(props, (
    'mod_version=0.1.0-alpha.77',
    'domain-only diversified productive-outpost network feedback'
), 'alpha.77 version docs')

print('Frontier Settlement alpha.77 current docs audit: PASS')
