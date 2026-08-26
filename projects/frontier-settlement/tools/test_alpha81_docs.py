#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A80 = ROOT / 'tools/test_alpha80_docs.py'
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.81', 'mod_version=0.1.0-alpha.80')
        s = s.replace(', plus Alpha.81 first-run UI, category-first construction palette, in-game guide, pre-founding HUD guidance, and Korean companion language overlays.', '.')
    elif self.name == 'COMPANION_LOCK.json':
        s = s.replace('"frontier_settlement": "0.1.0-alpha.81"', '"frontier_settlement": "0.1.0-alpha.80"')
    return s

Path.read_text = legacy_read
try:
    chain = _real_read(A80, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.80 canonical docs audit: PASS')", 'pass')
    ns = {'__file__': str(A80), '__name__': '__main__'}
    exec(compile(chain, str(A80), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read

def text(name): return (ROOT / name).read_text(encoding='utf-8')
def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f'{label} missing: {token}')

props = text('gradle.properties')
note = text('UI_AND_KOREAN_ALPHA81.md')
lock = json.loads(text('COMPANION_LOCK.json'))
must(props, ('mod_version=0.1.0-alpha.81', 'Alpha.81 first-run UI', 'Korean companion language overlays'), 'alpha.81 props docs')
if lock.get('status') != 'candidate_runtime_lock':
    raise SystemExit('alpha.81 docs lock status drifted')
if lock.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.81':
    raise SystemExit('alpha.81 docs lock target drifted')
notes = lock.get('notes', [])
if not any('Alpha.81 keeps the Alpha.80 candidate binary pins unchanged' in value for value in notes):
    raise SystemExit('alpha.81 docs lock rationale missing')
if not any('No companion code/assets are copied into Frontier' in value for value in notes):
    raise SystemExit('alpha.81 docs lock copy/dependency boundary missing')
must(note, (
    '0.1.0-alpha.81',
    'YACL',
    'Jade',
    "Xaero's Minimap",
    '명령어 없이',
    'Weapons Expanded',
    'Variants & Ventures',
    'Repurposed Structures',
    'YetAnotherConfigLib',
    '코드/에셋을 복사하지 않는다',
    '실제 그래픽 클라이언트',
), 'alpha.81 UX/localization note')
print('Frontier Settlement alpha.81 canonical docs audit: PASS')
