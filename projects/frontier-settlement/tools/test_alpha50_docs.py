#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(name): return (ROOT / name).read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label} stale/invalid: {token}')

readme = text('README.md')
canonical = text('CANONICAL_PLAN.md')
gap = text('COMPLETION_GAP_AUDIT.md')
original = text('ORIGINAL_DESIGN_v0.2.md')

must(original, ('Frontier Settlement',), 'original design must remain present')

must(readme, (
    '## Current version: 0.1.0-alpha.50',
    '## Alpha.50 — 13×13 civil work with physical imported fill',
    'maximum footprint **13×13**',
    '**5 blocks of cut** or **5 blocks of fill**',
    'real `DIRT` / `COARSE_DIRT`',
    'successful placement shrinks one carried item',
    'physically returns the remaining ItemStack',
    '가상 토사 0',
    '## Alpha.49 — historical balanced-earth first pass',
    '**9×9 / ±4**',
    'retaining-heavy large terraces',
    'ravine-scale works', 'long bridges', 'tunnels',
    'single authority for outpost transport',
    'Transport workers belong to a specific outpost',
    'pause at unloaded route boundaries',
    'builder walks from actual settlement storage carrying real wood/stone stacks',
), 'alpha.50 README')
forbid(readme, ('## Current version: 0.1.0-alpha.49',), 'alpha.50 README')

must(canonical, (
    'Current canonical implementation: **0.1.0-alpha.50**',
    'Current functional families are exactly **15**',
    'builder walks from actual settlement storage carrying real wood/stone stacks',
    'Transport workers belong to a specific outpost',
    'pause at unloaded route boundaries',
    'single authority for outpost transport',
    'tier-visible public works',
    '군사 전초도 같은 도로 운송자가 역방향 보급',
    '위험지역 군사 역할이 우선',
    '## 6. Selected-area civil works — Alpha.49 history and Alpha.50 current',
    '**9×9 / cut-fill ±4**',
    'maximum **13×13** footprint',
    'max cut **5** blocks/column',
    'initial imported requirement is `max(0, fillBlocks - cutBlocks)`',
    'successful setBlock -> shrink carried ItemStack by1',
    'project enters a persisted return phase',
    'Retaining-heavy larger terraces, ravine-scale civil work, long bridges, tunnels and monumental engineering remain unfinished.',
    '## 14. Current playable slice after Alpha.50',
    '## 15. Unfinished original-scope priorities after Alpha.50',
    'full companion lock fresh-world client/server runtime',
), 'alpha.50 canonical plan')
forbid(canonical, (
    'Current canonical implementation: **0.1.0-alpha.49**',
    '## 14. Current playable slice after Alpha.49',
    '## 15. Unfinished original-scope priorities after Alpha.49',
), 'alpha.50 canonical plan')

must(gap, (
    '현재 구현 기준: `0.1.0-alpha.50`',
    '| 선택 영역 절토/성토 | **완료/부분** | Alpha.50 DOMAIN 13×13 / ±5 current pass |',
    '| 외부 토사 반입/대형 성토 | **완료/부분** | Alpha.50 real dirt/coarse-dirt imported fill first expansion; 더 큰 성토는 남음 |',
    '| 대형 옹벽/테라스 | **미구현/부분** |',
    '| 대형 협곡 다리/터널/기념비급 토목 | **미구현** |',
    '### Alpha.49 historical selected-area 감사',
    '**9×9 / ±4**',
    '### Alpha.50 physical imported-fill 감사',
    '최대 **13×13**, column cut/fill 각각 최대 **5**',
    'actual storage까지 걸어가 최대16개 실제 DIRT/COARSE_DIRT를 MAINHAND로 추출',
    '`setBlock` 성공 뒤에만 carried ItemStack 1개 shrink + project step advance',
    'persisted return phase',
    'retaining-heavy terrace, ravine-scale work, long bridge, tunnel, monumental engineering은 여전히 미구현',
    '현재 functional family는 정확히 **15**다',
    '## 9. 현재 남은 우선순위',
    '## 10. Alpha.50 추가 실플레이 acceptance',
    '## 11. 완료 판정 금지선',
), 'alpha.50 completion gap audit')
forbid(gap, (
    '현재 구현 기준: `0.1.0-alpha.49`',
    '| 외부 토사 반입/대형 성토 | **미구현** | 현재 fill은 같은 프로젝트의 real cut volume 안에서만 허용 |',
    '## 10. Alpha.49 추가 실플레이 acceptance',
), 'alpha.50 completion gap audit')

print('Frontier Settlement alpha.50 canonical docs audit: PASS')
