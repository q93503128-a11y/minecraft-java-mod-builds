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
    ui_service = read("VillageUiService.java")
    ui_screen = read("VillageUiScreen.java")
    skill_screen = read("VillageSkillTreeScreen.java")
    confirm_screen = read("VillageConfirmScreen.java")
    action_descriptions = read("VillageActionDescriptions.java")
    client_keys = read("VillageClientKeys.java")
    builder = read("VillageSimpleBuildingBuilder.java")
    building_catalog = read("VillageBuildingCatalog.java")
    fortress = read("VillageFortressTerrain.java")
    fortress_buildings = read("VillageFortressBuildings.java")
    doors = read("VillageDoorSystem.java")
    defense = read("VillageDefenseSystem.java")
    techniques = read("VillageCombatTechniqueSystem.java")
    roles = read("VillageRole.java")
    rpg = read("VillageRpgSystem.java")
    rpg_progress = read("RpgProgress.java")
    council = read("VillageCouncilState.java")

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

    assert "ChatFormatting.stripFormatting" in starter
    assert "Component.literal(CALLER_NAME).withStyle" in starter
    assert "hasCaller(player)" in starter
    assert "event.getHand() != InteractionHand.MAIN_HAND" not in starter

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
    assert "MobCategory.MONSTER" in world
    assert "purgeDaytimeHostiles" in world
    assert "isInsideBattlefield" in world
    assert "VillageWorldSystem.purgeDaytimeHostiles" in guardians
    assert "mob.getType().getCategory() == MobCategory.MONSTER" in guardians

    assert "VillageFortressBuildings.buildingAtTerminal" in progression
    assert "Building.fromTerminal(block)" not in progression
    assert "VillageFortressBuildings.terminalPosition" in town_hall
    assert "distSqr(hallCenter)" not in town_hall

    assert "new VillageSkillTreeScreen(payload)" in client_ui
    assert "Button.builder" in skill_screen
    assert "VillageConfirmScreen" in skill_screen
    assert '"습득 가능".equals(status)' in skill_screen
    assert "ClientPacketDistributor" not in skill_screen
    assert "selectNode" in skill_screen
    assert "nodeX(int tier)" in skill_screen
    assert "connectionColor" in skill_screen

    assert "Button.builder" in ui_screen
    assert "button -> selectAction" in ui_screen
    assert "executeSelected" in ui_screen
    assert "VillageActionDescriptions.describe" in ui_screen
    assert "VillageConfirmScreen" in ui_screen
    assert "requiresConfirmation" in action_descriptions
    assert "확인하고 실행" in confirm_screen
    assert "button.png" not in ui_screen  # Vanilla buttons consume the licensed override automatically.

    assert '"manage:town_hall"' in ui_service
    assert '"manage:walls"' in ui_service
    assert "openFacilityManagement" in ui_service
    assert '"role_info:ranger"' in ui_service
    assert '"role_info:engineer"' in ui_service
    assert '"role_info:builder"' not in ui_service
    assert '"role_info:quartermaster"' not in ui_service
    assert '"use_skill", "open_skill_tree"' not in ui_service
    assert "역할 스킬: 기본 R키" in ui_service
    assert 'case "use_skill" -> player.sendSystemMessage' in ui_service
    assert "fillLocalBuildingActions" in ui_service
    assert "회관에서는 시설의 수리·강화만 관리합니다" in ui_service

    assert "RANGER(" in roles and "ENGINEER(" in roles
    assert "BUILDER(" not in roles and "QUARTERMASTER(" not in roles and "STEWARD(" not in roles
    assert 'case "builder", "steward", "engineer"' in roles
    assert 'case "quartermaster", "medic"' in roles
    assert 'case "scout", "ranger"' in roles
    assert "isOnWallTop" in rpg
    assert "RANGED_FOCUS_UNTIL" in rpg
    assert "hasActiveEngineer" in defense

    assert "RegisterKeyMappingsEvent" in client_keys
    assert "ClientTickEvent.Post" in client_keys
    assert "consumeClick()" in client_keys
    assert 'VillageUiActionPayload("use_skill")' in client_keys
    assert "GLFW.GLFW_KEY_R" in client_keys

    assert "buildSolidCeiling" in builder
    assert "for (int fillY = roofBase; fillY < y; fillY++)" in builder
    assert "Blocks.SEA_LANTERN" in builder
    assert "setValue(DoorBlock.OPEN, false)" in builder
    assert "TOWN_HALL -> new Spec(\n                    -21, 36, 43, 27" in building_catalog
    assert "GATE_HALF_WIDTH = 9" in fortress

    assert "VillageDoorSystem.handle(event)" in guardians
    assert "normalizeHinges" in doors
    assert "setDoorOpen(level, partner, open)" in doors
    assert "DoorHingeSide.LEFT" in doors and "DoorHingeSide.RIGHT" in doors
    assert "VillageDefenseSystem.tick(event.getServer())" in guardians
    assert "VillageDefenseSystem.recognizeDefenseMob(mob)" in guardians
    assert "hireMercenary" in defense
    assert "nearestActiveEnemy" in defense
    assert "hasActiveEngineer" in defense

    assert "handleSwordTechnique" in techniques
    assert "handleArrowTechnique" in techniques
    assert "setRemainingFireTicks" in techniques
    assert "activeEnemiesNear" in techniques
    assert "120 + level * 72 + level * level * 7" in rpg_progress
    assert "new RpgProgress(level, 0).experienceToNextLevel()" in council
    assert "player.heal(player.getMaxHealth())" not in council.split("grantExperience", 1)[1].split("rpgStatus", 1)[0]

    assert "attackPoint" in fortress_buildings
    assert "isTouchingStructure" in fortress_buildings
    assert "distanceSquaredToStructure" in fortress_buildings

    print("[PASS] Raid death processing has one event entry point and cleans entity state")
    print("[PASS] Raids scale into diverse elite and boss waves with boundary attacks")
    print("[PASS] Skill nodes use SavedData and review-confirm graph UI")
    print("[PASS] Town hall management is separated from local facility functions")
    print("[PASS] Four distinct roles, wall archer, engineer defences, and R-key skill are wired")
    print("[PASS] Caller identity, double doors, daytime hostile cleanup, inventory safety are guarded")
    print("[PASS] Roofs, lighting, compact footprints, sealed gate, towers and mercenaries remain wired")
    print("[PASS] Player experience requirements use the slower quadratic curve")


if __name__ == "__main__":
    main()
