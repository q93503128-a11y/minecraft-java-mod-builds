#!/usr/bin/env python3
"""Source-level contracts for the current Village Guardians design."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    guardians = read("VillageGuardians.java")
    controller = read("VillageUiController.java")
    local_actions = read("VillageLocalActionSystem.java")
    network = read("VillageNetwork.java")
    client_ui = read("VillageClientUi.java")
    hud_system = read("VillageHudSystem.java")
    main_hud = read("VillageMainHudOverlay.java")
    skill_hud = read("VillageSkillHudOverlay.java")
    safe_area = read("VillageUiSafeArea.java")
    hud_suppressor = read("VillageUiHudSuppressor.java")
    quick_safe = read("VillageQuickChatSafeScreen.java")
    fusion_safe = read("VillageFusionSafeScreen.java")
    command_ui = read("VillageCommandCenterScreen.java")
    town_ui = read("VillageTownHallGridScreen.java")
    shop_ui = read("VillageShopCatalogScreen.java")
    action_ui = read("VillageActionDetailScreen.java")
    relic_ui = read("VillageRelicScreen.java")
    relic_choice_ui = read("VillageRelicChoiceConfirmScreen.java")
    wave_ui = read("VillageWaveIntelDossierScreen.java")
    game_over_ui = read("VillageGameOverScreen.java")
    inventory = read("VillageInventoryPanel.java")
    keys = read("VillageClientKeys.java")
    starter = read("VillageStarterKit.java")
    shop = read("VillageEquipmentShop.java")
    rarity = read("VillageEquipmentRaritySystem.java")
    raid_loot = read("VillageRaidLootSystem.java")
    trading = read("VillageTradingSystem.java")
    research = read("VillageDefenseResearchSystem.java")
    mercenary = read("VillageMercenarySystem.java")
    relic = read("VillageRelicSystem.java")
    wave = read("VillageWaveIntelSystem.java")
    facade = read("VillageBuildingFacadeFix.java")
    signatures = read("VillageBuildingSignatures.java")
    building_router = read("VillageBuildingInteractionRouter.java")
    respawn = read("VillageRespawnSystem.java")
    gate = read("VillageGatePrioritySystem.java")
    raid = read("VillageRaidSystem.java")
    descriptions = read("VillageActionDescriptions.java")

    assert guardians.count("VillageRaidSystem.onLivingDeath(event)") == 1
    assert "VillageDefenseResearchSystem.initializeServer" in guardians
    assert "VillageRelicSystem.initializeServer" in guardians
    assert "VillageMercenarySystem.initializeServer" in guardians
    assert "VillageMercenarySystem.tick" in guardians
    assert "VillagePlacedTurretSystem.tick" in guardians
    assert "VillageTowerResearchBonusSystem" not in guardians
    assert "VillageGlobalMobPurgeSystem.purge" in guardians
    assert "if (!mob.isPersistenceRequired()) event.setCanceled(true)" in guardians

    # HUD collision contract: persistent status never uses vanilla action bar and the accepted
    # low-profile skill strip stays safely above the hotbar while modal screens own a safe viewport.
    assert "ClientboundSetActionBarTextPacket" not in hud_system
    assert "VillageNetwork.sendMainHud(player, text)" in hud_system
    assert "MainHudPayload" in network and '"main_hud"' in network
    assert "VillageMainHudOverlay.accept(payload)" in client_ui
    assert "minecraft.gui.screen() != null" in main_hud
    assert "minecraft.gui.screen() != null" in skill_hud
    assert "graphics.guiHeight() - 98" in skill_hud
    assert "VillageQuickChatSafeScreen.drawDiamond" in skill_hud
    assert "abilityCard" not in skill_hud
    assert "bottomReserve" not in safe_area
    assert "bottomPadding" in safe_area and "height - bottomPadding" in safe_area
    assert "RenderGuiLayerEvent.Pre" in hud_suppressor
    for layer in ("HOTBAR", "PLAYER_HEALTH", "FOOD_LEVEL", "OVERLAY_MESSAGE", "TITLE", "CHAT"):
        assert f"VanillaGuiLayers.{layer}" in hud_suppressor
    assert "event.setCanceled(true)" in hud_suppressor
    assert "Minecraft.getInstance().gui.screen()" in hud_suppressor

    # Production routing must use current responsive surfaces, never retired parchment/generic screens.
    assert 'case "quick_chat" -> new VillageQuickChatSafeScreen(payload)' in client_ui
    assert 'case "equipment_fusion" -> new VillageFusionSafeScreen(payload)' in client_ui
    assert 'case "town_hall" -> new VillageTownHallGridScreen(payload)' in client_ui
    assert 'case "equipment_shop" -> new VillageShopCatalogScreen(payload)' in client_ui
    assert 'case "status", "caller" -> new VillageCommandCenterScreen(payload)' in client_ui
    assert '"building", "management", "funding", "tower_control", "tower_detail", "skill_test" ->' in client_ui
    assert 'case "relic_collection" -> new VillageRelicScreen(payload)' in client_ui
    assert 'case "relic_choice" -> new VillageRelicChoiceConfirmScreen(payload)' in client_ui
    assert 'case "wave_intel" -> new VillageWaveIntelDossierScreen(payload)' in client_ui
    assert 'case "game_over" -> new VillageGameOverScreen(payload)' in client_ui
    assert "default -> new VillageActionDetailScreen(payload)" in client_ui
    for legacy in ("VillageTownHallScreen", "VillageShopScreen", "VillageQuickChatScreen",
                   "VillageFacilityScreen", "VillageUiScreen", "VillageRelicChoiceScreen",
                   "VillageWaveIntelScreen", "VillageStatusScreen"):
        assert legacy not in client_ui

    for source in (quick_safe, fusion_safe, command_ui, town_ui, shop_ui, action_ui,
                   relic_ui, relic_choice_ui, wave_ui):
        assert "VillageUiSafeArea.screen" in source
    assert "drawSignalLabel" in quick_safe
    assert "safe.bottom() - 29" in quick_safe
    assert "ClientPacketDistributor.sendToServer" in quick_safe
    assert '"fusion_combine:"' in fusion_safe
    assert "enableScissor" in fusion_safe and "enableScissor" in command_ui
    town_render = town_ui.split("public void extractRenderState", 1)[1].split("private void drawFrame", 1)[0]
    town_buttons = town_ui.split("private List<ButtonSpec> facilityButtons", 1)[1].split("private String functionAction", 1)[0]
    assert "drawTabs(" not in town_render
    assert '"repair:" + f.id()' in town_buttons and '"upgrade:" + f.id()' in town_buttons
    assert "functionAction" not in town_buttons and "open_funding" not in town_buttons
    assert 'ALL("전체"' in shop_ui and 'SALE("판매"' in shop_ui
    assert "VillageConfirmScreen" in shop_ui
    assert '"facility_info".equals(rawActions[i])' in action_ui
    assert "rowHeight" in relic_ui and "cellWidth" in relic_ui
    assert "reliquary" in relic_ui.lower() and "drawDiamond" in relic_ui
    assert "VillageConfirmScreen" in relic_choice_ui and "영구 적용" in relic_choice_ui
    assert "VillageEnemyBestiary.find" in wave_ui and "drawDossier" in wave_ui
    assert "VillageConfirmScreen" in game_over_ui and "requiresConfirmation" in game_over_ui

    assert '"open_status"' in inventory
    assert '"open_skill_tree"' in inventory
    assert '"open_role_progress_current"' in inventory
    assert '"open_quick_chat"' in inventory
    assert "VillageClientKeys.compactSummary()" in inventory
    for key in ("GLFW_KEY_Z", "GLFW_KEY_V", "GLFW_KEY_B", "GLFW_KEY_H", "GLFW_KEY_J", "GLFW_KEY_K"):
        assert key in keys
    assert "GLFW_KEY_U" not in keys and "CALLER" not in keys

    assert 'case "claim_bread"' in controller
    assert "VillageUiService.openBuilding" not in building_router
    assert "VillageUiController.openBuilding" in building_router
    local_body = controller.split("public static void openBuilding", 1)[1].split("public static void openMercenaryCommand", 1)[0]
    assert '"manage:"' not in local_body
    assert "openEquipmentShop(player)" in local_body
    assert "수리·강화·포탑 건설은 회관" in controller
    dashboard = controller.split("public static void openDashboard", 1)[1].split("public static void openRoleAssignment", 1)[0]
    assert "select_role:" not in dashboard and '"role"' not in dashboard
    assert "openRoleAssignment" in controller and "직업 배치는 기술 연구소" in controller
    assert "nextEffect" in controller
    assert "upgradeCost" in controller
    assert "repairCost" in controller
    assert 'case "forge_upgrade", "smithy_forge_upgrade"' in controller
    assert '"smithy_forge_upgrade"' in controller
    assert "장비 선택 강화" in controller
    assert "openPersonalProgress(ServerPlayer player)" in controller
    assert "openSkillTree(player);" in controller
    assert "장비 강화는 대장간 단말기 근처에서만 가능합니다." in controller
    assert "기술 장착은 기술 연구소에서만 가능합니다." not in read("VillageUiService.java")
    assert "직업 기술 습득은 기술 연구소에서만 가능합니다." in read("VillageUiService.java")
    assert "TreeBubble" in read("VillageRoleProgressScreen.java")
    assert "SkillBubble" in read("VillageRoleProgressScreen.java")
    assert "renderDetail" not in read("VillageSkillTreeScreen.java")
    assert "forge_combine" in controller
    assert 'case "use_infirmary"' in local_actions
    assert 'case "train"' in local_actions
    assert "VillageLocalActionSystem.handle" in network

    assert "level < offer.requiredLevel()" not in shop
    assert "requiredDay" in shop
    assert "VillageRelicSystem" not in shop
    assert "removeCallerItems" in starter
    assert "namedCaller" not in starter
    assert "giveOrDrop(player, Items.CLOCK" not in starter
    assert "giveOrDrop(player, Items.GOAT_HORN" not in starter
    assert "호출기 아이템은 폐지" in starter
    assert "인벤토리 화면의 빠른 통신 버튼" in starter

    assert "combineSelected" in rarity and "fusionCandidates" in rarity
    assert "등급 하나로 합성했습니다" in rarity
    assert len([line for line in rarity.splitlines() if line.strip().startswith(("COMMON(", "UNCOMMON(", "RARE(", "EPIC(", "LEGENDARY("))]) == 5
    assert "event.getDrops().clear()" in raid_loot
    assert "createRaidDrop" in raid_loot
    assert "ROTTEN_FLESH" not in raid_loot
    assert "MAIN_INVENTORY_SLOTS = 36" in trading
    assert "VillageDefenseResearchSystem.lootValueMultiplier" in trading

    for branch in ("MERCENARY", "TOWER", "LOGISTICS"):
        assert branch in research
    for role_name in ("BASTION", "STRIKER", "RANGER", "MEDIC"):
        assert role_name in mercenary
    assert "VillageMercenaryData.TYPE" in mercenary
    assert "getTags()" not in mercenary
    assert "setPersistenceRequired" in mercenary
    assert "awardKillExperience" in mercenary
    assert "MAX_LEVEL = 60" in mercenary

    assert "offerToParty" in relic
    assert "relic_select:" in relic
    assert "openCollection" in relic
    assert "pending" in read("VillageRelicData.java")
    assert "previews(ServerPlayer player)" in wave and "WavePreview" in wave and "previewArchetype" in wave

    assert "DoubleBlockHalf.LOWER" in facade
    assert "기능 단말기" in facade
    assert "mirrorColumn" in facade
    assert "fillPlane(level, anchor, sideways, 2, -1, 2, spec.panel())" in signatures
    assert "buildBackdrop(level, anchor, sideways, 1, 0, 2)" in signatures
    non_wall_build = signatures.split("static void build(ServerLevel", 1)[1].split("static void remove", 1)[0]
    assert "clearPlane(level, anchor, sideways, 2, -1, 2)" not in non_wall_build

    assert 'case "forge_upgrade", "smithy_forge_upgrade"' in descriptions
    assert 'action.equals("smithy_forge_upgrade")' in descriptions
    assert 'action.equals("forge_combine")' in descriptions

    assert "RESPAWN_DELAY_TICKS = 20 * 20" in respawn
    assert "VillageProgressionSystem.Building.WALLS" in gate
    assert "FORCED_NEXT_WAVE_TICKS = 20 * 60" in raid
    assert "MAX_ACTIVE_ENEMIES = 100" in raid
    assert "VillageFortressBuildings.isTouchingStructure" in raid

    print("[PASS] Modal screens suppress vanilla HUD/chat/title and stay inside the hotbar-safe viewport")
    print("[PASS] Production routing uses current town/shop/action/relic/wave surfaces only")
    print("[PASS] Town hall exposes repair/upgrade only while facility functions remain location-authoritative")
    print("[PASS] Quick chat, fusion, relic and tactical dossier safeguards remain wired")
    print("[PASS] Existing progression, building, shop, loot, mercenary, research and raid contracts remain wired")


if __name__ == "__main__":
    main()
