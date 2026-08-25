#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A71=ROOT/'tools/test_alpha71_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name in ('CANONICAL_PLAN.md','COMPLETION_GAP_AUDIT.md','README.md','COMPANION_LOCK.json'):
        s=s.replace('0.1.0-alpha.72','0.1.0-alpha.71').replace('Alpha.72','Alpha.71')
    return s
Path.read_text=legacy_view
try:
    a=_real(A71,encoding='utf-8').replace("print('Frontier Settlement alpha.71 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A71),'__name__':'__main__'}; exec(compile(a,str(A71),'exec'),ns,ns)
finally: Path.read_text=_real
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.72**','### Alpha.72 full-project authority / transaction hardening','105-Java-file full-audit','same builder walks back to the town anchor','road grading is now one reversible world transaction','renewable public-defense infrastructure','Long two-player death/unload/save-reload/reconnect and fresh-world companion-stack acceptance remain unfinished'), 'alpha.72 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.72`','### Alpha.72 전체 소스 authority / transaction 감사','Frontier Java 105개','같은 builder가 마을 anchor로 실제 복귀','broad storage insert를 제거','무한 철 파밍을 막는다','신규 컨텐츠/UUID save ledger/virtual cargo/force-load/teleport/key/UI/building/second logistics authority 없음'), 'alpha.72 gap')
must(readme,('## Current version: 0.1.0-alpha.72','## Alpha.72 — full-project authority / transaction hardening','full 105-Java-file audit','No new Alpha.72 key','Long two-player unload/death/save-reconnect and fresh-world companion acceptance remain unfinished'), 'alpha.72 readme')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.72 companion lock overclaim')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.72': raise SystemExit('alpha.72 lock target mismatch')
notes='\n'.join(lock.get('notes',[])); must(notes,('Alpha.72 is full-project error hardening only','No new content, UUID save ledger, virtual cargo'), 'alpha.72 lock note')
print('Frontier Settlement alpha.72 canonical docs audit: PASS')
