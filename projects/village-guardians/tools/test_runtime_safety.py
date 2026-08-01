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
    structure_hud = read("VillageStructureHud.java")
    progression = read("VillageProgressionSystem.java")
    town_hall = read("VillageTownHallInteraction.java")
    client_ui = read("VillageClientUi.java")
    ui_screen = read("VillageUiScreen.java")
    skill_screen = read("VillageSkillTreeScreen.java")
    builder = read("VillageSimpleBuildingBuilder.java")
    building_catalog = read("VillageBuildingCatalog.java")
    fortress = read("VillageFortressTerrain.java")
    fortress_buildings = read("VillageFortressBuildings.java")
    doors = read("VillageDoorSystem.java")
    defense = read("VillageDefenseSystem.java")
    techniques = read("VillageCombatTechniqueSystem.java")
    rpg_progress = read("RpgProgress.java")

    assert guardians.count("VillageRaidSystem.onLivingDeath(event)") == 1
    assert "public static boolean isActiveEnemy(UUID uuid)" in raid
    assert "public static boolean isActive()" in raid
    assert "VillageWorldSystem.unmarkAllowedGameMob(uuid)" in raid
    assert "VillageHealthDisplaySystem.forgetEnemy(uuid)" in raid
    assert "EntityTypes.RAVAGER" in raid
    assert "EntityTypes.WITCH" in raid
    assert "EntityTypes.PILLAGER" in raid
    assert "useEnemyAbility" in raid
    assert "VillageFortressBuildings.attackPoint" in raid
    assert "VillageFortressBuildings.isTouchingStructure" in raid

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
    assert "VillageRaidSystem.isActive()" in structure_hud
    assert "nextBuilding" in structure_hud

    assert "RETURN_COOLDOWN_TICKS" in world
    assert "COMBAT_RETURN_LOCK_TICKS" in world
    assert "findSafeReturnPosition" in world
    assert world.count("removeUnauthorizedMobs(level, center);") == 1
    assert "VillageProgressionSystem.restoreFacilitiesForMigration()" in world
    assert "CRYING_OBSIDIAN" in world
    assert "resetForRestart(server, false)" not in world

    assert "VillageFortressBuildings.buildingAtTerminal" in progression
    assert "Building.fromTerminal(block)" not in progression
    assert "VillageFortressBuildings.terminalPosition" in town_hall
    assert "distSqr(hallCenter)" not in town_hall

    assert "new VillageSkillTreeScreen(payload)" in client_ui
    assert 'if ("습득 가능".equals(status))' in skill_screen
    assert "maxScroll" in skill_screen

    assert "Button.builder" in ui_screen
    assert "actionPageCount" in ui_screen
    assert "rebuildActionButtons" in ui_screen
    assert "clearWidgets()" in ui_screen
    assert "renderActions" not in ui_screen
    assert "button.png" not in ui_screen  # Vanilla buttons consume the licensed override automatically.

    assert "buildSolidCeiling" in builder
    assert "for (int fillY = roofBase; fillY < y; fillY++)" in builder
    assert "Blocks.SEA_LANTERN" in builder
    assert "setValue(DoorBlock.OPEN, false)" in builder
    assert "TOWN_HALL -> new Spec(\n                    -21, 36, 43, 27" in building_catalog
    assert "GATE_HALF_WIDTH = 9" in fortress

    assert "VillageDoorSystem.handle(event)" in guardians
    assert "setDoorOpen(level, partner, open)" in doors
    assert "VillageDefenseSystem.tick(event.getServer())" in guardians
    assert "VillageDefenseSystem.recognizeDefenseMob(mob)" in guardians
    assert "hireMercenary" in defense
    assert "nearestActiveEnemy" in defense
    assert "wallLevel >= 3" in defense

    assert "handleSwordTechnique" in techniques
    assert "handleArrowTechnique" in techniques
    assert "setRemainingFireTicks" in techniques
    assert "activeEnemiesNear" in techniques
    assert "120 + level * 72 + level * level * 7" in rpg_progress

    assert "attackPoint" in fortress_buildings
    assert "isTouchingStructure" in fortress_buildings
    assert "distanceSquaredToStructure" in fortress_buildings

    print("[PASS] Raid death processing has one event entry point and cleans entity state")
    print("[PASS] Raids scale into diverse elite and boss waves with boundary attacks")
    print("[PASS] Skill nodes use SavedData without add/remove tag probing")
    print("[PASS] Loot selling cannot clear armor or offhand slots")
    print("[PASS] Caller and facility interactions require exact identities and positions")
    print("[PASS] Roofs, lighting, paired doors, compact footprints, and sealed gate are guarded")
    print("[PASS] Licensed vanilla buttons paginate all village commands inside the panel")
    print("[PASS] Persistent facility HUD, towers, mercenaries, and late-game techniques are wired")
    print("[PASS] Player experience requirements use the slower quadratic curve")


if __name__ == "__main__":
    main()
