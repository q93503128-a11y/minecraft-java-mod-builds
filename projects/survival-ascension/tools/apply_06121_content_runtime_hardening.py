#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"{label}: expected anchor not found in {path.relative_to(ROOT)}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


incident = ROOT / "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java"
replace_once(
    incident,
    '''        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()\n                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective), false);''',
    '''        String reinforcementNote = active.reinforcementCount > 0 ? " §7· 이변 개체 1체 포함" : "";\n        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()\n                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective + reinforcementNote), false);''',
    "rare incident player-facing reinforcement state",
)

guide = ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java"
replace_once(
    guide,
    '''                h("전선 현지 보급"), p("원정 작전·전초 방어·요새 방어는 현장 보급권과 별도로 출발 전초의 등록 통+연결 창고 통에 각 작전이 요구하는 실물 재고가 있어야 시작합니다. 필요한 수량은 각 작전의 서버 규칙이 직접 검증하며 인벤토리나 다른 거점 재고로 대체하지 않습니다. 먼 전초는 직접 적재하거나 물리 화물로 보급해야 합니다."),''',
    '''                h("전선 현지 보급"), p("원정 작전·전초 방어·요새 방어는 현장 보급권과 별도로 출발 전초의 등록 통+연결 창고 통에 각 작전이 요구하는 실물 재고가 있어야 시작합니다. 원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3, 전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12, 요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32가 필요합니다. 인벤토리나 다른 거점 재고로 대체하지 않으며 먼 전초는 직접 적재하거나 물리 화물로 보급해야 합니다."),''',
    "frontline local-supply guide costs",
)

# The 0.58 regression wrapper is intentionally translated to the current 0.61 contracts.
# Keep the historical test immutable while making its runtime copy compare against current canonical values.
release_test = ROOT / "tools/test_release_content_pack.py"
text = release_test.read_text(encoding="utf-8")
text = text.replace("산업 가공소 완공 → 통 4블록 이내", "산업 가공소 완공 → 통/공용 보급고 4블록 이내")

translation_anchor = "    ('Math.min(13, base + bonus)', 'Math.min(21, base + bonus)'),\n"
current_translations = """    ('WOODCUTTING -> { early = 1.60D; late = 1.25D; }', 'WOODCUTTING -> { early = 2.50D; late = 2.00D; }'),
    ('HARVESTING -> { early = 1.50D; late = 1.20D; }', 'HARVESTING -> { early = 3.00D; late = 2.50D; }'),
    ('COMBAT -> { early = 1.25D; late = 1.15D; }', 'COMBAT -> { early = 4.00D; late = 3.50D; }'),
    ('CONSTRUCTION -> { early = 2.75D; late = 1.75D; }', 'CONSTRUCTION -> { early = 5.00D; late = 3.50D; }'),
    ('MOBILITY -> { early = 2.10D; late = 1.40D; }', 'MOBILITY -> { early = 4.00D; late = 3.00D; }'),
    ('OutpostSiegeSystem.isActive(player) && !consumeLocalOutpostSupply', 'OutpostSiegeSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))'),
    ('ExpeditionOperationSystem.isActive(player) && !consumeLocalOutpostSupply', 'ExpeditionOperationSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))'),
"""
if "('WOODCUTTING -> { early = 1.60D; late = 1.25D; }', 'WOODCUTTING -> { early = 2.50D; late = 2.00D; }')" not in text:
    if translation_anchor not in text:
        raise SystemExit("content audit translation anchor missing")
    text = text.replace(translation_anchor, translation_anchor + current_translations)
elif "('OutpostSiegeSystem.isActive(player) && !consumeLocalOutpostSupply', 'OutpostSiegeSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))')" not in text:
    if translation_anchor not in text:
        raise SystemExit("content audit transactional translation anchor missing")
    text = text.replace(translation_anchor, translation_anchor + """    ('OutpostSiegeSystem.isActive(player) && !consumeLocalOutpostSupply', 'OutpostSiegeSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))'),
    ('ExpeditionOperationSystem.isActive(player) && !consumeLocalOutpostSupply', 'ExpeditionOperationSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))'),
""")
release_test.write_text(text, encoding="utf-8")

# Fail closed if current contracts vanished or player guidance drifted from the authoritative loadouts.
incident_text = incident.read_text(encoding="utf-8")
guide_text = guide.read_text(encoding="utf-8")
production_text = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java").read_text(encoding="utf-8")
release_text = release_test.read_text(encoding="utf-8")
assert 'active.reinforcementCount > 0 ? " §7· 이변 개체 1체 포함" : ""' in incident_text
assert "산업 가공소 완공 → 통/공용 보급고 4블록 이내" in release_text
assert "산업 가공소 완공 → 통 4블록 이내" not in release_text
for player_copy, runtime_contract in (
    ("원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3", (
        'new LocalRequirement("식량(밀/당근/감자/비트)", 12',
        'new LocalRequirement("철 주괴", 3',
        'new LocalRequirement("연료(석탄 또는 숯)", 3',
    )),
    ("전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12", (
        'new LocalRequirement("식량(밀/당근/감자/비트)", 16',
        'new LocalRequirement("철 주괴", 5',
        'new LocalRequirement("아무 종류의 통나무", 12',
    )),
    ("요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32", (
        'new LocalRequirement("식량(밀/당근/감자/비트)", 32',
        'new LocalRequirement("철 주괴", 8',
        'new LocalRequirement("석재 벽돌", 32',
    )),
):
    assert player_copy in guide_text
    for token in runtime_contract:
        assert token in production_text
for current in (
    "WOODCUTTING -> { early = 2.50D; late = 2.00D; }",
    "HARVESTING -> { early = 3.00D; late = 2.50D; }",
    "COMBAT -> { early = 4.00D; late = 3.50D; }",
    "CONSTRUCTION -> { early = 5.00D; late = 3.50D; }",
    "MOBILITY -> { early = 4.00D; late = 3.00D; }",
    "OutpostSiegeSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))",
    "ExpeditionOperationSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))",
):
    assert current in release_text

print("Applied Survival Ascension 0.61.21 content/runtime hardening")
