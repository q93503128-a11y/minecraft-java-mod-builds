#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.64','## Alpha.64 — atomic food-funded worker arrivals','failed entity insertion consumes no food and adds no population','food consumption unexpectedly fails','Transport workers belong to a specific outpost','there is still only one authority for long-distance outpost transport'),'alpha.64 README')
must(can,('Current canonical implementation: **0.1.0-alpha.64**','### Alpha.64 atomic worker-arrival transaction','failed `addFreshEntity` means no food loss and no population increment','unexpected food-consume failure discards only that new worker','## 14. Current playable slice after Alpha.64','## 15. Unfinished original-scope priorities after Alpha.64','there is still only one authority for long-distance outpost transport'),'alpha.64 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.64`','### Alpha.64 주민 유입/운송자 대체 원자성 감사','entity add 실패면 food/population 변경 0','방금 생성한 주민을 discard','repeated-death no-dup 실플레이 acceptance','there is still only one authority for long-distance outpost transport'),'alpha.64 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.64"','Alpha.64 makes existing food-funded resident arrivals atomic'),'alpha.64 lock')
print('Frontier Settlement alpha.64 canonical docs audit: PASS')
