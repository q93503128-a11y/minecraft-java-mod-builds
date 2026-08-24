#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
original=text('ORIGINAL_DESIGN_v0.2.md'); readme=text('README.md'); canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(original,('도로는 시작점·끝점·필요 시 중간점만 지정한다.','경로는 급경사와 건물을 피하고, 작은 계단·교량을 자동으로 포함한다.','과도한 월드 수정은 금지한다.'),'original scope')
must(readme,('## Current version: 0.1.0-alpha.54','## Alpha.54 — bounded one-bend tunnels and physical portals','24 centerline cells','one 90-degree bend','3 tunnel centers','5 blocks wide × 4 blocks high','22 real-stone units','single authority for outpost transport','Transport workers belong to a specific outpost','pause at unloaded route boundaries'),'alpha.54 README')
must(canonical,('Current canonical implementation: **0.1.0-alpha.54**','### Alpha.54 bounded one-bend tunnel / physical portal pass','at most one90-degree','at least3 tunnel centers','two 5-wide × 4-high stone-brick portal frames','each run adds22 real-stone portal units','there is still only one authority for long-distance outpost transport','## 14. Current playable slice after Alpha.54','## 15. Unfinished original-scope priorities after Alpha.54'),'alpha.54 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.54`','### Alpha.54 one-bend tunnel / physical portal 감사','90도 turn 최대1회','양쪽 tunnel leg 최소3','5폭 × 4높이','run당22 실제 stone 비용','더 큰 토목은 자동 다음 우선순위가 아니라','## 11. 완료 판정 금지선'),'alpha.54 gap')
print('Frontier Settlement alpha.54 canonical docs audit: PASS')
