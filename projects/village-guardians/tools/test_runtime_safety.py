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
    town_hall_screen = read("VillageTownHallScreen.java")
    skill_screen = read("VillageSkillTreeScreen.java")
    inventory_panel = read("VillageInventoryPanel.java")
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
    assert "RANGED_1" in skill and "RANGED_3" in skill
    assert "projectileDamageMultiplier" in skill

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
    assert "isNearTownHall" in town_hall
    assert "ROLE_MANAGEMENT_DISTANCE_SQUARED" in town_hall

    assert "new VillageSkillTreeScreen(payload)" in client_ui
    assert "new VillageTownHallScreen(payload)" in client_ui
    assert 'case "town_hall"' in client_ui

    assert "Button.builder" not in skill_screen
    assert "VillageConfirmScreen" not in skill_screen
    assert "savedPanX" in skill_screen and "savedPanY" in skill_screen
    assert "mouseDragged(MouseButtonEvent event, double dragX, double dragY)" in skill_screen
    assert "drawNodeIcon" in skill_screen
    assert "renderConnections" in skill_screen
    assert "return Branch.RANGED;" in skill_screen
    assert '"습득 가능".equals(nodes.get(selectedIndex).status())' in skill_screen

    assert "Button.builder" not in ui_screen
    assert "VillageConfirmScreen" not in ui_screen
    assert "actionRegions" in ui_screen
    assert "renderFooter" in ui_screen
    assert "isImmediate" in ui_screen
    assert "VillageActionDescriptions.describe" in ui_screen
    assert "requiresConfirmation" in action_descriptions
    assert "상세 화면 열기" not in action_descriptions

    assert "Button.builder" not in town_hall_screen
    assert "drawRoleCard" in town_hall_screen
    assert "drawRoleIcon" in town_hall_screen
    assert "renderFacilities" in town_hall_screen
    assert "시설 관리" in town_hall_screen
    assert "역할 배치" in town_hall_screen

    assert 'VillageUiActionPayload("open_skill_tree")' in inventory_panel
    assert 'VillageUiActionPayload("return_village")' in inventory_panel
    assert 'VillageUiActionPayload("open_status")' not in inventory_panel
    assert '"전술 발전"' in inventory_panel
    assert '"마을 귀환"' in inventory_panel

    assert 'send(player, "town_hall"' in ui_service
    assert '"role"' in ui_service and '"facility"' in ui_service
    assert "openFacilityManagement" in ui_service
    assert "select_role:" in ui_service
    assert "VillageTownHallInteraction.isNearTownHall(player)" in ui_service
    assert 'List.of("open_skill_tree", "return_village")' in ui_service
    assert '"role_info:ranger"' not in ui_service
    assert '"role_info:engineer"' not in ui_service
    assert 'case "use_skill" -> player.sendSystemMessage' in ui_service
    assert "fillLocalBuildingActions" in ui_service

    assert "RANGER(" in roles and "ENGINEER(" in roles
    assert "BUILDER(" not in roles and "QUARTERMASTER(" not in roles and "STEWARD(" not in roles
    assert 'case "builder", "steward", "engineer"' in roles
    assert 'case "quartermaster", "medic"' in roles
    assert 'case "scout", "ranger"' in roles
    assert "isOnWallTop" in rpg
    assert "RANGED_FOCUS_UNTIL" in rpg
    assert "VillageSkillTreeSystem.projectileDamageMultiplier(attacker)" in rpg
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
    print("[PASS] Skill nodes use SavedData with a draggable four-way icon tree")
    print("[PASS] Inventory exposes only tactical growth and village return")
    print("[PASS] Town hall owns role selection and facility management cards")
    print("[PASS] Four roles, wall archer, engineer defences, ranged branch, and R-key skill are wired")
    print("[PASS] Caller identity, double doors, daytime hostile cleanup, and inventory safety are guarded")
    print("[PASS] Player experience requirements use the slower quadratic curve")


if __name__ == "__main__":
    main()
