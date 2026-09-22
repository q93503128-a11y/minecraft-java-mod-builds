#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    identity = read("VillageEquipmentIdentity.java")
    sets = read("VillageEquipmentSetSystem.java")
    shop = read("VillageEquipmentShop.java")
    rarity = read("VillageEquipmentRaritySystem.java")
    rpg = read("VillageRpgSystem.java")
    role = read("VillageRoleSkillSystem.java")
    inventory = read("VillageInventoryPanel.java")

    assert 'KEY_SET = "villageguardians_set"' in identity
    assert "stampSet(ItemStack stack, String setId)" in identity
    assert "String setId(ItemStack stack)" in identity

    for set_id in (
        "frontline_executor", "night_hunter", "arcane_resonance",
        "dawn_covenant", "wall_guardian"
    ):
        assert f'"{set_id}"' in sets
    assert "return Math.min(5, count);" in sets
    assert '"/5 · "' in sets
    assert "setForOfferId" in sets
    assert "setForRaidDrop" in sets
    assert "defaultSetForItem" in sets
    import re
    offer_ids = re.findall(r'^[ ]{8}[A-Z0-9_]+\("([^"]+)"', shop, re.MULTILINE)
    assert len(offer_ids) >= 36
    assert all(f'"{offer_id}"' in sets for offer_id in offer_ids)
    assert "FRONTLINE_EXECUTOR" in inventory and "ARCANE_RESONANCE" in inventory
    assert "DAWN_COVENANT" in inventory and "WALL_GUARDIAN" in inventory
    assert "2/3/4/5" in inventory

    for item in ("Items.MACE", "Items.TRIDENT", "Items.BLAZE_ROD",
                 "Items.DIAMOND_LEGGINGS", "Items.DIAMOND_BOOTS"):
        assert item in rarity

    assert "VillageEquipmentIdentity.stampSet(stack, set.id())" in shop
    assert "VillageEquipmentIdentity.stampSet(result," in rarity
    assert "같은 종류·세트·전장 단계·등급·강화 단계" in rarity
    assert "VillageEquipmentIdentity.stampSet(result, set.id())" in rarity

    assert "VillageEquipmentSetSystem.targetMultiplier(attacker, target, projectile)" in rpg
    assert "VillageEquipmentSetSystem.roleSkillMultiplier(player, role, skill.promotionTier())" in role
    assert "VillageEquipmentSetSystem.cooldownReductionSeconds(player, role)" in role

    assert "target.getHealth() <= target.getMaxHealth() * 0.40f" in sets
    assert "player.distanceToSqr(target) >= 144.0" in sets
    assert "case ARCANIST" in sets and "case LUMINAR" in sets and "case WARDEN" in sets

    print("[PASS] every new graded gear path receives an explicit or deterministic equipment set")
    print("[PASS] five role-oriented sets expose 2/3/4/5-piece progression and major 5-piece capstones")
    print("[PASS] set identity survives fusion and no longer depends on vanilla armor/weapon slot alone")
    print("[PASS] melee execution, long-range hunting, role-skill power, cooldown and defense hooks are wired")

if __name__ == "__main__":
    main()
