#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def text(path): return path.read_text(encoding='utf-8')
def write(path, content): path.write_text(content, encoding='utf-8')
def repl(path, old, new):
    src = text(path)
    if old not in src:
        raise SystemExit(f'missing consistency anchor in {path}: {old[:180]!r}')
    write(path, src.replace(old, new, 1))

gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
repl(gap,
'''| 실물 외부무기 군사 armory/loadout | **미구현/부분** | visual sword는 client-only, 실제 Weapons Expanded 보급 루프 없음 |''',
'''| 실물 외부무기 군사 armory/loadout | **완료/부분** | Alpha.57 본진 병영은 real external-weapon MAINHAND loadout 완료/부분; 원격 위험지역 전초 실물 무기 역보급은 남음 |''')
repl(gap,
'''- actual external-weapon physical armory는 아직 완료가 아님.''',
'''- Alpha.48 시점에는 actual external-weapon physical armory가 미완료였고, Alpha.57에서 loaded 본진 병영은 real MAINHAND ItemStack 무장으로 구현됨; 원격 위험지역 전초 무기 역보급은 기존 road transporter authority를 재사용할 수 있을 때만 남은 범위.''')

canonical = ROOT / 'CANONICAL_PLAN.md'
repl(canonical,
'''A physical external-weapon armory/loadout loop remains unfinished. If added, it must use actual ItemStacks and automation and must not require manually opening every soldier.''',
'''At Alpha.48 the physical external-weapon armory/loadout loop was unfinished. Alpha.57 now covers loaded town-barracks soldiers with actual MAINHAND ItemStacks and automation. The remaining remote-sentry extension must reuse the existing road-bound reverse-supply transporter and must not require manually opening every soldier.''')

docs = ROOT / 'tools/test_alpha61_docs.py'
src = text(docs)
old = "must(gap,('현재 구현 기준: `0.1.0-alpha.61`','전초기지 grading 원자성 | **완료/부분**','### Alpha.61 전초 grading transaction 감사','역순 rollback','실제 실패주입, 청크 경계 unload, save/reload acceptance는 아직 남는다'),'alpha.61 gap')"
new = "must(gap,('현재 구현 기준: `0.1.0-alpha.61`','전초기지 grading 원자성 | **완료/부분**','### Alpha.61 전초 grading transaction 감사','역순 rollback','실제 실패주입, 청크 경계 unload, save/reload acceptance는 아직 남는다','실물 외부무기 군사 armory/loadout | **완료/부분**','Alpha.57 본진 병영은 real external-weapon MAINHAND loadout 완료/부분','원격 위험지역 전초 실물 무기 역보급은 남음'),'alpha.61 gap')"
if old not in src:
    raise SystemExit('alpha.61 docs audit gap anchor missing')
src = src.replace(old, new, 1)
old2 = "must(can,('Current canonical implementation: **0.1.0-alpha.61**','### Alpha.61 outpost grade-cell transaction hardening','rolls back successful partial changes in reverse order','persisted outpost construction step advances only after the full grade cell succeeds','## 14. Current playable slice after Alpha.61','## 15. Unfinished original-scope priorities after Alpha.61'),'alpha.61 canonical')"
new2 = "must(can,('Current canonical implementation: **0.1.0-alpha.61**','### Alpha.61 outpost grade-cell transaction hardening','rolls back successful partial changes in reverse order','persisted outpost construction step advances only after the full grade cell succeeds','## 14. Current playable slice after Alpha.61','## 15. Unfinished original-scope priorities after Alpha.61','Alpha.57 now covers loaded town-barracks soldiers with actual MAINHAND ItemStacks','remaining remote-sentry extension must reuse the existing road-bound reverse-supply transporter'),'alpha.61 canonical')"
if old2 not in src:
    raise SystemExit('alpha.61 docs audit canonical anchor missing')
src = src.replace(old2, new2, 1)
write(docs, src)
print('Fixed Alpha.61 canonical military-armament status consistency.')
