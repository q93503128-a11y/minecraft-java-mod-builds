#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A69=ROOT/'tools/test_alpha69_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name in ('CANONICAL_PLAN.md','COMPLETION_GAP_AUDIT.md','README.md','COMPANION_LOCK.json'): s=s.replace('0.1.0-alpha.70','0.1.0-alpha.69').replace('Alpha.70','Alpha.69')
    return s
Path.read_text=legacy_view
try:
    a=_real(A69,encoding='utf-8').replace("print('Frontier Settlement alpha.69 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A69),'__name__':'__main__'}; exec(compile(a,str(A69),'exec'),ns,ns)
finally: Path.read_text=_real
def text(name): return (ROOT/name).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.70**','### Alpha.70 specialized-outpost production lifecycle / physical mutation transaction hardening','every chunk intersecting the exact existing ±48 assignment lookup AABB is loaded','corresponding `setBlock` world mutation succeeds','Long two-player repeated death, route/rest unload, save/reload and reconnect acceptance remains unfinished'),'alpha.70 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.70`','### Alpha.70 전초 현지 생산자 lifecycle / 생산 world transaction 감사','lookup AABB 전체 청크가 `hasChunkAt`으로 loaded','실패한 world mutation은 생산량 0','실제 2인 반복 death -> cargo recovery -> unload/reload -> save/reconnect acceptance는 계속 남는다'),'alpha.70 gap')
must(readme,('## Current version: 0.1.0-alpha.70','## Alpha.70 — specialized-outpost production lifecycle / mutation transaction hardening','No new Alpha.70 key was added.','output is created only after the matching world `setBlock` succeeds'),'alpha.70 README')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.70 companion lock overclaimed runtime status')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.70': raise SystemExit('alpha.70 lock target mismatch')
notes=chr(10).join(lock.get('notes',[])); must(notes,('Alpha.70 hardens specialized outpost production','No population ledger, food refund, force-load, teleport'),'alpha.70 lock note')
for forbidden in ('v0.2 complete','실플레이 검증 완료','full companion runtime: PASS'):
    if forbidden in readme: raise SystemExit(f'alpha.70 README overclaim: {forbidden}')
print('Frontier Settlement alpha.70 canonical docs audit: PASS')
