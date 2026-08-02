#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def text(name):
    return (JAVA / name).read_text(encoding="utf-8")

quick = text("VillageQuickChatScreen.java")
facility = text("VillageFacilityScreen.java")
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

assert quick.count("int centerWidth = 56") == 2
assert quick.count("int centerHeight = 18") == 2
assert 'graphics.centeredText(font, "닫기"' in quick
assert "facility_info" in facility and "informationSelected" in facility and "shouldShowBody" in facility
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
print("Village Guardians v0.17.6 gameplay, UI, item and defeat contracts passed.")
