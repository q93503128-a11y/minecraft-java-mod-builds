#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.57','## Alpha.57 — automated physical barracks armament','nearest concrete storage container within 160 blocks','exactly one real external weapon ItemStack','EquipmentSlot.MAINHAND','sole recoverable military drop','군사 전초도 같은 도로 운송자가 역방향 보급','there is still only one authority for long-distance outpost transport'),'alpha.57 README')
must(can,('Current canonical implementation: **0.1.0-alpha.57**','### Alpha.57 automated physical barracks armory','shared settlement storage must be fully loaded',"actual weapon becomes the soldier's vanilla MAINHAND equipment",'remote military sentries remain generic','## 15. Unfinished original-scope priorities after Alpha.57','long survival + two-player multiplayer acceptance'),'alpha.57 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.57`','### Alpha.57 본진 병영 실물 무장 감사','본진 병영 실물 외부무기 armory/loadout | **완료/부분**','remote weapon supply는 **군사 전초도 같은 도로 운송자가 역방향 보급**','physical military armory/loadout은 **본진 병영 기준 완료/부분**','## 11. 완료 판정 금지선'),'alpha.57 gap')
print('Frontier Settlement alpha.57 canonical docs audit: PASS')
