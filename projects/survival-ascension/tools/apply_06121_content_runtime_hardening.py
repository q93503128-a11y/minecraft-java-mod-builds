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


# Rare optional expedition reinforcements must be visible to the player when they actually spawned.
incident = ROOT / "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java"
replace_once(
    incident,
    '''        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()\n                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective), false);''',
    '''        String reinforcementNote = active.reinforcementCount > 0 ? " §7· 이변 개체 1체 포함" : "";\n        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()\n                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective + reinforcementNote), false);''',
    "rare incident player-facing reinforcement state",
)

# Local frontline costs have one runtime authority. The guide and status copy must derive from it,
# rather than duplicating balance literals that can drift independently.
production = ROOT / "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java"
replace_once(
    production,
    '''    private ProductionService() {}\n''',
    '''    private ProductionService() {}\n\n    public static String localSupplyGuideText() {\n        return "원정은 " + localSupplyCostText(LocalLoadout.EXPEDITION)\n                + " / 전초 방어는 " + localSupplyCostText(LocalLoadout.OUTPOST_DEFENSE)\n                + " / 요새 방어는 " + localSupplyCostText(LocalLoadout.BASTION_DEFENSE);\n    }\n\n    private static String localSupplyCostText(LocalLoadout loadout) {\n        StringBuilder out = new StringBuilder();\n        for (LocalRequirement requirement : requirements(loadout)) {\n            if (!out.isEmpty()) out.append(" + ");\n            out.append(requirement.label()).append(' ').append(requirement.amount());\n        }\n        return out.toString();\n    }\n''',
    "single-authority local-supply guide formatter",
)
replace_once(
    production,
    '''        player.sendSystemMessage(Component.literal("§7전선 작전은 보급권과 별도로 출발 전초의 실제 통 재고를 소비합니다. 원정=식량(밀/당근/감자/비트)12+철 주괴3+연료(석탄/숯)3 / 방어=식량16+철 주괴5+아무 종류의 통나무12 / 요새=식량32+철 주괴8+석재 벽돌32."));''',
    '''        player.sendSystemMessage(Component.literal("§7전선 작전은 보급권과 별도로 출발 전초의 실제 통 재고를 소비합니다. " + localSupplyGuideText() + "."));''',
    "status local-supply costs derive from runtime authority",
)

guide = ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java"
replace_once(
    guide,
    '''import kr.moonseungjun.survivalascension.production.FreightService;\n''',
    '''import kr.moonseungjun.survivalascension.production.FreightService;\nimport kr.moonseungjun.survivalascension.production.ProductionService;\n''',
    "GuideScreen production authority import",
)
replace_once(
    guide,
    '''                h("전선 현지 보급"), p("원정 작전·전초 방어·요새 방어는 현장 보급권과 별도로 출발 전초의 등록 통+연결 창고 통에 각 작전이 요구하는 실물 재고가 있어야 시작합니다. 필요한 수량은 각 작전의 서버 규칙이 직접 검증하며 인벤토리나 다른 거점 재고로 대체하지 않습니다. 먼 전초는 직접 적재하거나 물리 화물로 보급해야 합니다."),''',
    '''                h("전선 현지 보급"), p("원정 작전·전초 방어·요새 방어는 현장 보급권과 별도로 출발 전초의 등록 통+연결 창고 통에 각 작전이 요구하는 실물 재고가 있어야 시작합니다. " + ProductionService.localSupplyGuideText() + "가 필요합니다. 인벤토리나 다른 거점 재고로 대체하지 않으며 먼 전초는 직접 적재하거나 물리 화물로 보급해야 합니다."),''',
    "frontline local-supply guide derives from runtime authority",
)

# Current-source regression: retain the existing anti-duplication check and assert the new authority seam.
current_test = ROOT / "tools/test_current_source.py"
replace_once(
    current_test,
    '''require("식량(밀/당근/감자/비트) 60" not in guide and "원정은 식량(밀/당근/감자/비트) 12" not in guide,\n        "guide contains duplicated hard-coded frontline supply balances")''',
    '''require("식량(밀/당근/감자/비트) 60" not in guide and "원정은 식량(밀/당근/감자/비트) 12" not in guide,\n        "guide contains duplicated hard-coded frontline supply balances")\nproduction = text(JAVA / "production/ProductionService.java")\nrequire("ProductionService.localSupplyGuideText()" in guide and "localSupplyGuideText()" in production,\n        "guide no longer derives local frontline costs from ProductionService authority")''',
    "current local-supply authority regression",
)

# The 0.58 regression wrapper is intentionally translated to current 0.61 contracts.
# Historical source remains immutable; the translated test must recognize the current single-authority UI seam.
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

for old, dynamic in (
    ("원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3", "ProductionService.localSupplyGuideText()"),
    ("전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12", "ProductionService.localSupplyGuideText()"),
    ("요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32", "ProductionService.localSupplyGuideText()"),
):
    text = text.replace(old, dynamic)
release_test.write_text(text, encoding="utf-8")

# Fail closed if current contracts vanished or player guidance stopped using the authoritative loadouts.
incident_text = incident.read_text(encoding="utf-8")
guide_text = guide.read_text(encoding="utf-8")
production_text = production.read_text(encoding="utf-8")
release_text = release_test.read_text(encoding="utf-8")
current_test_text = current_test.read_text(encoding="utf-8")
assert 'active.reinforcementCount > 0 ? " §7· 이변 개체 1체 포함" : ""' in incident_text
assert "산업 가공소 완공 → 통/공용 보급고 4블록 이내" in release_text
assert "산업 가공소 완공 → 통 4블록 이내" not in release_text
assert "ProductionService.localSupplyGuideText()" in guide_text
assert "원정은 식량(밀/당근/감자/비트) 12" not in guide_text
assert "localSupplyCostText(LocalLoadout.EXPEDITION)" in production_text
assert "localSupplyCostText(LocalLoadout.OUTPOST_DEFENSE)" in production_text
assert "localSupplyCostText(LocalLoadout.BASTION_DEFENSE)" in production_text
assert "ProductionService.localSupplyGuideText()" in current_test_text
for runtime_contract in (
    'new LocalRequirement("식량(밀/당근/감자/비트)", 12',
    'new LocalRequirement("철 주괴", 3',
    'new LocalRequirement("연료(석탄 또는 숯)", 3',
    'new LocalRequirement("식량(밀/당근/감자/비트)", 16',
    'new LocalRequirement("철 주괴", 5',
    'new LocalRequirement("아무 종류의 통나무", 12',
    'new LocalRequirement("식량(밀/당근/감자/비트)", 32',
    'new LocalRequirement("철 주괴", 8',
    'new LocalRequirement("석재 벽돌", 32',
):
    assert runtime_contract in production_text
for current in (
    "WOODCUTTING -> { early = 2.50D; late = 2.00D; }",
    "HARVESTING -> { early = 3.00D; late = 2.50D; }",
    "COMBAT -> { early = 4.00D; late = 3.50D; }",
    "CONSTRUCTION -> { early = 5.00D; late = 3.50D; }",
    "MOBILITY -> { early = 4.00D; late = 3.00D; }",
    "OutpostSiegeSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))",
    "ExpeditionOperationSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared))",
    "ProductionService.localSupplyGuideText()",
):
    assert current in release_text

print("Applied Survival Ascension 0.61.21 content/runtime hardening")