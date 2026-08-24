#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.63','## Alpha.63 — transport transaction hardening','actual outpost delivery point','returns it through the existing road/town-deposit path','exact carried MAINHAND ItemStack once','Transport workers belong to a specific outpost','pause at unloaded route boundaries','there is still only one authority for long-distance outpost transport'),'alpha.63 README')
must(can,('Current canonical implementation: **0.1.0-alpha.63**','### Alpha.63 transporter transaction hardening','weaponSupplyShortage(...)','exact carried weapon remains in transporter MAINHAND','tagged transport-worker death','## 14. Current playable slice after Alpha.63','## 15. Unfinished original-scope priorities after Alpha.63','there is still only one authority for long-distance outpost transport'),'alpha.63 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.63`','Alpha.62 원격 전초 exact MAINHAND 물리 보급','### Alpha.63 운송 트랜잭션 하드닝 감사','실제 전초 창고 삽입 직전 다시 검사','MAINHAND ItemStack exact copy1','route unload/save-reload/reconnect/반복 사망 no-dup acceptance','there is still only one authority for long-distance outpost transport'),'alpha.63 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.63"','Alpha.63 hardens the same road transporter transaction boundary'),'alpha.63 lock')
print('Frontier Settlement alpha.63 canonical docs audit: PASS')
