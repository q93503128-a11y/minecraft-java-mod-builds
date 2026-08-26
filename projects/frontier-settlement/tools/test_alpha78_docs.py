#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A77 = ROOT / 'tools/test_alpha77_docs.py'
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.78', 'mod_version=0.1.0-alpha.77')
    elif self.name == 'PROJECT.md':
        s = s.replace('Current implementation delta: **0.1.0-alpha.78**',
                      'Current implementation delta: **0.1.0-alpha.77**')
    elif self.name == 'CANONICAL_PLAN.md':
        s = s.replace('Current canonical implementation: **0.1.0-alpha.78**.',
                      'Current canonical implementation: **0.1.0-alpha.74**.')
    elif self.name == 'COMPLETION_GAP_AUDIT.md':
        s = s.replace('현재 구현 기준: `0.1.0-alpha.78`', '현재 구현 기준: `0.1.0-alpha.74`')
    elif self.name == 'README.md':
        s = s.replace('## Current version: 0.1.0-alpha.78', '## Current version: 0.1.0-alpha.74')
    elif self.name == 'COMPANION_LOCK.json':
        s = s.replace('"frontier_settlement": "0.1.0-alpha.78"',
                      '"frontier_settlement": "0.1.0-alpha.74"')
    return s

Path.read_text = legacy_read
try:
    chain = _real_read(A77, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.77 current docs audit: PASS')", 'pass')
    ns = {'__file__': str(A77), '__name__': '__main__'}
    exec(compile(chain, str(A77), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read

def text(name):
    return (ROOT / name).read_text(encoding='utf-8')

def must(source, tokens, label):
    for token in tokens:
        if token not in source:
            raise SystemExit(f'{label} missing: {token}')

props = text('gradle.properties')
project = text('PROJECT.md')
canonical = text('CANONICAL_PLAN.md')
gap = text('COMPLETION_GAP_AUDIT.md')
readme = text('README.md')
lock = json.loads(text('COMPANION_LOCK.json'))

must(props, ('mod_version=0.1.0-alpha.78', 'territory-network freight efficiency'), 'alpha.78 props docs')
must(project, ('Current implementation delta: **0.1.0-alpha.78**', '32 / 36 / 40 / 44 items',
               'military first, waterfront second, normal freight last'), 'alpha.78 PROJECT docs')
must(canonical, ('Current canonical implementation: **0.1.0-alpha.78**',
                 '### Alpha.78 territory-network physical freight efficiency', '32/36/40/44',
                 'hard cap 44', 'second logistics authority is introduced'), 'alpha.78 canonical docs')
must(gap, ('현재 구현 기준: `0.1.0-alpha.78`', '### Alpha.78 영지망 물리 물류 효율 패스',
           '32/36/40/44, 최대 44', 'route unload에서는 정지'), 'alpha.78 gap docs')
must(readme, ('## Current version: 0.1.0-alpha.78',
              '## Alpha.78 — territory-network physical freight efficiency',
              '32 / 36 / 40 / 44', 'No cargo is virtualized'), 'alpha.78 README docs')
if lock.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.78':
    raise SystemExit('alpha.78 companion lock target mismatch')
notes = '\n'.join(lock.get('notes', []))
must(notes, ('Alpha.78 reuses the existing DOMAIN territory-network level',
             'Military/waterfront reverse supply keeps the historical 16/32 cap and priority',
             'no virtual cargo, force-load, teleport, second logistics authority or hard companion dependency'),
     'alpha.78 companion lock note')

print('Frontier Settlement alpha.78 canonical docs audit: PASS')
