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

original = text('ORIGINAL_DESIGN_v0.2.md')
readme = text('README.md')
canonical = text('CANONICAL_PLAN.md')
gap = text('COMPLETION_GAP_AUDIT.md')

must(original, ('# 개척마을 프로젝트 정본 기획서 v0.2 — 저장소 복원본',
                '후반에는 선택 영역 평탄화/절토/성토를 보조 기능으로 제공할 수 있다.',
                '평탄화를 위해 산 하나를 통째로 삭제하는 식의 과도한 월드 수정은 금지한다.'),
     'original design scope must remain')

must(readme, ('## Current version: 0.1.0-alpha.51',
              '## Alpha.51 — 17×17 retaining-heavy terraces',
              'maximum selected footprint is **17×17**', '**7 blocks of cut** or **7 blocks of fill**',
              'at least **3 blocks** above natural exterior ground', 'retaining height is capped at **7 blocks**',
              'real `COBBLESTONE`', 'only after successful `setBlock` shrinks one ItemStack',
              'cut -> retaining wall -> fill -> carried-material return',
              '## Alpha.50 — 13×13 civil work with physical imported fill',
              '## Alpha.49 — historical balanced-earth first pass',
              'Ravine-scale crossings, long bridges, tunnels and monumental civil engineering remain unfinished',
              'single authority for outpost transport', 'Transport workers belong to a specific outpost',
              'pause at unloaded route boundaries'), 'alpha.51 README')
forbid(readme, ('## Current version: 0.1.0-alpha.50', 'Canonical Alpha.50 CI order:'), 'alpha.51 README stale')

must(canonical, ('Current canonical implementation: **0.1.0-alpha.51**',
                 'Current functional families are exactly **15**',
                 'builder walks from actual settlement storage carrying real wood/stone stacks',
                 'Transport workers belong to a specific outpost', 'pause at unloaded route boundaries',
                 'single authority for outpost transport', 'there is still only one authority for long-distance outpost transport',
                 'tier-visible public works', '군사 전초도 같은 도로 운송자가 역방향 보급', '위험지역 군사 역할이 우선',
                 '## 6. Selected-area civil works — Alpha.49/50 history and Alpha.51 current',
                 '### Alpha.51 current retaining-heavy terrace expansion', '**17×17 / ±7**',
                 'one-block outer protection ring', 'exact `COBBLESTONE` ItemStacks',
                 '`cut -> retaining -> fill -> return`', '`PHASE_RETAIN=3`',
                 'Ravine-scale crossings, long bridges, tunnels and monumental engineering remain unfinished.',
                 '## 14. Current playable slice after Alpha.51',
                 '## 15. Unfinished original-scope priorities after Alpha.51',
                 '**ravine-scale / long bridge / tunnel civil-engineering pass**',
                 'full companion lock fresh-world client/server runtime'), 'alpha.51 canonical plan')
forbid(canonical, ('Current canonical implementation: **0.1.0-alpha.50**',
                   '## 14. Current playable slice after Alpha.50',
                   '## 15. Unfinished original-scope priorities after Alpha.50'), 'alpha.51 canonical stale')

must(gap, ('현재 구현 기준: `0.1.0-alpha.51`',
           '| 선택 영역 절토/성토 | **완료/부분** | Alpha.51 DOMAIN 17×17 / ±7 current pass |',
           '| 대형 옹벽/테라스 | **완료/부분** | Alpha.51 1-block outer ring',
           '| Alpha.51 옹벽 자재 | **완료/부분** | exact COBBLESTONE',
           '### Alpha.51 retaining-heavy terrace 감사',
           'phase order cut→retaining→fill→return', 'max16 COBBLESTONE MAINHAND',
           'successful setBlock→1 item shrink→step advance',
           'ravine-scale work, long bridge, tunnel, monumental engineering은 여전히 미구현',
           '현재 functional family는 정확히 **15**다',
           '## 9. 현재 남은 우선순위',
           '## 10. Alpha.51 추가 실플레이 acceptance',
           '## 11. 완료 판정 금지선'), 'alpha.51 gap audit')
forbid(gap, ('현재 구현 기준: `0.1.0-alpha.50`', '## 10. Alpha.50 추가 실플레이 acceptance'), 'alpha.51 gap stale')

print('Frontier Settlement alpha.51 canonical docs audit: PASS')
