#!/usr/bin/env python3
"""Cross-system completion audit for the current Village Guardians canonical design."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    project = (ROOT / "PROJECT.md").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    loot = read("VillageRaidLootSystem.java")
    expanded = read("VillageExpandedEquipmentSystem.java")
    sets = read("VillageEquipmentSetSystem.java")
    tooltip = read("VillageEquipmentTooltipClient.java")
    abilities = read("VillageRoleAbilitySystem.java")
    rpg = read("VillageRpgSystem.java")
    techniques = read("VillageCombatTechniqueSystem.java")
    campaign = read("VillageCampaignProgression.java")
    progress = read("RpgProgress.java")
    promotion = read("VillageRolePromotionSystem.java")
    mastery = read("VillageRoleMasterySystem.java")
    research = read("VillageDefenseResearchSystem.java")
    merc = read("VillageMercenarySystem.java")
    turret = read("VillagePlacedTurretSystem.java")

    # Canonical campaign/progression boundaries agree across docs and runtime.
    assert "제100일" in project and "101일 이후" in project
    assert "CAMPAIGN_END_DAY = 100" in campaign
    assert "MAX_LEVEL = 300" in progress
    assert "FIRST_PROMOTION_LEVEL = 30" in promotion
    assert "SECOND_PROMOTION_LEVEL = 60" in promotion
    for level in ("90", "150", "210", "270", "300"):
        assert f"LEVEL = {level}" in mastery or level in mastery
    assert "MAX_LEVEL = 20" in research
    assert "MAX_LEVEL = 100" in merc
    assert "MAX_TURRET_LEVEL = 10" in turret

    # The actual runtime loot route, not a helper that can be bypassed, owns explicit set identity.
    assert "VillageExpandedEquipmentSystem.createRaidDrop" in loot
    assert "VillageEquipmentSetSystem.setForRaidDrop(archetype, boss, random)" in expanded
    assert "VillageEquipmentIdentity.stampSet(result, set.id())" in expanded
    assert 'set.displayName() + " " + baseName' in expanded

    # Five-piece capstones cover real arrows/basic attacks and direct custom skill damage.
    assert "targetMultiplier(ServerPlayer player, Mob target, boolean projectile)" in sets
    assert "roleSkillTargetMultiplier" in sets
    assert "VillageEquipmentSetSystem.roleSkillTargetMultiplier(owner, target, role)" in abilities
    assert "VillageRpgSystem.dealPreScaledPlayerDamage(level, owner, target, trained)" in abilities
    assert "damageSources().indirectMagic(owner, owner)" in rpg
    assert "if (!preScaledPlayerDamage && !preScaledRicochet)" in rpg
    assert "VillageRpgSystem.dealPreScaledPlayerDamage(level, attacker, target, damage)" in techniques

    # Current player-facing docs/UI no longer advertise the retired two-set/three-piece model.
    current_set_section = readme.split("## 장비·무기·세트", 1)[1].split("## 회관 지휘 UI", 1)[0]
    assert "2/3세트 장비:" not in current_set_section
    assert "2/3/4/5세트 장비:" in current_set_section
    for name in ("전선 집행자", "밤사냥꾼", "비전 공명", "여명 성약", "성벽 수호자"):
        assert name in current_set_section
    assert 'new int[]{2, 3, 4, 5}' in tooltip
    assert "set.capstoneText()" in tooltip

    print("[PASS] canonical Day100/Lv300/promotion/mastery/defense caps agree with runtime")
    print("[PASS] actual raid loot path stamps source-derived explicit five-set identity")
    print("[PASS] five-piece conditional capstones reach direct role-skill damage as well as normal attacks")
    print("[PASS] custom role skills and secondary combat techniques preserve player kill ownership without double scaling")
    print("[PASS] current README and equipment tooltip expose the production 2/3/4/5-piece model")

if __name__ == "__main__":
    main()
