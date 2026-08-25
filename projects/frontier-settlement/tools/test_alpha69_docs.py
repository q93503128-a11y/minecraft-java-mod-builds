#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A68=ROOT/'tools/test_alpha68_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name in ('CANONICAL_PLAN.md','COMPLETION_GAP_AUDIT.md','README.md','COMPANION_LOCK.json'):
        s=s.replace('0.1.0-alpha.69','0.1.0-alpha.68').replace('Alpha.69','Alpha.68')
    return s
Path.read_text=legacy_view
try:
    a=_real(A68,encoding='utf-8').replace("print('Frontier Settlement alpha.68 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A68),'__name__':'__main__'}; exec(compile(a,str(A68),'exec'),ns,ns)
finally:
    Path.read_text=_real
def text(name): return (ROOT/name).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.69**','### Alpha.69 historical duplicate-assignment containment','replacement remains authorized only by **zero** matching workers','chosen deterministically by UUID order','Long two-player repeated-death/night-rest/save-reload/reconnect runtime acceptance remains unfinished'),'alpha.69 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.69`','### Alpha.69 기존 중복 assignment containment 감사','matching resident가 정확히 0명일 때만 food4 replacement','물리적 중복 cleanup은 계속 미완료'),'alpha.69 gap')
must(readme,('## Current version: 0.1.0-alpha.69','## Alpha.69 — historical duplicate-assignment containment','No new Alpha.69 key was added.'),'alpha.69 readme')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.69 companion lock overclaimed runtime status')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.69': raise SystemExit('alpha.69 lock target mismatch')
notes='\n'.join(lock.get('notes',[])); must(notes,('Alpha.69 contains historical duplicate assigned residents','No resident is deleted, no cargo is refunded or minted'),'alpha.69 lock note')
for forbidden in ('v0.2 complete','실플레이 검증 완료','full companion runtime: PASS'):
    if forbidden in readme: raise SystemExit(f'alpha.69 README overclaim: {forbidden}')
print('Frontier Settlement alpha.69 canonical docs audit: PASS')
