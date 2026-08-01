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
    roles = read("VillageRole.java")
    role_system = read("VillageRoleSkillSystem.java")
    role_data = read("VillageRoleProgressData.java")
    role_screen = read("VillageRoleProgressScreen.java")
    skill = read("VillageSkillTreeSystem.java")
    skill_data = read("VillageSkillTreeData.java")
    skill_screen = read("VillageSkillTreeScreen.java")
    inventory = read("VillageInventoryPanel.java")
    town_hall = read("VillageTownHallScreen.java")
    generic_ui = read("VillageUiScreen.java")
    ui_service = read("VillageUiService.java")
    client_ui = read("VillageClientUi.java")
    client_keys = read("VillageClientKeys.java")
    progression = read("VillageProgressionSystem.java")
    equipment = read("VillageEquipmentShop.java")
    defense = read("VillageDefenseSystem.java")
    tower_builder = read("VillageDefenseTowerBuilder.java")
    gate_priority = read("VillageGatePrioritySystem.java")
    respawn = read("VillageRespawnSystem.java")
    funding = read("VillageFundingSystem.java")
    location = read("VillageLocationRules.java")
    hall_access = read("VillageTownHallAccessFix.java")
    fortress_buildings = read("VillageFortressBuildings.java")
    trading = read("VillageTradingSystem.java")
    starter = read("VillageStarterKit.java")
    health = read("VillageHealthDisplaySystem.java")
    structure_hud = read("VillageStructureHud.java")
    status_hud = read("VillageHudSystem.java")
    rpg = read("VillageRpgSystem.java")
    techniques = read("VillageCombatTechniqueSystem.java")
    rpg_progress = read("RpgProgress.java")
    council = read("VillageCouncilState.java")

    assert guardians.count("VillageRaidSystem.onLivingDeath(event)") == 1
    assert "VillageRoleSkillSystem.initializeServer" in guardians
    assert "purgeUnauthorizedVillageMobs" in guardians
    assert "!mob.isPersistenceRequired()" in guardians
    assert "VillageDefenseSystem.recognizeDefenseMob(mob)" in guardians
    assert "VillageGatePrioritySystem.tick" in guardians
    assert "VillageRespawnSystem.handleIncomingDamage" in guardians
    assert "VillageRespawnSystem.tick" in guardians
    assert "!VillageRespawnSystem.isDowned(player)" in guardians

    assert "public static boolean isActiveEnemy(UUID uuid)" in raid
    assert "VillageFortressBuildings.attackPoint" in raid
    assert "VillageFortressBuildings.isTouchingStructure" in raid

    for role in ("VANGUARD", "RANGER", "ARCANIST", "LUMINAR", "WARDEN"):
        assert role + "(" in roles
    assert "GUARD_CAPTAIN(" not in roles
    assert "ENGINEER(" not in roles
    assert "MEDIC(" not in roles
    assert 'case "guard_captain", "warrior", "vanguard"' in roles
    assert 'case "builder", "steward", "engineer", "tank", "warden"' in roles

    assert "tree_masks" in role_data
    assert "skill_masks" in role_data
    assert "equipped_skills" in role_data
    assert "RoleBranch" in role_system
    assert "DURATION_3" in role_system and "POWER_3" in role_system and "SPECIAL_3" in role_system
    assert role_system.count("VillageRole.VANGUARD") >= 4
    assert role_system.count("VillageRole.RANGER") >= 4
    assert role_system.count("VillageRole.ARCANIST") >= 4
    assert role_system.count("VillageRole.LUMINAR") >= 4
    assert role_system.count("VillageRole.WARDEN") >= 4
    assert "equipSkill(ServerPlayer player, String skillId, int slot)" in role_system
    assert "useEquippedSkill(ServerPlayer player, int slot)" in role_system
    assert "R: " in role_system and "G: " in role_system
    assert "skillScroll" in role_screen
    assert "contentViewport" in role_screen
    assert "FOOTER_HEIGHT" in role_screen
    assert "mouseScrolled" in role_screen
    assert "savedZoom" in role_screen
    assert "세 갈래" in role_screen
    assert "R에 장착" in role_screen and "G에 장착" in role_screen

    assert "UNLOCKED_MASKS" in skill
    assert "VillageSkillTreeData.TYPE" in skill
    assert "player.addTag" not in skill
    assert "hasValidAllocation" in skill
    assert len([line for line in skill.splitlines() if line.strip().startswith(("POWER_", "GUARD_", "SUPPORT_", "RANGED_"))]) >= 20
    assert "executionMultiplier" in skill
    assert "projectileFireBonusTicks" in skill
    assert "extraRicochetTargets" in skill
    assert "unlocked_masks" in skill_data
    assert "savedZoom" in skill_screen
    assert "setZoom" in skill_screen
    assert "mouseScrolled" in skill_screen
    assert "0.55" in skill_screen and "1.75" in skill_screen

    assert "(screenWidth - 176) / 2" in inventory
    assert "MIN_WIDTH = 92" in inventory
    assert 'action = "open_status"' in inventory
    assert 'action = "return_village"' in inventory
    assert '"open_skill_tree"' not in inventory

    assert "List<RoleCard>" in town_hall
    assert "List<FacilityCard>" in town_hall
    assert "roleListScroll" in town_hall
    assert "roleDetailScroll" in town_hall
    assert "facilityListScroll" in town_hall
    assert "facilityDetailScroll" in town_hall
    assert "성장·기술은 연구소에서 관리" in town_hall
    assert "포탑 지휘·성벽 관리" in town_hall
    assert town_hall.count("enableScissor") >= 4
    assert "footerScroll" in generic_ui
    assert "actionScroll" in generic_ui
    assert "bodyScroll" in generic_ui
    assert generic_ui.count("enableScissor") >= 3

    assert 'case "role_progress" -> new VillageRoleProgressScreen(payload)' in client_ui
    assert 'GLFW.GLFW_KEY_R' in client_keys
    assert 'GLFW.GLFW_KEY_G' in client_keys
    assert 'VillageUiActionPayload("use_skill:0")' in client_keys
    assert 'VillageUiActionPayload("use_skill:1")' in client_keys

    assert "openCallerMenu" in ui_service
    assert "openTowerControl" in ui_service
    assert "openFunding" in ui_service
    assert "requireSkillHall" in ui_service
    assert "VillageLocationRules.isNearSkillHall" in ui_service
    assert "직업 성장과 기술 장착은 기술·마법 연구소" in ui_service
    assert 'case WALLS -> "open_tower_control"' in ui_service
    assert '"open_role_progress_current"' in ui_service
    assert '"role_node:"' in ui_service
    assert '"role_skill_unlock:"' in ui_service
    assert '"role_skill_equip:"' in ui_service
    assert '"open_equipment_shop"' in ui_service
    assert '"gear:"' in ui_service
    assert '"funding:"' in ui_service

    assert "public static synchronized boolean spendCoins" in progression
    assert "DataComponents.CUSTOM_NAME" in equipment
    assert "requiredLevel" in equipment and "requiredDay" in equipment
    assert "VillageEquipmentShop.outgoingMultiplier" in rpg
    assert "VillageEquipmentShop.incomingMultiplier" in rpg
    assert "VillageSkillTreeSystem.executionMultiplier" in rpg
    assert "VillageSkillTreeSystem.killHealAmount" in rpg

    assert "setPersistenceRequired()" in defense
    assert "사망하지 않는 한 저장과 재접속 후에도 유지" in defense
    assert "mercenaryHireCost" in defense
    assert "VillageProgressionSystem.spendCoins" in defense
    assert "HIRE_IRON_COST" not in defense
    assert "fireBallista" in defense
    assert "fireFlame" in defense
    assert "fireFrost" in defense
    assert "fireArcane" in defense
    assert "VillageRole.WARDEN" in defense
    assert "buildBallista" in tower_builder
    assert "buildFlame" in tower_builder
    assert "buildFrost" in tower_builder
    assert "buildArcane" in tower_builder
    assert "VillageDefenseTowerBuilder.build" in world

    assert "VillageWorldSystem.isNorthGatePassable" in gate_priority
    assert "mob.setTarget(null)" in gate_priority
    assert "VillageProgressionSystem.Building.WALLS" in gate_priority
    assert "VillageRaidSystem.isActiveEnemy" in gate_priority
    assert "RESPAWN_DELAY_TICKS = 20 * 20" in respawn
    assert "GameType.SPECTATOR" in respawn
    assert "GameType.ADVENTURE" in respawn
    assert "player.setHealth(player.getMaxHealth())" in respawn
    assert "적은 시설 공격을 계속" in respawn
    assert "VillageProgressionSystem.addSupplies" in funding
    assert "VillageProgressionSystem.spendCoins" in funding
    assert "isNearSkillHall" in location and "isNearTownHall" in location

    assert "VillageTownHallAccessFix.apply" in fortress_buildings
    assert "z1 - 9" in hall_access
    assert "Blocks.DARK_OAK_FENCE" in hall_access
    assert "Blocks.RESPAWN_ANCHOR" in world
    assert "purgeUnauthorizedVillageMobs" in world

    assert "MAIN_INVENTORY_SLOTS = 36" in trading
    assert "Items.CLOCK" in starter
    assert "migrateLegacyCaller" in starter
    assert "openCallerMenu" in starter
    assert "ChatFormatting.stripFormatting" in starter
    assert "PLAYER_TEAM_PREFIX + player.getUUID().toString().substring(0, 8)" in health
    assert "VillageRaidSystem.isActive()" in structure_hud
    assert "REFRESH_TICKS = 10" in status_hud
    assert "VillageRespawnSystem.hudText" in status_hud
    assert "handleSwordTechnique" in techniques
    assert "handleArrowTechnique" in techniques
    assert "120 + level * 72 + level * level * 7" in rpg_progress
    assert "new RpgProgress(level, 0).experienceToNextLevel()" in council

    print("[PASS] Five combat roles use persistent three-branch progression in the research hall")
    print("[PASS] Twenty role skills support level/currency unlocks and two equipped key slots")
    print("[PASS] Town hall, research and generic screens use independent clipped scroll panes")
    print("[PASS] Inventory and restored clock caller expose status, communication and return only")
    print("[PASS] Town hall owns tower command, facility management and coin-funded supplies")
    print("[PASS] Closed gates override player targeting and force raiders onto the wall")
    print("[PASS] Lethal damage enters a 20-second spectator countdown before village revival")
    print("[PASS] Natural village mobs are blocked while coin-hired persistent mercenaries remain")
    print("[PASS] Four fixed tower archetypes and progressive vanilla equipment are wired")


if __name__ == "__main__":
    main()
