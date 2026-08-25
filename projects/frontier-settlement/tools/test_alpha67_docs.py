#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(name): return (ROOT/name).read_text(encoding='utf-8')
def must(s,tokens,label):
    for token in tokens:
        if token not in s: raise SystemExit(f'{label} missing: {token}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,(
    'Current canonical implementation: **0.1.0-alpha.67**',
    '### Alpha.67 fail-closed outpost transporter assignment evidence',
    'exact transporter lookup envelope',
    'pause at unloaded route boundaries',
    'there is still only one authority for long-distance outpost transport',
    'Long two-player repeated-death/save-reload runtime acceptance remains unfinished',
),'alpha.67 canonical')
must(gap,(
    '현재 구현 기준: `0.1.0-alpha.67`',
    '### Alpha.67 전초 운송 주민 assignment evidence 감사',
    'routeBounds',
    '실제 route unload/reload/save-reload/reconnect 반복 acceptance는 계속 남는다',
),'alpha.67 gap audit')
# The README remains a product overview; it must not claim v0.2/full runtime completion.
for forbidden in ('v0.2 complete','실플레이 검증 완료','full companion runtime: PASS'):
    if forbidden in readme: raise SystemExit(f'alpha.67 README overclaim: {forbidden}')
if lock.get('status') != 'candidate_runtime_lock': raise SystemExit('alpha.67 lock status changed from candidate_runtime_lock')
target=lock.get('target',{})
expected={'minecraft':'26.2','loader':'neoforge','neoforge':'26.2.0.38-beta','frontier_settlement':'0.1.0-alpha.67'}
for key,value in expected.items():
    if target.get(key) != value: raise SystemExit(f'alpha.67 lock target mismatch {key}: {target.get(key)!r}')
notes='\n'.join(lock.get('notes',[]))
must(notes,('Alpha.66 unifies the civilian lifecycle boundary','Alpha.67 makes outpost transporter absence decisions fail closed','no force-load, teleport, second transporter authority, virtual cargo or companion dependency'),'alpha.67 lock notes')
print('Frontier Settlement alpha.67 canonical docs audit: PASS')
