#!/usr/bin/env python3
"""Source-level safety contracts for high-risk Village Guardians systems."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    guardians = read("VillageGuardians.java")
    raid = read("VillageRaidSystem.java")
    world = read("VillageWorldSystem.java")
    skill = read("VillageSkillTreeSystem.java")
    skill_data = read("VillageSkillTreeData.java")
    trading = read("VillageTradingSystem.java")
    starter = read("VillageStarterKit.java")
    health = read("VillageHealthDisplaySystem.java")
    progression = read("VillageProgressionSystem.java")
    town_hall = read("VillageTownHallInteraction.java")
    client_ui = read("VillageClientUi.java")
    skill_screen = read("VillageSkillTreeScreen.java")

    assert guardians.count("VillageRaidSystem.onLivingDeath(event)") == 1
    assert "public static boolean isActiveEnemy(UUID uuid)" in raid
    assert "VillageWorldSystem.unmarkAllowedGameMob(uuid)" in raid
    assert "VillageHealthDisplaySystem.forgetEnemy(uuid)" in raid

    assert "UNLOCKED_MASKS" in skill
    assert "VillageSkillTreeData.TYPE" in skill
    assert "player.addTag" not in skill
    assert "player.removeTag" not in skill
    assert "hasValidAllocation" in skill
    assert 'return "데이터 잠금"' in skill
    assert "unlocked_masks" in skill_data
    assert "sanitizeMask" in skill_data
    assert "VillageSkillTreeSystem.initializeServer" in guardians

    assert "MAIN_INVENTORY_SLOTS = 36" in trading
    assert "Math.min(MAIN_INVENTORY_SLOTS" in trading
    assert "getContainerSize(); slot++" not in trading

    assert "DataComponents.CUSTOM_NAME" in starter
    assert "CALLER_NAME.equals(customName.getString())" in starter
    assert "stack.getItem() == Items.GOAT_HORN" in starter

    assert "PLAYER_TEAM_PREFIX + player.getUUID().toString().substring(0, 8)" in health
    assert 'String teamName = "vghp_" + Math.min' not in health
    assert "VillageRaidSystem.isActiveEnemy" in health
    assert "ENEMY_BASE_NAMES" in health

    assert "RETURN_COOLDOWN_TICKS" in world
    assert "COMBAT_RETURN_LOCK_TICKS" in world
    assert "findSafeReturnPosition" in world
    assert world.count("removeUnauthorizedMobs(level, center);") == 1
    assert "VillageProgressionSystem.restoreFacilitiesForMigration()" in world
    assert "resetForRestart(server, false)" not in world

    assert "VillageFortressBuildings.buildingAtTerminal" in progression
    assert "Building.fromTerminal(block)" not in progression
    assert "VillageFortressBuildings.terminalPosition" in town_hall
    assert "distSqr(hallCenter)" not in town_hall

    assert "new VillageSkillTreeScreen(payload)" in client_ui
    assert 'if ("습득 가능".equals(status))' in skill_screen
    assert "maxScroll" in skill_screen

    print("[PASS] Raid death processing has one event entry point and cleans entity state")
    print("[PASS] Skill nodes use SavedData without add/remove tag probing")
    print("[PASS] Loot selling cannot clear armor or offhand slots")
    print("[PASS] Caller and facility interactions require exact identities and positions")
    print("[PASS] Migration, return travel, health labels, and compact skill UI are guarded")


if __name__ == "__main__":
    main()
