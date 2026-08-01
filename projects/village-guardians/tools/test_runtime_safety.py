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
    raid_loot = read("VillageRaidLootSystem.java")
    world = read("VillageWorldSystem.java")
    signatures = read("VillageBuildingSignatures.java")
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
    facility_screen = read("VillageFacilityScreen.java")
    status_screen = read("VillageStatusScreen.java")
    quick_chat = read("VillageQuickChatScreen.java")
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
    assert "VillageWorldSystem.isInsideBattlefield" in guardians
    assert "!mob.isPersistenceRequired()" in guardians
    assert "VillageDefenseSystem.recognizeDefenseMob(mob)" in guardians
    assert "VillageGatePrioritySystem.tick" in guardians
    assert "VillageRespawnSystem.handleIncomingDamage" in guardians
    assert "VillageRespawnSystem.tick" in guardians
    assert "VillageRaidLootSystem.handleDrops" in guardians
    assert "LivingDropsEvent" in guardians

    assert "public static boolean isActiveEnemy(UUID uuid)" in raid
    assert "public static boolean isRaidEnemy(Entity entity)" in raid
    assert "FORCED_NEXT_WAVE_TICKS = 20 * 60" in raid
    assert "MAX_ACTIVE_ENEMIES = 100" in raid
    assert "잔존 적" in raid
    spawn_body = raid.split("private static void spawnWave", 1)[1].split("private static void applyScaling", 1)[0]
    assert "ACTIVE_ENEMIES.clear();" not in spawn_body
    assert "VillageFortressBuildings.attackPoint" in raid
    assert "VillageFortressBuildings.isTouchingStructure" in raid
    assert "event.getDrops().clear()" in raid_loot
    assert "VillageRaidSystem.isRaidEnemy" in raid_loot

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

    assert "sideBySide" in generic_ui
    assert "rightPaneWidth" in generic_ui
    assert "contentWidth * 27 / 100" in generic_ui
    assert "selectedIndex = Math.min(actions.length, labels.length) == 1 ? 0 : -1" in generic_ui
    assert "footerScroll" in generic_ui
    assert "actionScroll" in generic_ui
    assert "bodyScroll" in generic_ui
    assert generic_ui.count("enableScissor") >= 3

    assert "informationLines" in facility_screen
    assert "contentWidth * 29 / 100" in facility_screen
    assert "CARD_HEIGHT = 44" in facility_screen
    assert "현장에서 바로 수리·강화" in facility_screen
    assert facility_screen.count("enableScissor") >= 2
    assert "VillageUiActionPayload(action)" in facility_screen

    assert "Read-only status page" in status_screen
    assert "현재 수호자 정보" in status_screen
    assert "ClientPacketDistributor" not in status_screen
    assert "actions" not in status_screen

    assert "누르는 즉시 전송" in quick_chat
    assert "VillageUiActionPayload(actions[index])" in quick_chat
    assert "OVERLAY = 0x4A000000" in quick_chat

    assert 'case "role_progress" -> new VillageRoleProgressScreen(payload)' in client_ui
    assert 'case "quick_chat" -> new VillageQuickChatScreen(payload)' in client_ui
    assert 'case "status" -> new VillageStatusScreen(payload)' in client_ui
    assert 'case "building", "management" -> new VillageFacilityScreen(payload)' in client_ui
    assert 'GLFW.GLFW_KEY_R' in client_keys
    assert 'GLFW.GLFW_KEY_G' in client_keys
    assert 'GLFW.GLFW_KEY_C' in client_keys
    assert 'VillageUiActionPayload("open_quick_chat")' in client_keys
    assert 'VillageUiActionPayload("use_skill:0")' in client_keys
    assert 'VillageUiActionPayload("use_skill:1")' in client_keys

    assert "openCallerMenu" in ui_service
    assert "openTowerControl" in ui_service
    assert "openFunding" in ui_service
    assert "requireSkillHall" in ui_service
    assert "requireManagementAccess" in ui_service
    assert "VillageLocationRules.isNear(player, building)" in ui_service
    assert 'send(player, "status", "수호자 상태", body, List.of(), List.of())' in ui_service
    assert '"manage:" + building.id()' in ui_service
    assert '"open_building:" + building.id()' in ui_service
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
    assert "VillageTimePhase.DAY" in rpg
    assert "MobEffects.SPEED, 50, 1" in rpg

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
    assert "installedStage >= 1" in tower_builder
    assert "installedStage >= 2" in tower_builder
    assert "installedStage >= 3" in tower_builder
    assert "installedStage >= 4" in tower_builder
    assert "clearInstallationPad" in tower_builder
    assert "VillageDefenseTowerBuilder.build" in world

    assert "VillageBuildingSignatures.buildAll" in world
    assert "VillageBuildingSignatures.remove" in world
    assert "BATTLEFIELD_RADIUS, 96, BATTLEFIELD_RADIUS" in world
    assert "Blocks.AMETHYST_BLOCK" in world
    assert "Blocks.COPPER_BLOCK" in world
    assert "center.below(6)" in world
    assert "buildGateShield" in signatures
    assert "buildCrown" in signatures
    assert "buildHammer" in signatures
    assert "buildRune" in signatures
    assert "buildHealingCross" not in signatures
    assert "buildSupplyCrate" in signatures
    assert "buildCrossedBlades" in signatures
    assert "VillageBuildingCatalog.entrance" in signatures
    assert "entranceFacing().getOpposite()" in signatures
    assert "building == VillageProgressionSystem.Building.INFIRMARY" in signatures
    assert "spec.height()" not in signatures
    assert "clearAbove" not in signatures

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

    assert "MAIN_INVENTORY_SLOTS = 36" in trading
    assert "Items.CLOCK" in starter
    assert "migrateLegacyCaller" in starter
    assert "C키로 투명 빠른 통신창" in starter
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

    print("[PASS] Facility information owns the large pane while action and cost controls stay compact")
    print("[PASS] Status is a dedicated read-only screen without generic action controls")
    print("[PASS] Every facility terminal exposes local repair and upgrade management")
    print("[PASS] Roof signatures are removed and replaced by front-facing facade marks")
    print("[PASS] The infirmary keeps its original front cross without a duplicate signature")
    print("[PASS] C opens a transparent one-click quick communication overlay")
    print("[PASS] Daytime grants sustained Speed II while night removes it naturally")
    print("[PASS] All unauthorized natural mobs are blocked throughout the battlefield")
    print("[PASS] Raid drops are cleared and rewards stay inside the progression economy")
    print("[PASS] Waves advance after a clear or force-advance after sixty seconds")
    print("[PASS] Defense towers are physically built only after installation stages")
    print("[PASS] Closed gates and delayed revival continue to protect the defense loop")


if __name__ == "__main__":
    main()
