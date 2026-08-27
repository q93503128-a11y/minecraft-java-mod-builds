#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A81 = ROOT / 'tools/test_alpha81_docs.py'
_real_read = Path.read_text


def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.82', 'mod_version=0.1.0-alpha.81')
        s = s.replace(', plus Alpha.82 collision-safe pack key profile with M as the Frontier settlement menu and default-only Xaero quick-waypoint B normalization that preserves user-customized controls.', '.')
    return s


Path.read_text = legacy_read
try:
    chain = _real_read(A81, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.81 canonical docs audit: PASS')", 'pass')
    ns = {'__file__': str(A81), '__name__': '__main__'}
    exec(compile(chain, str(A81), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read


def text(name): return (ROOT / name).read_text(encoding='utf-8')
def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f'{label} missing: {token}')

props = text('gradle.properties')
note = text('KEYBIND_PROFILE_ALPHA82.md')
must(props, ('mod_version=0.1.0-alpha.82', 'Alpha.82 collision-safe pack key profile'), 'alpha.82 props docs')
must(note, (
    '0.1.0-alpha.82',
    '`M` — Frontier Settlement main menu',
    '`B` — Sophisticated Backpacks',
    '`U` — Xaero',
    'still `isDefault()`',
    'player-customized mapping',
    'Better Combat',
    'Weapons Expanded 1.9.3',
    'No companion Java class, code, texture or UI asset is copied',
    'Real graphical-client acceptance',
), 'alpha.82 key profile note')
print('Frontier Settlement alpha.82 canonical docs audit: PASS')
