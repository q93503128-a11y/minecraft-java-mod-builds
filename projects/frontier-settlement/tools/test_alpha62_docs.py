#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.62','## Alpha.62 — road-bound remote sentry physical armament','food reserve first -> metal reserve second -> weapon third','same outpost-assigned transporter','exact physically equipped external weapon once','there is still only one authority for long-distance outpost transport'),'alpha.62 README')
must(can,('Current canonical implementation: **0.1.0-alpha.62**','### Alpha.62 road-bound remote-sentry physical armament','food reserve -> metal reserve -> one external weapon','same `MILITARY_RETURN_TRIP_TAG` / `MILITARY_SUPPLY_TRIP_TAG`','## 14. Current playable slice after Alpha.62','## 15. Unfinished original-scope priorities after Alpha.62','Alpha.62 remote military weapon road-haul/local-equip/death-recovery'),'alpha.62 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.62`','Alpha.57 본진 병영 + Alpha.62 원격 위험지역 전초 real external-weapon MAINHAND 물리 보급','원격 위험지역 전초 실물 무기 역보급은 남음','### Alpha.62 원격 군사 실물 무기 역보급 감사','food shortage -> metal shortage -> external weapon1','군사 전초도 같은 도로 운송자가 역방향 보급','there is still only one authority for long-distance outpost transport','route unload, save/reload, sentry death/recruit 반복 no-dup acceptance'),'alpha.62 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.62"','Alpha.62 extends the existing military reverse-supply transporter'),'alpha.62 lock')
print('Frontier Settlement alpha.62 canonical docs audit: PASS')
