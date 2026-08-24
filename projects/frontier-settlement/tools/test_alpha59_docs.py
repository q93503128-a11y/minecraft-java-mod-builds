#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.59','## Alpha.59 — centralized single-project authority hardening','SettlementProjectAuthority.anyActive(server, data)','stale client preview','MAIN-thread request','actual two-client long-survival'),'alpha.59 README')
must(can,('Current canonical implementation: **0.1.0-alpha.59**','### Alpha.59 centralized single-project authority hardening','every building/road/outpost/civil preview/start path calls the same `SettlementProjectAuthority.anyActive` gate','## 14. Current playable slice after Alpha.59','## 15. Unfinished original-scope priorities after Alpha.59','actual long-survival/two-client/reconnect runtime acceptance remains unfinished'),'alpha.59 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.59`','shared 공사 단일 authority | **완료/부분**','### Alpha.59 shared project authority 감사','실제 2-client 동시 confirm 및 save/reconnect 장시간 acceptance','Alpha.59 simultaneous building/road/outpost/civil confirm exclusivity acceptance'),'alpha.59 gap')
print('Frontier Settlement alpha.59 canonical docs audit: PASS')
