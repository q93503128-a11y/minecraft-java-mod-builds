#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.66','## Alpha.66 — loaded-evidence-safe civilian lifecycle authority','unloaded resident is **not** treated as dead','atomic real-food4 path','pre-Alpha.66 advanced artisans','ADVANCED_WORKER_TAG','no new SavedData field'),'alpha.66 README')
must(can,('Current canonical implementation: **0.1.0-alpha.66**','### Alpha.66 loaded-evidence-safe civilian lifecycle authority','incomplete evidence freezes reconciliation/replacement','existing civilian housing/food authority','old `SettlementService` free advanced-artisan spawn is removed','## 14. Current playable slice after Alpha.66','## 15. Unfinished original-scope priorities after Alpha.66'),'alpha.66 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.66`','### Alpha.66 민간 주민 lifecycle / 언로드 증거 감사','unloaded 주민을 사망자로 간주하지 않고','housing + real food4','소급 food 청구 없이','실제 2인 반복 사망/재접속/save-reload acceptance'),'alpha.66 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.66"','Alpha.66 unifies the civilian lifecycle boundary'),'alpha.66 lock')
# Alpha.65 physical death boundary remains explicit.
must(readme,('## Alpha.65 — exact local civilian cargo death recovery','road-bound outpost transporters are explicitly excluded'),'alpha.65 retained README')
print('Frontier Settlement alpha.66 canonical docs audit: PASS')
