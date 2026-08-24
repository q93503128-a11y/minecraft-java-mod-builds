#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.61','## Alpha.61 — rollback-safe outpost grading','snapshots every world block','restored in reverse order','advanceOutpostConstruction()` runs only after the complete grade cell succeeds','two-player runtime acceptance'),'alpha.61 README')
must(can,('Current canonical implementation: **0.1.0-alpha.61**','### Alpha.61 outpost grade-cell transaction hardening','rolls back successful partial changes in reverse order','persisted outpost construction step advances only after the full grade cell succeeds','## 14. Current playable slice after Alpha.61','## 15. Unfinished original-scope priorities after Alpha.61'),'alpha.61 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.61`','전초기지 grading 원자성 | **완료/부분**','### Alpha.61 전초 grading transaction 감사','역순 rollback','실제 실패주입, 청크 경계 unload, save/reload acceptance는 아직 남는다'),'alpha.61 gap')
print('Frontier Settlement alpha.61 canonical docs audit: PASS')
