#!/usr/bin/env python3
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
A74 = ROOT / 'tools/test_alpha74_docs.py'
_real_read = Path.read_text

# Alpha.74 remains the consolidated long-form canonical/gap/runtime-lock baseline.
# Alpha.75-76 are recorded as current implementation deltas in PROJECT.md until the next
# consolidated documentation pass, so validate the historical ledger first and then the delta.
a = _real_read(A74, encoding='utf-8').replace("print('Frontier Settlement alpha.74 canonical docs audit: PASS')", 'pass')
ns = {'__file__': str(A74), '__name__': '__main__'}
exec(compile(a, str(A74), 'exec'), ns, ns)

def text(name):
    return (ROOT / name).read_text(encoding='utf-8')

def must(s, tokens, label):
    for token in tokens:
        if token not in s:
            raise SystemExit(f'{label} missing: {token}')

project = text('PROJECT.md')
props = text('gradle.properties')
must(project, (
    'Current implementation delta: **0.1.0-alpha.76**',
    'NPC event/dialogue/social breadth is intentionally bounded',
    'Alpha.76 deepens the exploration -> territory loop',
    'at most +16 field evidence',
    'at most +6 exposed-stone evidence',
    'Mining remains untouched so metadata cannot fabricate an ore specialization'
), 'alpha.76 current direction docs')
must(props, (
    'mod_version=0.1.0-alpha.76',
    'bounded trade/industrial structure knowledge that biases agriculture/quarry outpost specialization'
), 'alpha.76 version docs')
print('Frontier Settlement alpha.76 current docs audit: PASS')
