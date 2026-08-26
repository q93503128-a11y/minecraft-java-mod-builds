#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A79 = ROOT / 'tools/test_alpha79_docs.py'
_real_read = Path.read_text


def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.80', 'mod_version=0.1.0-alpha.79')
        s = s.replace(', plus Alpha.80 client-boot hardening that defers the presentation-only service-sword ItemStack until real render-state extraction after registry component binding and audits client code against registry-backed static ItemStack initialization.', '.')
    return s


Path.read_text = legacy_read
try:
    chain = _real_read(A79, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.79 canonical docs audit: PASS')", 'pass')
    ns = {'__file__': str(A79), '__name__': '__main__'}
    exec(compile(chain, str(A79), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read


def text(name):
    return (ROOT / name).read_text(encoding='utf-8')


def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f'{label} missing: {token}')


props = text('gradle.properties')
hotfix = text('CLIENT_BOOT_HOTFIX_ALPHA80.md')

must(props, (
    'mod_version=0.1.0-alpha.80',
    'Alpha.80 client-boot hardening',
), 'alpha.80 props docs')
must(hotfix, (
    '0.1.0-alpha.80',
    'Components not bound yet',
    'FrontierSoldierRenderer.<clinit>',
    'render-state extraction',
    'No companion version changed',
    'real Modrinth client launch',
), 'alpha.80 client boot hotfix note')

print('Frontier Settlement alpha.80 canonical docs audit: PASS')
