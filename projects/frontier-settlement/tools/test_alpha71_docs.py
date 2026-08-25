#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A70=ROOT/'tools/test_alpha70_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name in ('CANONICAL_PLAN.md','COMPLETION_GAP_AUDIT.md','README.md','COMPANION_LOCK.json'): s=s.replace('0.1.0-alpha.71','0.1.0-alpha.70').replace('Alpha.71','Alpha.70')
    return s
Path.read_text=legacy_view
try:
    a=_real(A70,encoding='utf-8').replace("print('Frontier Settlement alpha.70 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A70),'__name__':'__main__'}; exec(compile(a,str(A70),'exec'),ns,ns)
finally: Path.read_text=_real
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.71**','### Alpha.71 barracks / construction-office route-evidence lifecycle hardening','food8 + metal2 recruitment','rather than `discard()`ed','Long two-player death/unload/save-reload/reconnect acceptance remains unfinished'),'alpha.71 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.71`','### Alpha.71 병영/건설소 route-evidence lifecycle 감사','food8 + metal2 재충원을 보류','기존 `duplicate.discard()` 제거','신규 컨텐츠/ledger/virtual cargo/force-load/teleport/key/UI/building/logistics authority 없음'),'alpha.71 gap')
must(readme,('## Current version: 0.1.0-alpha.71','## Alpha.71 — route-evidence military / construction-supply lifecycle hardening','No new Alpha.71 key was added.','No content was added.'),'alpha.71 readme')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.71 companion lock overclaim')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.71': raise SystemExit('alpha.71 lock target mismatch')
notes=chr(10).join(lock.get('notes',[])); must(notes,('Alpha.71 is lifecycle-only hardening','No new content, worker ledger, virtual cargo'),'alpha.71 lock note')
print('Frontier Settlement alpha.71 canonical docs audit: PASS')
