#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.65','## Alpha.65 — exact local civilian cargo death recovery','vanilla equipment-drop randomness is cleared','current MAINHAND ItemStack is emitted exactly once','pre-Alpha.65 saves','road-bound outpost transporters are explicitly excluded','no new SavedData field'),'alpha.65 README')
must(can,('Current canonical implementation: **0.1.0-alpha.65**','### Alpha.65 local civilian physical-cargo death boundary','exactly one copy of its current MAINHAND stack','outpost transport workers are excluded','## 14. Current playable slice after Alpha.65','## 15. Unfinished original-scope priorities after Alpha.65','no-dup/no-loss acceptance remain'),'alpha.65 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.65`','### Alpha.65 로컬 주민 실물 화물 사망 경계 감사','MAINHAND exact ItemStack copy1','Alpha.63 transporter handler만 사용','no-loss/no-dup 실플레이 acceptance'),'alpha.65 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.65"','Alpha.65 gives local production/workshop civilians exact physical MAINHAND cargo recovery on death'),'alpha.65 lock')
# Previous Alpha.64 contract remains explicitly documented.
must(readme,('## Alpha.64 — atomic food-funded worker arrivals','failed entity insertion consumes no food and adds no population','there is still only one authority for long-distance outpost transport'),'alpha.64 retained README')
must(can,('### Alpha.64 atomic worker-arrival transaction','failed `addFreshEntity` means no food loss and no population increment','there is still only one authority for long-distance outpost transport'),'alpha.64 retained canonical')
print('Frontier Settlement alpha.65 canonical docs audit: PASS')
