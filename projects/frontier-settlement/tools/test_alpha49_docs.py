#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(name):
    return (ROOT / name).read_text(encoding='utf-8')


def must(source, tokens, label):
    for token in tokens:
        if token not in source:
            raise SystemExit(f'{label} missing: {token}')


def forbid(source, tokens, label):
    for token in tokens:
        if token in source:
            raise SystemExit(f'{label} stale/invalid: {token}')


readme = text('README.md')
canonical = text('CANONICAL_PLAN.md')
gap = text('COMPLETION_GAP_AUDIT.md')

must(readme, (
    '## Current version: 0.1.0-alpha.49',
    '## Alpha.49 — bounded selected-area balanced earthworks',
    'maximum footprint **9×9**',
    'initial fill volume may not exceed initial cut volume',
    'project-local earth bank',
    'not an ItemStack, settlement resource, currency, cargo or reusable balance',
    'existing shared `건설 주민`',
    'does **not** claim imported-fill projects',
    'single authority for outpost transport',
    'builder walks from actual settlement storage carrying real wood/stone stacks',
), 'alpha.49 README')
forbid(readme, ('## Current version: 0.1.0-alpha.48',), 'alpha.49 README')

must(canonical, (
    'Current canonical implementation: **0.1.0-alpha.49**',
    'Current functional families are exactly **15**',
    'builder walks from actual settlement storage carrying real wood/stone stacks',
    'Transport workers belong to a specific outpost',
    'pause at unloaded route boundaries',
    'single authority for outpost transport',
    'tier-visible public works',
    '## 6. Alpha.49 selected-area civil works',
    'maximum 9×9 footprint',
    '**fill volume must be <= cut volume**',
    '`earthBank` persists for save/reload correctness but is not a resource ledger',
    'Alpha.49 reuses `SettlementConstructionService.ensureBuilder`',
    '## 14. Current playable slice after Alpha.49',
    '1. **larger civil-engineering second pass**',
    'full companion lock fresh-world client/server runtime',
), 'alpha.49 canonical plan')
forbid(canonical, (
    '## 14. Current playable slice after Alpha.48',
    'Current canonical implementation: **0.1.0-alpha.48**',
), 'alpha.49 canonical plan')

must(gap, (
    '현재 구현 기준: `0.1.0-alpha.49`',
    '| 선택 영역 절토/성토 | **완료/부분** | Alpha.49 DOMAIN 9×9 balanced-earth first pass |',
    '| 외부 토사 반입/대형 성토 | **미구현** |',
    '| 대형 협곡 다리/터널/기념비급 토목 | **미구현** |',
    '### Alpha.49 selected-area civil-work 감사',
    '**fill > cut이면 착공 거부**',
    'earthBank는 save/reload용 project-local relocation accounting',
    '기존 `건설 주민`을 재사용하고 두 번째 builder authority 없음',
    '현재 functional family는 정확히 **15**다',
    '1. **larger civil-engineering second pass**',
    '## 10. Alpha.49 추가 실플레이 acceptance',
    '## 11. 완료 판정 금지선',
), 'alpha.49 completion gap audit')
forbid(gap, (
    '현재 구현 기준: `0.1.0-alpha.48`',
    '| 선택 영역 절토/성토 | **미구현** |',
), 'alpha.49 completion gap audit')

print('Frontier Settlement alpha.49 canonical docs audit: PASS')
