#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.60','## Alpha.60 — rollback-safe ordinary construction transactions','setBlock` must succeed **before** the crate ItemStacks are consumed','player pre-fill cannot bypass construction cost','failed grade mutation restores all successful partial changes','already-paid `finishIfValid` replacement','real two-player runtime acceptance'),'alpha.60 README')
must(can,('Current canonical implementation: **0.1.0-alpha.60**','### Alpha.60 ordinary construction transaction hardening','successful `setBlock` -> physical crate consume -> construction state advance','unexpected consume failure after a new placement restores the prior block state','Alpha.44 grading captures reversible snapshots','## 14. Current playable slice after Alpha.60','## 15. Unfinished original-scope priorities after Alpha.60'),'alpha.60 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.60`','일반 건물 world/item 거래 원자성 | **완료/부분**','### Alpha.60 일반 건설 transaction 감사','player pre-fill 무료건설 exploit 없음','complete grade mutation rollback + step 유지','실제 실패주입 및 장시간 save/reload 검증은 남는다'),'alpha.60 gap')
print('Frontier Settlement alpha.60 canonical docs audit: PASS')
