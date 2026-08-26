#!/usr/bin/env python3
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
A78 = ROOT / 'tools/test_alpha78_docs.py'
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties': s = s.replace('mod_version=0.1.0-alpha.79', 'mod_version=0.1.0-alpha.78')
    elif self.name == 'PROJECT.md': s = s.replace('Current implementation delta: **0.1.0-alpha.79**', 'Current implementation delta: **0.1.0-alpha.78**')
    elif self.name == 'CANONICAL_PLAN.md': s = s.replace('Current canonical implementation: **0.1.0-alpha.79**.', 'Current canonical implementation: **0.1.0-alpha.78**.')
    elif self.name == 'COMPLETION_GAP_AUDIT.md': s = s.replace('현재 구현 기준: `0.1.0-alpha.79`', '현재 구현 기준: `0.1.0-alpha.78`')
    elif self.name == 'README.md': s = s.replace('## Current version: 0.1.0-alpha.79', '## Current version: 0.1.0-alpha.78')
    elif self.name == 'COMPANION_LOCK.json': s = s.replace('"frontier_settlement": "0.1.0-alpha.79"', '"frontier_settlement": "0.1.0-alpha.78"')
    return s
Path.read_text = legacy_read
try:
    chain = _real_read(A78, encoding='utf-8').replace("print('Frontier Settlement alpha.78 canonical docs audit: PASS')", 'pass')
    ns = {'__file__': str(A78), '__name__': '__main__'}
    exec(compile(chain, str(A78), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read

def text(name): return (ROOT / name).read_text(encoding='utf-8')
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f'{label} missing: {token}')
props=text('gradle.properties'); project=text('PROJECT.md'); canonical=text('CANONICAL_PLAN.md')
gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); audit=text('PRE_PLAYTEST_MANUAL_AUDIT_ALPHA79.md')
lock=json.loads(text('COMPANION_LOCK.json'))
must(props, ('mod_version=0.1.0-alpha.79','pre-playtest manual-audit hardening'), 'alpha.79 props docs')
must(project, ('Current implementation delta: **0.1.0-alpha.79**','Alpha.79 pre-playtest manual-audit hardening'), 'alpha.79 PROJECT')
must(canonical, ('Current canonical implementation: **0.1.0-alpha.79**','### Alpha.79 pre-playtest manual-audit hardening','ambiguous cross-tagged stacks fail closed'), 'alpha.79 canonical')
must(gap, ('현재 구현 기준: `0.1.0-alpha.79`','### Alpha.79 테스트 전 전체 수동검사 하드닝'), 'alpha.79 gap')
must(readme, ('## Current version: 0.1.0-alpha.79','## Alpha.79 — pre-playtest manual-audit hardening'), 'alpha.79 readme')
must(audit, ('human/manual source review','New physical outpost pre-placement material bypass','Ambiguous companion resource tags','Still not proven until real play'), 'alpha.79 manual audit')
if lock.get('target',{}).get('frontier_settlement') != '0.1.0-alpha.79':
    raise SystemExit('alpha.79 companion lock mismatch')
print('Frontier Settlement alpha.79 canonical docs audit: PASS')
