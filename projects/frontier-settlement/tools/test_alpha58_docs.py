#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.58','## Alpha.58 — multiplayer snapshot/session pre-acceptance hardening','executesOn(HandlerThread.MAIN)','broadcasts the same authoritative snapshot to every connected player','ClientPlayerNetworkEvent.LoggingOut','pre-acceptance hardening','Long survival + two-player gameplay'),'alpha.58 README')
must(can,('Current canonical implementation: **0.1.0-alpha.58**','### Alpha.58 multiplayer pre-acceptance hardening','HandlerThread.MAIN','republishes the same authoritative snapshot to all connected players','client `LoggingOut` resets','Remaining acceptance is intentionally real-play','after Alpha.58'),'alpha.58 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.58`','2인 snapshot/session 정합 pre-hardening | **완료/부분**','### Alpha.58 멀티 snapshot/session pre-acceptance 감사','실제 장시간 2인 acceptance는 아직 미완료','Alpha.58 simultaneous player confirm/login-refresh/logout-reset/reconnect acceptance'),'alpha.58 gap')
print('Frontier Settlement alpha.58 canonical docs audit: PASS')
