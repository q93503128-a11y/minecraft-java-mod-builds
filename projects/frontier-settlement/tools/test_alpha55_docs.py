#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); original=text('ORIGINAL_DESIGN_v0.2.md')
must(original,('탐험','전초'),'original exploration/outpost scope')
must(readme,('## Current version: 0.1.0-alpha.55','## Alpha.55 — non-farmable exploration knowledge feeds existing outposts','survey level 0–3','conquest level 0–2','64 wood + 44 stone','Transport workers belong to a specific outpost','pause at unloaded route boundaries','there is still only one authority for long-distance outpost transport'),'alpha.55 README')
must(can,('Current canonical implementation: **0.1.0-alpha.55**','### Alpha.55 exploration knowledge -> existing outpost value','surveyLevel = min(3','conquestLevel = min(2','minimum64/44','builder walks from actual settlement storage carrying real wood/stone stacks','single authority for outpost transport','there is still only one authority for long-distance outpost transport','## 15. Unfinished original-scope priorities after Alpha.55'),'alpha.55 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.55`','### Alpha.55 탐험 지식 / 전초 가치 감사','최저64/44','generic exploration-to-settlement value는 **완료/부분**','## 11. 완료 판정 금지선'),'alpha.55 gap')
print('Frontier Settlement alpha.55 canonical docs audit: PASS')
