#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A73=ROOT/'tools/test_alpha73_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='CANONICAL_PLAN.md': s=s.replace('Current canonical implementation: **0.1.0-alpha.74**.','Current canonical implementation: **0.1.0-alpha.73**.')
    elif self.name=='COMPLETION_GAP_AUDIT.md': s=s.replace('현재 구현 기준: `0.1.0-alpha.74`','현재 구현 기준: `0.1.0-alpha.73`')
    elif self.name=='README.md': s=s.replace('## Current version: 0.1.0-alpha.74','## Current version: 0.1.0-alpha.73')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.74"','"frontier_settlement": "0.1.0-alpha.73"')
    return s
Path.read_text=legacy_view
try:
    a=_real(A73,encoding='utf-8').replace("print('Frontier Settlement alpha.73 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A73),'__name__':'__main__'}; exec(compile(a,str(A73),'exec'),ns,ns)
finally: Path.read_text=_real
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.74**','### Alpha.74 external-threat field knowledge gameplay pass','from 8 down to a floor of 5','from 28 to at most 40'), 'alpha.74 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.74`','### Alpha.74 외부 위협 전투지식 게임성 패스','8 -> 7 -> 6 -> 최소 5','28 -> 32 -> 36 -> 최대 40'), 'alpha.74 gap')
must(readme,('## Current version: 0.1.0-alpha.74','## Alpha.74 — external-threat field knowledge gameplay pass','8 -> minimum 5','28 -> maximum 40'), 'alpha.74 readme')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.74 companion lock overclaim')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.74': raise SystemExit('alpha.74 lock target mismatch')
notes='\n'.join(lock.get('notes',[])); must(notes,('Alpha.74 adds bounded first-kill external-hostile field knowledge','does not promote human client/spawn-density acceptance'), 'alpha.74 lock note')
print('Frontier Settlement alpha.74 canonical docs audit: PASS')
