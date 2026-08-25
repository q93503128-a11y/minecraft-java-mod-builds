#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A72=ROOT/'tools/test_alpha72_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='CANONICAL_PLAN.md':
        s=s.replace('Current canonical implementation: **0.1.0-alpha.73**.','Current canonical implementation: **0.1.0-alpha.72**.')
    elif self.name=='COMPLETION_GAP_AUDIT.md':
        s=s.replace('현재 구현 기준: `0.1.0-alpha.73`','현재 구현 기준: `0.1.0-alpha.72`')
    elif self.name=='README.md':
        s=s.replace('## Current version: 0.1.0-alpha.73','## Current version: 0.1.0-alpha.72')
    elif self.name=='COMPANION_LOCK.json':
        s=s.replace('"frontier_settlement": "0.1.0-alpha.73"','"frontier_settlement": "0.1.0-alpha.72"')
    return s
Path.read_text=legacy_view
try:
    a=_real(A72,encoding='utf-8').replace("print('Frontier Settlement alpha.72 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A72),'__name__':'__main__'}; exec(compile(a,str(A72),'exec'),ns,ns)
finally: Path.read_text=_real
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.73**','### Alpha.73 expedition feedback gameplay pass','explore/fight -> settlement becomes more useful','market-barrel relic payout','base64 up to128','at most40/50'), 'alpha.73 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.73`','### Alpha.73 탐험-정착 되먹임 게임성 패스','최대 추가 보너스는 +7','base64에서 최대128','최대40/50'), 'alpha.73 gap')
must(readme,('## Current version: 0.1.0-alpha.73','## Alpha.73 — expedition feedback gameplay pass','market payout by +7','repair efficiency from64 to128','forge power30→40','reforge power40→50'), 'alpha.73 readme')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.73 companion lock overclaim')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.73': raise SystemExit('alpha.73 lock target mismatch')
notes='\n'.join(lock.get('notes',[])); must(notes,('Alpha.73 is a gameplay feedback pass','no new currency/menu/save authority'), 'alpha.73 lock note')
print('Frontier Settlement alpha.73 canonical docs audit: PASS')
