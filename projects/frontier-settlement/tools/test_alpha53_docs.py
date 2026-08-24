#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
original=text('ORIGINAL_DESIGN_v0.2.md'); readme=text('README.md'); canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(original,('도로는 시작점·끝점·필요 시 중간점만 지정한다.','경로는 급경사와 건물을 피하고, 작은 계단·교량을 자동으로 포함한다.','과도한 월드 수정은 금지한다.'),'original scope')
must(readme,('## Current version: 0.1.0-alpha.53','## Alpha.53 — bounded straight road tunnels','straight tunnel up to 24 centerline cells','3 blocks wide','3 blocks high','PROFILE_TUNNEL=2','TUNNEL_STEP_OFFSET=1_500_000','no drops','frontier-town + construction office'),'alpha.53 README')
must(canonical,('Current canonical implementation: **0.1.0-alpha.53**','### Alpha.53 bounded straight road tunnels','at most24 centerline cells','PROFILE_TUNNEL=2','1.5M..<2M','width3','clear height3','single authority for outpost transport','there is still only one authority for long-distance outpost transport','## 14. Current playable slice after Alpha.53','## 15. Unfinished original-scope priorities after Alpha.53'),'alpha.53 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.53`','### Alpha.53 bounded tunnel 감사','직선 도로 터널','max24','minimum cover4','successful `setBlock(AIR)`','no drops','## 11. 완료 판정 금지선'),'alpha.53 gap')
print('Frontier Settlement alpha.53 canonical docs audit: PASS')
