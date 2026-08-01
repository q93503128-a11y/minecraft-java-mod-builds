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

    # Raid lifecycle, target priority and controlled rewards.
    assert guardians.count("VillageRaidSystem.onLivingDeath(event)") == 1
    assert "VillageGatePrioritySystem.tick" in guardians
    assert "VillageRespawnSystem.tick" in guardians
    assert "VillageRaidLootSystem.handleDrops" in guardians
    assert "purgeUnauthorizedVillageMobs" in guardians
    assert "public static boolean isActiveEnemy(UUID uuid)" in raid
    assert "FORCED_NEXT_WAVE_TICKS = 20 * 60" in raid
    assert "MAX_ACTIVE_ENEMIES = 100" in raid
    assert "VillageFortressBuildings.attackPoint" in raid
    assert "VillageFortressBuildings.isTouchingStructure" in raid
    assert "event.getDrops().clear()" in raid_loot
    assert "VillageRaidSystem.isRaidEnemy" in raid_loot

    # Five balanced player roles, persistent growth and two equipped skills.
    for role in ("VANGUARD", "RANGER", "ARCANIST", "LUMINAR", "WARDEN"):
        assert role + "(" in roles
    assert "GUARD_CAPTAIN(" not in roles
    assert "ENGINEER(" not in roles
    assert "MEDIC(" not in roles
    assert "tree_masks" in role_data
    assert "skill_masks" in role_data
    assert "equipped_skills" in role_data
    assert "DURATION_3" in role_system
    assert "POWER_3" in role_system
    assert "SPECIAL_3" in role_system
    assert "equipSkill(ServerPlayer player, String skillId, int slot)" in role_system
    assert "useEquippedSkill(ServerPlayer player, int slot)" in role_system
    assert "R: " in role_system and "G: " in role_system
    assert "contentViewport" in role_screen
    assert "savedZoom" in role_screen
    assert "R에 장착" in role_screen and "G에 장착" in role_screen

    # Skill tree remains pannable and zoomable.
    assert "UNLOCKED_MASKS" in skill
    assert "VillageSkillTreeData.TYPE" in skill
    assert "unlocked_masks" in skill_data
    assert "savedZoom" in skill_screen
    assert "setZoom" in skill_screen
    assert "mouseScrolled" in skill_screen
    assert "0.55" in skill_screen and "1.75" in skill_screen

    # Inventory remains small and only exposes status/return shortcuts.
    assert "(screenWidth - 176) / 2" in inventory
    assert "MIN_WIDTH = 92" in inventory
    assert 'action = "open_status"' in inventory
    assert 'action = "return_village"' in inventory
    assert '"open_skill_tree"' not in inventory

    # Town hall still owns role placement, facility management and tower access.
    assert "List<RoleCard>" in town_hall
    assert "List<FacilityCard>" in town_hall
    assert "facilityListScroll" in town_hall
    assert "facilityDetailScroll" in town_hall
    assert "포탑 지휘·성벽 관리" in town_hall
    assert town_hall.count("enableScissor") >= 4

    # Critical responsive UI contract: actions are never sacrificed to fixed panes.
    assert "selectedIndex = actionCount() > 0 ? 0 : -1" in generic_ui
    assert "actionHeight < CARD_HEIGHT + 12" in generic_ui
    assert "CARD_HEIGHT = 44" in generic_ui
    assert "renderReadOnlyBody" in generic_ui
    assert "contentHeight - bodyHeight - footerHeight" in generic_ui
    assert "actionScroll" in generic_ui
    assert "footerScroll" in generic_ui
    assert generic_ui.count("enableScissor") >= 3
    assert 'default -> new VillageUiScreen(payload)' in client_ui
    assert 'case "status" -> new VillageStatusScreen(payload)' in client_ui
    assert 'case "building", "management" -> new VillageFacilityScreen(payload)' not in client_ui

    # Status is read-only; communication is one-click and transparent.
    assert "Read-only status page" in status_screen
    assert "ClientPacketDistributor" not in status_screen
    assert "actions" not in status_screen
    assert "누르는 즉시 전송" in quick_chat
    assert "VillageUiActionPayload(actions[index])" in quick_chat
    assert "OVERLAY = 0x4A000000" in quick_chat

    # Every important action is still sent by the server payload.
    assert "openCallerMenu" in ui_service
    assert "openTowerControl" in ui_service
    assert "openFunding" in ui_service
    assert "requireManagementAccess" in ui_service
    assert 'send(player, "status", "수호자 상태", body, List.of(), List.of())' in ui_service
    assert '"manage:" + building.id()' in ui_service
    assert '"open_building:" + building.id()' in ui_service
    assert '"open_role_progress_current"' in ui_service
    assert '"open_equipment_shop"' in ui_service
    assert '"gear:"' in ui_service
    assert '"funding:"' in ui_service
    assert '"tower_open:"' in ui_service

    # Economy, equipment, daytime mobility and permanent defenders.
    assert "public static synchronized boolean spendCoins" in progression
    assert "DataComponents.CUSTOM_NAME" in equipment
    assert "requiredLevel" in equipment and "requiredDay" in equipment
    assert "VillageEquipmentShop.outgoingMultiplier" in rpg
    assert "VillageEquipmentShop.incomingMultiplier" in rpg
    assert "VillageTimePhase.DAY" in rpg
    assert "MobEffects.SPEED, 50, 1" in rpg
    assert "setPersistenceRequired()" in defense
    assert "mercenaryHireCost" in defense
    assert "VillageProgressionSystem.spendCoins" in defense
    assert "fireBallista" in defense
    assert "fireFlame" in defense
    assert "fireFrost" in defense
    assert "fireArcane" in defense

    # Towers exist only at unlocked installation stages.
    for stage in range(1, 5):
        assert f"installedStage >= {stage}" in tower_builder
    assert "clearInstallationPad" in tower_builder
    assert "VillageDefenseTowerBuilder.build" in world

    # Existing worlds migrate away from rooftop symbols to front facade marks.
    assert "VillageBuildingSignatures.buildAll" in world
    assert "Blocks.LAPIS_BLOCK" in world
    assert "center.below(6)" in world
    assert "VillageBuildingCatalog.entrance" in signatures
    assert "entranceFacing().getOpposite()" in signatures
    assert "building == VillageProgressionSystem.Building.INFIRMARY" in signatures
    assert "spec.height()" not in signatures
    assert "clearAbove" not in signatures
    assert "buildHealingCross" not in signatures

    # Gate priority, delayed revival and facility access remain enforced.
    assert "VillageWorldSystem.isNorthGatePassable" in gate_priority
    assert "mob.setTarget(null)" in gate_priority
    assert "VillageProgressionSystem.Building.WALLS" in gate_priority
    assert "RESPAWN_DELAY_TICKS = 20 * 20" in respawn
    assert "GameType.SPECTATOR" in respawn
    assert "GameType.ADVENTURE" in respawn
    assert "player.setHealth(player.getMaxHealth())" in respawn
    assert "VillageProgressionSystem.addSupplies" in funding
    assert "VillageProgressionSystem.spendCoins" in funding
    assert "isNearSkillHall" in location and "isNearTownHall" in location
    assert "VillageTownHallAccessFix.apply" in fortress_buildings
    assert "z1 - 9" in hall_access

    # Starter kit, HUD and combat-technique contracts.
    assert "MAIN_INVENTORY_SLOTS = 36" in trading
    assert "Items.CLOCK" in starter
    assert "migrateLegacyCaller" in starter
    assert "C키로 투명 빠른 통신창" in starter
    assert "PLAYER_TEAM_PREFIX + player.getUUID().toString().substring(0, 8)" in health
    assert "VillageRaidSystem.isActive()" in structure_hud
    assert "REFRESH_TICKS = 10" in status_hud
    assert "VillageRespawnSystem.hudText" in status_hud
    assert "handleSwordTechnique" in techniques
    assert "handleArrowTechnique" in techniques
    assert "120 + level * 72 + level * level * 7" in rpg_progress
    assert "new RpgProgress(level, 0).experienceToNextLevel()" in council

    print("[PASS] Action cards retain guaranteed space at large GUI scales")
    print("[PASS] The first action is selected automatically instead of showing an empty prompt")
    print("[PASS] Funding, facility, shop and tower payloads share the validated action renderer")
    print("[PASS] Status remains a dedicated read-only screen")
    print("[PASS] Town hall retains facility, wall and tower entry points")
    print("[PASS] Raid, economy, respawn, role and skill safety contracts remain intact")


if __name__ == "__main__":
    main()
