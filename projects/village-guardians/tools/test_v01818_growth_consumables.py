#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

def main():
    assert "mod_version=" in (ROOT / "gradle.properties").read_text(encoding="utf-8")

    consumable = read("VillageConsumableSystem.java")
    for key in ("BANDAGE", "CLEANSER", "STIMULANT", "AEGIS_TONIC", "ARCANE_CATALYST", "FIELD_REPAIR_KIT"):
        assert key in consumable
    assert "VillagePlacedTurretSystem.fieldRepairNearest" in consumable
    assert "ARCANE_SURGE_UNTIL" in consumable and "1.20f" in consumable
    identity = read("VillageConsumableIdentity.java")
    assert "villageguardians_consumable_id" in identity
    assert "VillageConsumableIdentity.stamp(stack, consumable.id())" in consumable
    assert "Consumable.fromId(VillageConsumableIdentity.id(stack))" in consumable

    progression = read("VillageProgressionSystem.java")
    assert 'Component.literal("마을 배급 식량")' in progression
    assert "skillHallPowerMultiplier" in progression and "skillHallDurationMultiplier" in progression
    assert "barracksLevel * 2 + skillRank(player)" not in progression
    assert "Math.min(7, research + barracksSupport + skillRank(player) / 2)" in progression

    equipment = read("VillageEquipmentRaritySystem.java")
    assert "MAX_ENHANCEMENT = 30" in equipment
    assert "maximumEnhancement(ItemStack stack)" in equipment
    for cap in ("return 30", "return 28", "return 26", "return 25", "return 24", "return 22", "return 20"):
        assert cap in equipment
    assert "enhancementAttackBonus" in equipment and "master * 0.0225f" in equipment
    assert "|| item == Items.MACE" in equipment and 'Items.MACE) return "공성 전투망치"' in equipment

    merc = read("VillageMercenarySystem.java")
    assert "MAX_LEVEL = 60" in merc
    assert "killsRequiredForLevel" in merc and "mercenaryPower" in merc
    assert "42.0 + Math.min(48.0, rank * 0.80)" in merc
    assert "8.0 + Math.min(13.0, rank * 0.22)" in merc
    assert "Math.min(5, LEVELS.getOrDefault" not in merc
    assert "Math.min(MAX_LEVEL, LEVELS.getOrDefault(uuid, 1))" in merc

    fortress = read("VillageFortressTerrain.java")
    assert "buildDefenderGalleries" in fortress
    assert "murderHole" in fortress
    assert "isFiringBayOffset" in fortress and "phase == 0 || phase == 1 || phase == 11" in fortress
    assert "firingBay && y >= 3 && y <= 4" in fortress

    turret = read("VillagePlacedTurretSystem.java")
    assert "fieldRepairNearest" in turret and "12.0 * 12.0" in turret

    role = read("VillageRoleSkillSystem.java")
    assert "VillageProgressionSystem.skillHallPowerMultiplier()" in role
    assert "VillageProgressionSystem.skillHallDurationMultiplier()" in role
    assert "VillageConsumableSystem.skillMultiplier(player)" in role

    controller = read("VillageUiController.java")
    assert '"consumable:" + consumable.id()' in controller
    assert 'case "buy_food"' not in controller
    assert 'case "claim_bread"' in controller
    assert "기술 위력 +" in controller and "재사용 효율 +" in controller

    shop = read("VillageShopCatalogScreen.java")
    assert 'action.startsWith("consumable:")' in shop
    assert 'action.equals("buy_food")' not in shop

    print("[PASS] v0.18.18 tactical consumables replace duplicate paid food")
    print("[PASS] endgame enhancement uses per-family caps and diminishing returns")
    print("[PASS] research hall, wall combat galleries, and Lv.60 mercenary growth are wired")

if __name__ == "__main__":
    main()
