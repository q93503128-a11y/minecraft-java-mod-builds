#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    raid = read("VillageRaidSystem.java")
    enemy = read("VillageEnemyArchetypeSystem.java")
    health = read("VillageHealthDisplaySystem.java")
    guardians = read("VillageGuardians.java")
    attack = read("VillageAttackPlanSystem.java")
    local = read("VillageLocalActionSystem.java")
    town = read("VillageTownHallGridScreen.java")
    hud = read("VillageUiHudSuppressor.java")
    terrain = read("VillageFortressTerrain.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    turrets = read("VillagePlacedTurretSystem.java")

    assert "mod_version=" in props

    # Battlefield readability: red team + all occluded raid enemies, bosses always highlighted.
    assert 'RAID_TEAM_NAME = "vg_raid"' in raid
    assert 'TeamColor.byName("red")' in raid
    outline = section(raid, "private static void updateEnemyOutline", "private static PlayerTeam ensureRaidTeam")
    for token in (
        "player.level() == mob.level()", "player.isAlive()", "!player.isSpectator()",
        "!VillageRespawnSystem.isDowned(player)", "160.0 * 160.0", "player.hasLineOfSight(mob)",
        "mob.setGlowingTag(isBossEnemy(mob) || !visibleToAnyPlayer)",
    ):
        assert token in outline, token
    assert "tactical" not in outline
    release = section(raid, "private static void releaseEnemy", "private static void discardEnemies")
    assert "entity.setGlowingTag(false)" in release

    # Nameplates remain intentionally selective even though cover outlines are broad again.
    assert "public static boolean alwaysShowNameplate" in enemy
    assert "mob.setCustomNameVisible(alwaysShowNameplate(archetype, boss, isFlying(mob)))" in enemy
    assert "shouldShowEnemyNameplate(server, mob)" in health
    assert "mob.setCustomNameVisible(true);" not in health

    # Movement ownership stays split: raid owns north/inside and aerial, attack plan owns side/rear exterior.
    assert "VillageAttackPlanSystem.ownsExteriorRouting(id, mob.blockPosition())" in raid
    assert "frontOf(uuid) != Front.NORTH && !isInsideFortress(pos)" in attack
    server_tick = section(guardians, "public void onServerTick", "public void onServerStopping")
    for owner in ("VillageRaidSystem", "VillageAttackPlanSystem", "VillageEnemyEliteSystem",
                  "VillageSiegeBossSystem", "VillagePlacedTurretSystem", "VillageMercenarySystem",
                  "VillageMercenaryDeploymentSystem"):
        assert f"{owner}.tick(event.getServer())" in server_tick, owner

    # No regression in facility ownership or town hall simplification.
    assert 'action.startsWith("facility:") || action.startsWith("manage:")' in local
    assert "시설 기능은 각 시설 단말기에서 사용하세요" in local
    buttons = section(town, "private List<ButtonSpec> facilityButtons", "private void drawButton")
    assert '"repair:" + f.id()' in buttons and '"upgrade:" + f.id()' in buttons
    assert "siege_command" not in buttons and "open_mercenary_command" not in buttons
    assert "VillageProgressionSystem.Building.BARRACKS" in section(deploy, "public static boolean canOpenAt", "public static void openCommand")

    # HUD/modal and latest wall-access/turret maintenance contracts survive the readability change.
    for modal in ("VillageTownHallGridScreen", "VillageShopCatalogScreen", "VillageWaveIntelDossierScreen",
                  "VillageRoleProgressScreen", "VillageGameOverScreen"):
        assert f"screen instanceof {modal}" in hud
    access = section(terrain, "private static void buildWallAccessRamp", "private static void buildTower")
    assert "int landingStart = stairStart + WALL_TOP_Y" in access
    assert "int landingEnd = WALL_RADIUS + 2" in access
    placement = section(turrets, "public static boolean handlePlacementClick", "public static String cancelPlacement")
    assert 'VillageMaintenanceRules.blockReason("포탑 배치")' in placement

    print("[PASS] every occluded active raid enemy is red-outlined; bosses stay outlined when visible")
    print("[PASS] spectator/downed players cannot suppress cover outlines for active defenders")
    print("[PASS] selective nameplates remain separate from broad cover readability")
    print("[PASS] raid/side-rear/aerial navigation ownership remains non-conflicting")
    print("[PASS] town hall, barracks and stale facility-action ownership contracts remain intact")
    print("[PASS] modal HUD suppression, wall-access climb and turret confirmation contracts remain intact")
    print("[PASS] v0.18.36 playtest readability regression audit complete")


if __name__ == "__main__":
    main()
