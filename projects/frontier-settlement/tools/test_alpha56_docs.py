#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.56','## Alpha.56 — soft biome-aware outpost specialization','+8 log evidence','+24 field evidence','+8 exposed-stone +1 ore evidence','+6 exposed-stone evidence','common biome tags','Transport workers belong to a specific outpost','pause at unloaded route boundaries'),'alpha.56 README')
must(can,('Current canonical implementation: **0.1.0-alpha.56**','### Alpha.56 common-biome-tag outpost specialization','physical local evidence remains primary','forest/dense vegetation adds8','plains/savanna adds24','mountain/hill adds8','badlands/sandy adds6','there is still only one authority for long-distance outpost transport','## 15. Unfinished original-scope priorities after Alpha.56'),'alpha.56 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.56`','### Alpha.56 common-biome-tag 전초 특화 감사','biome-aware companion specialization | **완료/부분**','generic companion-biome-aware specialization은 **완료/부분**','## 11. 완료 판정 금지선'),'alpha.56 gap')
print('Frontier Settlement alpha.56 canonical docs audit: PASS')
