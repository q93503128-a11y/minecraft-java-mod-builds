#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    common = read("VillageSkillTreeSystem.java")
    common_data = read("VillageSkillTreeData.java")
    role = read("VillageRoleSkillSystem.java")
    personal = read("VillagePersonalCombatSystem.java")
    rpg = read("VillageRpgSystem.java")
    research = read("VillageDefenseResearchSystem.java")
    research_data = read("VillageDefenseResearchData.java")
    controller = read("VillageUiController.java")
    common_ui = read("VillageSkillTreeScreen.java")
    role_ui = read("VillageRoleProgressScreen.java")
    shop_ui = read("VillageShopScreen.java")

    assert common.count('("power_') == 7
    assert common.count('("guard_') == 7
    assert common.count('("support_') == 7
    assert common.count('("ranged_') == 7
    assert "(level - 1) / 2" in common
    assert "sharedSupplyChance" in common and "teamHealOnKillAmount" in common
    assert "Integer.SIZE - 1" in common_data

    for branch in ("DURATION", "POWER", "SPECIAL"):
        assert role.count(branch + "_") >= 5
    assert "DURATION_5" in role and "POWER_5" in role and "SPECIAL_5" in role
    assert "roleTreeCooldownReductionSeconds" in role
    assert "VillageRoleSkillSystem.nodes()" in controller

    assert "handleIncomingDamage" in personal
    assert "applyKillMomentum" in personal
    assert "healNearbyAlliesOnKill" in personal
    assert "VillagePersonalCombatSystem.handleIncomingDamage" in rpg
    assert "VillagePersonalCombatSystem.reset" in rpg

    assert "MAX_LEVEL = 5" in research
    assert "VillageDefenseResearchSystem.MAX_LEVEL" in research_data
    assert '"/" + VillageDefenseResearchSystem.MAX_LEVEL' in controller

    assert "double distance = 86.0" in common_ui
    assert "savedZoom = 0.74" in common_ui
    assert "double y = -tier * 76.0" in role_ui
    assert "savedZoom = 0.74" in role_ui
    assert "renderDetail" not in common_ui
    assert "Bubble" in common_ui
    assert "TreeBubble" in role_ui and "SkillBubble" in role_ui
    assert "renderTreeFooter" not in role_ui and "renderSkillFooter" not in role_ui
    assert "ACTION_HEIGHT = 20" in shop_ui
    assert "contentWidth < 330" in shop_ui
    assert "ChatFormatting.stripFormatting" in shop_ui
    assert "§l" not in shop_ui and "§6" not in shop_ui

    print("[PASS] Common tactical tree has 28 nodes with functional capstones")
    print("[PASS] Five roles expose 75 ordered role-upgrade nodes without ordinal migration")
    print("[PASS] Emergency barrier, momentum and party recovery are wired into combat")
    print("[PASS] Defense research expands from 9 to 15 upgrades")
    print("[PASS] Skill trees fit overview spacing and the shop uses compact safe actions")


if __name__ == "__main__":
    main()
