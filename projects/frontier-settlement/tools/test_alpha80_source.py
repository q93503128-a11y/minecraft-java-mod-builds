#!/usr/bin/env python3
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
CLIENT = JAVA / 'client'
A79 = ROOT / 'tools/test_alpha79_source.py'
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
        "print('Frontier Settlement alpha.23-79 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A79), '__name__': '__main__'}
    exec(compile(chain, str(A79), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read


def text(path):
    return Path(path).read_text(encoding='utf-8')


def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f'{label} missing: {token}')


renderer = text(CLIENT / 'FrontierSoldierRenderer.java')
props = text(ROOT / 'gradle.properties')

must(renderer, (
    'private ItemStack visualServiceSword;',
    'ItemStack serviceSword = visualServiceSword();',
    'private ItemStack visualServiceSword()',
    'if (visualServiceSword == null)',
    'visualServiceSword = new ItemStack(Items.IRON_SWORD);',
    'presentation-only and is never inserted into the entity, world, inventory, or settlement economy',
), 'alpha.80 lazy service-sword render fallback')

for forbidden in (
    'private static final ItemStack VISUAL_SERVICE_SWORD',
    'static final ItemStack VISUAL_SERVICE_SWORD = new ItemStack',
):
    if forbidden in renderer:
        raise SystemExit(f'alpha.80 early ItemStack bootstrap hazard remains: {forbidden}')

# Minecraft 26.2 can load renderer/client classes before registry-backed item components are bound.
# Fail the cumulative audit if a future client class recreates the same class-initializer hazard.
static_itemstack = re.compile(
    r'\bstatic\s+(?:final\s+)?ItemStack\s+\w+\s*=\s*(?:new\s+ItemStack\s*\(|Items\.[^;\n]+getDefaultInstance\s*\()')
for path in CLIENT.rglob('*.java'):
    src = text(path)
    if static_itemstack.search(src):
        raise SystemExit(f'alpha.80 registry-backed static ItemStack initializer in client code: {path.relative_to(ROOT)}')

must(props, (
    'mod_version=0.1.0-alpha.80',
    'Alpha.80 client-boot hardening',
    'defers the presentation-only service-sword ItemStack',
), 'alpha.80 props')

print('Frontier Settlement alpha.23-80 cumulative source audit: PASS')
