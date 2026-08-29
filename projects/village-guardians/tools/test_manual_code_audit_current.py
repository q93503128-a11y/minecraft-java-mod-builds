#!/usr/bin/env python3
"""Regression contracts for the current manual code audit and cleanup."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]


def main() -> None:
    guardians = read("VillageGuardians.java")
    raid = read("VillageRaidSystem.java")
    attack = read("VillageAttackPlanSystem.java")
    elite = read("VillageEnemyEliteSystem.java")
    boss = read("VillageSiegeBossSystem.java")
    health = read("VillageHealthDisplaySystem.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    progression = read("VillageProgressionSystem.java")
    starter = read("VillageStarterKit.java")
    rules = read("VillageMaintenanceRules.java")
    town = read("VillageTownHallGridScreen.java")
    hud = read("VillageUiHudSuppressor.java")
    service = read("VillageUiService.java")

    # Duplicate/obsolete production owners are physically gone, not merely bypassed.
    for retired in ("VillageGatePrioritySystem.java", "VillageTownHallSystem.java", "VillageFundingSystem.java"):
        assert not (JAVA / retired).exists(), retired
    for token in ("VillageGatePrioritySystem", "handleBuildingInteraction"):
        assert token not in guardians
    assert "VillageFundingSystem" not in service and "funding:" not in service
    assert "public static void openDashboard" not in service

    # Navigation has one owner per state: north/inside in raid, side/rear exterior in attack plan.
    assert "frontOf(uuid) != Front.NORTH && !isInsideFortress(pos)" in attack
    assert "VillageAttackPlanSystem.ownsExteriorRouting(id, mob.blockPosition())" in raid
    assert "return VillageProgressionSystem.Building.WALLS;" in raid
    assert elite.count("VillageAttackPlanSystem.ownsExteriorRouting(mob.getUUID(), mob.blockPosition())") >= 2
    assert "VillageAttackPlanSystem.ownsExteriorRouting(boss.getUUID(), boss.blockPosition())" in boss

    # Authoritative UUID rosters replace repeated battlefield-sized scans in hot loops.
    assert "public static List<Mob> activeEnemies(ServerLevel level)" in raid
    assert "VillageRaidSystem.activeEnemies(level)" in elite
    assert "VillageRaidSystem.activeEnemies(level)" in boss
    assert "VillageRaidSystem.activeEnemies(level)" in health
    for source in (elite, boss, health, deploy, merc):
        assert "BATTLEFIELD_RADIUS" not in source
    assert "public static synchronized List<IronGolem> loadedMercenaries" in merc
    assert "List<IronGolem> loaded = VillageMercenarySystem.loadedMercenaries" in deploy

    # Client actions are revalidated in the authoritative mutation methods.
    assert "VillageProgressionSystem.isGameOver()" in rules
    assert "VillageCouncilState.currentPhase() != VillageTimePhase.DAY" in rules
    for name, location, operational in (
        ("VillageDefenseResearchSystem.java", "isNearSkillHall", "Building.SKILL_HALL"),
        ("VillageSkillTreeSystem.java", "isNearSkillHall", "Building.SKILL_HALL"),
        ("VillageRoleSkillSystem.java", "isNearSkillHall", "Building.SKILL_HALL"),
        ("VillageEquipmentRaritySystem.java", "Building.SMITHY", "Building.SMITHY"),
        ("VillageTradingSystem.java", "Building.STOREHOUSE", "Building.STOREHOUSE"),
        ("VillageEquipmentShop.java", "Building.STOREHOUSE", "Building.STOREHOUSE"),
        ("VillageConsumableSystem.java", "Building.STOREHOUSE", "Building.STOREHOUSE"),
    ):
        source = read(name)
        assert location in source and operational in source, name
        assert "VillageMaintenanceRules.blockReason" in source, name
    assert progression.count("VillageMaintenanceRules.blockReason") >= 3
    assert "VillageMaintenanceRules.blockReason(\"용병 고용\")" in merc
    assert "if (!canOpenAt(player))" in deploy

    # A full restart covers known offline players on their next login and clears all player-owned storage.
    reset = section(progression, "public static synchronized void resetForRestart", "public static int upgradeCost")
    assert "PENDING_NEW_GAME_RESETS.addAll(COINS.keySet())" in reset
    assert "PENDING_RESET_PREFIX + uuid" in progression
    assert "consumePendingNewGameReset" in progression
    assert "VillageProgressionSystem.consumePendingNewGameReset(player)" in starter
    assert "player.getInventory().clearContent()" in starter
    assert "player.getEnderChestInventory().clearContent()" in starter

    # Current town-hall UI has no parsed-but-unrendered role branch, and every large modal suppresses HUD layers.
    for dead in ("RoleCard", "drawRoleDetail", "drawTabs", "functionAction", "open_funding"):
        assert dead not in town
    for modal in ("VillageTownHallGridScreen", "VillageShopCatalogScreen", "VillageVictoryScreen"):
        assert f"screen instanceof {modal}" in hud

    print("[PASS] duplicate town-hall, funding and gate-priority owners are physically retired")
    print("[PASS] raid navigation and side/rear routing have explicit non-conflicting owners")
    print("[PASS] raid and mercenary hot loops resolve authoritative UUID rosters without battlefield scans")
    print("[PASS] server mutation methods revalidate location, facility and preparation state")
    print("[PASS] full restart resets online and known-offline player-owned inventories")
    print("[PASS] dead town-hall role code is gone and all large modals suppress vanilla HUD layers")


if __name__ == "__main__":
    main()
