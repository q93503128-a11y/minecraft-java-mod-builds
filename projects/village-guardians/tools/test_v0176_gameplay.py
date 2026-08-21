#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def text(name):
    return (JAVA / name).read_text(encoding="utf-8")

quick = text("VillageQuickChatSafeScreen.java")
detail = text("VillageActionDetailScreen.java")
rarity = text("VillageEquipmentRaritySystem.java")
shop = text("VillageEquipmentShop.java")
controller = text("VillageUiController.java")
progression = text("VillageProgressionSystem.java")
council = text("VillageCouncilState.java")
enemy = text("VillageEnemyArchetypeSystem.java")
trading = text("VillageTradingSystem.java")
loot = text("VillageRaidLootSystem.java")
raid = text("VillageRaidSystem.java")
result = text("VillageResultScreen.java")
client = text("VillageClientUi.java")

# Current quick communication and detail-first facility surfaces replace the retired fixed-size screens.
assert "VillageUiSafeArea.screen(width, height)" in quick
assert "radiusX" in quick and "radiusY" in quick and "insideDiamond" in quick
assert 'graphics.text(font, "ESC 닫기"' in quick
assert "VillageUiSafeArea.screen(width, height)" in detail
assert "confirmationRequired" in detail and "panelWidth < 390" in detail
assert 'case "building", "management", "funding"' in client

assert "enhanceSelected" in rarity and "EnhancementCandidate" in rarity
assert "enhancementLevel" in rarity and "createNamed" in rarity
assert "offer.rarity().displayName()" in controller and "openForgeEnhancement" in controller
assert "openItemSell" in controller and "openResult" in controller
assert 'case INFIRMARY -> { }' in controller
assert '"train", "전투 훈련' not in controller
assert "experienceMultiplierPercent" in progression and "tickInfirmary" in progression
assert "baseAmount" in council and "experienceMultiplierPercent" in council
assert "building == Building.TOWN_HALL && next == 0" in progression
assert "마을 회관이 파괴되었습니다" in raid
assert "Attributes.MOVEMENT_SPEED" in enemy and "setBaseValue(0.19)" in enemy
assert "case RUSHER -> { }" in enemy
assert "[판매용]" in loot
assert "sellCandidates" in trading and "sellSelected" in trading
assert "VillageResultScreen" in result and 'graphics.centeredText(font, "확인"' in result
assert "VillageEquipmentRaritySystem.createNamed" in shop
print("Village Guardians current gameplay, safe UI, item and defeat contracts passed.")