#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A67=ROOT/'tools/test_alpha67_docs.py'
def text(name): return (ROOT/name).read_text(encoding='utf-8')
def must(s,tokens,label):
    for token in tokens:
        if token not in s: raise SystemExit(f'{label} missing: {token}')
# Preserve Alpha.67 canonical constraints while evaluating Alpha.68 current-version metadata.
a=text('tools/test_alpha67_docs.py').replace("print('Frontier Settlement alpha.67 canonical docs audit: PASS')",'pass').replace('0.1.0-alpha.67','0.1.0-alpha.68'); ns={'__file__':str(A67),'__name__':'__main__'}; exec(compile(a,str(A67),'exec'),ns,ns)
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,(
    'Current canonical implementation: **0.1.0-alpha.68**',
    '### Alpha.68 rest-anchor-aware civilian lifecycle evidence',
    'work center + every concrete settlement storage endpoint + every completed HOUSE footprint',
    'normal night rest -> house unload -> false missing -> food-funded duplicate replacement',
    'Long two-player repeated-death/night-rest/save-reload/reconnect runtime acceptance remains unfinished',
),'alpha.68 canonical')
must(gap,(
    '현재 구현 기준: `0.1.0-alpha.68`',
    '### Alpha.68 야간 휴식 anchor / 민간 assignment evidence 감사',
    '정상 야간 집 이동 -> house unload -> false missing -> duplicate resident/transporter',
    '실제 2인 야간 반복 death/replacement/save-reload/reconnect acceptance는 계속 남는다',
),'alpha.68 gap')
must(readme,('## Current version: 0.1.0-alpha.68','## Alpha.68 — rest-anchor-aware civilian lifecycle evidence','No new Alpha.68 key was added.'),'alpha.68 README')
if lock.get('status') != 'candidate_runtime_lock': raise SystemExit('alpha.68 companion lock overclaimed runtime status')
if lock.get('target',{}).get('frontier_settlement') != '0.1.0-alpha.68': raise SystemExit('alpha.68 companion lock target mismatch')
notes='\n'.join(lock.get('notes',[]))
must(notes,('Alpha.68 closes the normal-rest false-missing lifecycle edge','no force-load, teleport, UUID reservation ledger'),'alpha.68 companion note')
for forbidden in ('v0.2 complete','실플레이 검증 완료','full companion runtime: PASS'):
    if forbidden in readme: raise SystemExit(f'alpha.68 README overclaim: {forbidden}')
print('Frontier Settlement alpha.68 canonical docs audit: PASS')
