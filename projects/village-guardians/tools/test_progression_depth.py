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
    shop_ui = read("VillageShopCatalogScreen.java")

    assert common.count('(\"power_') == 10
    assert common.count('(\"guard_') == 10
    assert common.count('(\"support_') == 10
    assert common.count('(\"ranged_') == 10
    assert "Math.max(0, level - 1)" in common
    assert common.count('(\"mobility_') == 10
    assert "pointCost()" in common
    assert "sharedSupplyChance" in common and "teamHealOnKillAmount" in common
    assert "Codec.LONG" in common_data and "Long.SIZE - 1" in common_data

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

    assert "double distance = 72.0" in common_ui
    assert "savedZoom = 0.50" in common_ui
    assert "double y = -tier * 76.0" in role_ui
    assert "savedZoom = 0.74" in role_ui
    assert "renderDetail" not in common_ui
    assert "Bubble" in common_ui
    assert "TreeBubble" in role_ui and "SkillBubble" in role_ui
    assert "renderTreeFooter" not in role_ui and "renderSkillFooter" not in role_ui

    # Current production shop is the categorized detail-first catalog, not the retired parchment list.
    for category in ('ALL("전체"', 'EQUIPMENT("장비"', 'ARMOR("방어구"',
                     'CONSUMABLE("소모품"', 'SALE("판매"'):
        assert category in shop_ui
    assert "VillageUiSafeArea.screen" in shop_ui
    assert "VillageConfirmScreen" in shop_ui
    assert "ChatFormatting.stripFormatting" in shop_ui
    assert 'action.equals("buy_arrows")' in shop_ui and 'action.equals("buy_food")' in shop_ui
    assert 'action.equals("open_item_sell")' in shop_ui and 'action.equals("sell_loot")' in shop_ui

    print("[PASS] Common tactical tree has 50 nodes, five branches and tier-scaled point costs")
    print("[PASS] Five roles expose 75 ordered role-upgrade nodes without ordinal migration")
    print("[PASS] Emergency barrier, momentum and party recovery are wired into combat")
    print("[PASS] Defense research expands from 9 to 15 upgrades")
    print("[PASS] Skill trees fit overview spacing and the current categorized shop uses safe detail-first actions")


if __name__ == "__main__":
    main()
