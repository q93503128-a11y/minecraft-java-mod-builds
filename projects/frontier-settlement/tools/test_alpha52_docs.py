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

must(original, ('도로는 시작점·끝점·필요 시 중간점만 지정한다.',
                '경로는 급경사와 건물을 피하고, 작은 계단·교량을 자동으로 포함한다.',
                '평탄화를 위해 산 하나를 통째로 삭제하는 식의 과도한 월드 수정은 금지한다.'),
     'original design scope')
must(readme, ('## Current version: 0.1.0-alpha.52',
              '## Alpha.52 — bounded long bridges and ravine crossings', '**24 centerline cells**',
              '**4 blocks**', '**12 blocks**', '`RoadConstructionState.bridge_supports`',
              'successful `setBlock` precedes carried-stone shrink and state advance',
              'final road repair no longer places missing road/bridge blocks for free',
              'Tunnels, more complex curved/deeper monumental crossings',
              'single authority for outpost transport', 'Transport workers belong to a specific outpost',
              'pause at unloaded route boundaries'), 'alpha.52 README')
forbid(readme, ('## Current version: 0.1.0-alpha.51', 'Canonical Alpha.51 CI order:'), 'alpha.52 README stale')
must(canonical, ('Current canonical implementation: **0.1.0-alpha.52**',
                 '### Alpha.52 bounded long-bridge / ravine crossing', 'at most24 centerline cells',
                 'optional `bridge_supports`', 'within12 blocks', 'successful world `setBlock` happens before carried-stone shrink/state advance',
                 'final validation/repair also requires physical stone',
                 'single authority for outpost transport', 'there is still only one authority for long-distance outpost transport',
                 '## 14. Current playable slice after Alpha.52', '## 15. Unfinished original-scope priorities after Alpha.52',
                 '**tunnel / deeper monumental crossing civil-engineering pass**'), 'alpha.52 canonical')
must(gap, ('현재 구현 기준: `0.1.0-alpha.52`', '### Alpha.52 long-bridge / ravine crossing 감사',
           '| 대형 협곡/장교량 | **완료/부분** | Alpha.52 max24',
           '| 터널/더 깊은 기념비급 토목 | **미구현/부분** |',
           'old saves default empty', '자연 지반을 최대12블록',
           'world setBlock 성공 → carried stone consume → road state advance',
           'free repair 없음', '## 11. 완료 판정 금지선'), 'alpha.52 gap')
forbid(gap, ('현재 구현 기준: `0.1.0-alpha.51`',
             '- ravine-scale / long bridge / tunnel larger civil engineering breadth;'), 'alpha.52 gap stale')

print('Frontier Settlement alpha.52 canonical docs audit: PASS')
