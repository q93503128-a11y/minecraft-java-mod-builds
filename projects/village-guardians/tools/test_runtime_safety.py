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
    facility_ui = read("VillageFacilityScreen.java")
    town_ui = read("VillageTownHallScreen.java")
    status_ui = read("VillageStatusScreen.java")
    shop_ui = read("VillageShopScreen.java")
    inventory = read("VillageInventoryPanel.java")
    keys = read("VillageClientKeys.java")
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

    assert guardians.count("VillageRaidSystem.onLivingDeath(event)") == 1
    assert "VillageDefenseResearchSystem.initializeServer" in guardians
    assert "VillageRelicSystem.initializeServer" in guardians
    assert "VillageMercenarySystem.initializeServer" in guardians
    assert "VillageMercenarySystem.tick" in guardians
    assert "VillageTowerResearchBonusSystem.tick" in guardians
    assert "VillageGlobalMobPurgeSystem.purge" in guardians
    assert "if (!mob.isPersistenceRequired()) event.setCanceled(true)" in guardians

    assert 'case "building", "management", "funding", "tower_control", "tower_detail", "caller", "relic_choice"' in client_ui
    assert 'case "equipment_shop" -> new VillageShopScreen(payload)' in client_ui
    assert 'case "status", "wave_intel" -> new VillageStatusScreen(payload)' in client_ui
    assert "listLeft" in facility_ui and "detailLeft" in facility_ui
    assert "selectedIndex = actionCount() > 0 ? 0 : -1" in facility_ui
    assert "PANEL = 0xFFF0E5CC" in facility_ui

    assert 'ROLES("직업 배치"' in town_ui
    assert 'REPAIR("시설 수리"' in town_ui
    assert 'MANAGEMENT("관리·건설"' in town_ui
    assert '"repair:" + facility.id()' in town_ui
    assert '"upgrade:" + facility.id()' in town_ui
    assert '"open_tower_control"' in town_ui

    assert "mouseScrolled" not in status_ui
    assert "drawScrollbar" not in status_ui
    assert "twoColumns" in status_ui
    assert "lastLayout" in status_ui
    assert "ClientPacketDistributor" not in status_ui

    assert 'WEAPON("무기")' in shop_ui
    assert 'ARMOR("방어구")' in shop_ui
    assert 'OTHER("기타")' in shop_ui
    assert "renderOfferList" in shop_ui and "renderOfferDetail" in shop_ui
    assert "level < offer.requiredLevel()" not in shop
    assert "requiredDay" in shop
    assert "VillageRelicSystem" in shop

    assert '"open_status"' in inventory
    assert '"open_personal_progress"' in inventory
    assert '"open_role_progress_current"' in inventory
    assert '"open_caller_menu"' in inventory
    assert "C 통신 · R/G 기술" in inventory
    for key in ("GLFW_KEY_I", "GLFW_KEY_P", "GLFW_KEY_O", "GLFW_KEY_V", "GLFW_KEY_C"):
        assert key in keys

    assert "claim_bread" not in controller
    assert "VillageUiService.openBuilding" not in building_router
    assert "VillageUiController.openBuilding" in building_router
    local_body = controller.split("public static void openBuilding", 1)[1].split("public static void openMercenaryCommand", 1)[0]
    assert '"manage:"' not in local_body
    assert "openEquipmentShop(player)" in local_body
    assert "수리·강화·포탑 건설은 회관" in controller
    assert 'case "use_infirmary"' in local_actions
    assert 'case "train"' in local_actions
    assert "VillageLocalActionSystem.handle" in network

    assert "combineFirstPair" in rarity
    assert "재화는 소모되지 않았습니다" in rarity
    assert len([line for line in rarity.splitlines() if line.strip().startswith(("COMMON(", "UNCOMMON(", "RARE(", "EPIC(", "LEGENDARY("))]) == 5
    assert "event.getDrops().clear()" in raid_loot
    assert "createRaidDrop" in raid_loot
    assert "ROTTEN_FLESH" not in raid_loot
    assert "MAIN_INVENTORY_SLOTS = 36" in trading
    assert "VillageDefenseResearchSystem.lootValueMultiplier" in trading

    for branch in ("MERCENARY", "TOWER", "LOGISTICS"):
        assert branch in research
    for role in ("BASTION", "STRIKER", "RANGER", "MEDIC"):
        assert role in mercenary
    assert "VillageMercenaryData.TYPE" in mercenary
    assert "getTags()" not in mercenary
    assert "setPersistenceRequired" in mercenary
    assert "awardKillExperience" in mercenary
    assert "Lv.5" in mercenary

    assert "offerToParty" in relic
    assert "relic_select:" in relic
    assert "pending" in read("VillageRelicData.java")
    assert "baseRoster" in wave and "specialRoster" in wave and "boss" in wave

    assert "DoubleBlockHalf.LOWER" in facade
    assert "기능 단말기" in facade
    assert "mirrorColumn" in facade
    assert "clearPlane(level, anchor, sideways, 2, -1, 2)" in signatures
    assert "buildBackdrop(level, anchor, sideways, 1, 0, 2)" in signatures

    assert "RESPAWN_DELAY_TICKS = 20 * 20" in respawn
    assert "VillageProgressionSystem.Building.WALLS" in gate
    assert "FORCED_NEXT_WAVE_TICKS = 20 * 60" in raid
    assert "MAX_ACTIVE_ENEMIES = 100" in raid
    assert "VillageFortressBuildings.isTouchingStructure" in raid

    print("[PASS] Bright split menus separate navigation, information and confirmation")
    print("[PASS] Town hall exclusively owns repair, upgrade and tower construction")
    print("[PASS] Shop opens into weapon, armour and other categories without level gates")
    print("[PASS] Raid drops use curated sale loot and graded equipment fusion")
    print("[PASS] Inventory and keyboard expose status, personal, role and caller menus")
    print("[PASS] Mercenary classes, defense research, boss relics and wave intel are wired")
    print("[PASS] Natural mobs are suppressed and legacy facades migrate safely")


if __name__ == "__main__":
    main()
