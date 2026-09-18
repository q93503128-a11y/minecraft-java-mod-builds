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
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    terrain = read("VillageFortressTerrain.java")
    enhancements = read("VillageBuildingEnhancements.java")
    world = read("VillageWorldSystem.java")
    enemy = read("VillageEnemyArchetypeSystem.java")
    raid = read("VillageRaidSystem.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    progression = read("VillageProgressionSystem.java")
    segment = read("VillageSiegeSegmentSystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    research = read("VillageDefenseResearchSystem.java")
    council = read("VillageCouncilState.java")
    network = read("VillageNetwork.java")
    controller = read("VillageUiController.java")
    town = read("VillageTownHallGridScreen.java")
    detail = read("VillageActionDetailScreen.java")
    shop = read("VillageShopCatalogScreen.java")
    ui_service = read("VillageUiService.java")
    respawn = read("VillageRespawnSystem.java")
    relic = read("VillageRelicSystem.java")
    role_ability = read("VillageRoleAbilitySystem.java")
    role_skill = read("VillageRoleSkillSystem.java")
    guardians = read("VillageGuardians.java")

    assert "mod_version=0.18.38-alpha.1" in props
    assert "현재 소스 버전 `0.18.38-alpha.1`" in readme
    assert "villageguardians-0.18.38-alpha.1.jar" in readme

    # Wall traffic contract: stairs, pads and ranger posts must use independent lanes.
    assert "SIDE_REAR_ACCESS_LANE = 52" in terrain
    access = section(terrain, "private static void buildWallAccess", "private static void buildTower")
    assert "new int[]{-25, 25}" in access
    assert "new int[]{-SIDE_REAR_ACCESS_LANE, SIDE_REAR_ACCESS_LANE}" in access
    assert "WALL_EMPLACEMENT_LANE = 34" in enhancements

    north_lanes = (-25, 25)
    side_rear_lanes = (-52, 52)
    emplacement_lanes = (-34, 34)
    ranger_posts = (-56, -52, -48, -44, -40, 40, 44, 48, 52, 56)

    def landing_cells(lane: int) -> set[int]:
        return set(range(lane - 3, lane + 4))

    def pad_cells(lane: int) -> set[int]:
        return set(range(lane - 2, lane + 3))

    for stair_lane in north_lanes:
        for pad_lane in emplacement_lanes:
            assert landing_cells(stair_lane).isdisjoint(pad_cells(pad_lane))
        assert all(post not in landing_cells(stair_lane) for post in ranger_posts)
    for stair_lane in side_rear_lanes:
        for pad_lane in emplacement_lanes:
            assert landing_cells(stair_lane).isdisjoint(pad_cells(pad_lane))

    # One owner per full-wall rail: wall body keeps inner crenellation, gallery keeps one outer safety rail.
    horizontal = section(terrain, "private static void buildHorizontalWall", "private static void buildVerticalWall")
    vertical = section(terrain, "private static void buildVerticalWall", "private static boolean isFiringBayOffset")
    gallery = section(terrain, "private static void buildDefenderGalleries", "private static void buildNorthGate")
    reinforce = section(enhancements, "static void reinforceWallRailings", "/** True only for the authored 3x3")
    assert "outerZ" not in horizontal
    assert "outerX" not in vertical
    assert gallery.count("Blocks.STONE_BRICK_WALL") == 4
    assert "murderHole" not in gallery
    assert "buildWallTopEmplacements(level, center)" in reinforce
    assert "placeRailing" not in reinforce

    # Existing 0.18.37 worlds receive a geometry-only migration; authoritative combat state is reprojected.
    assert "center.below(11)).is(Blocks.IRON_BLOCK)" in world
    assert "center.below(11), Blocks.IRON_BLOCK" in world
    ensure = section(world, "public static synchronized void ensureFortifiedVillage", "public static synchronized void forceRebuild")
    assert "resetForNewGame" not in ensure
    assert "VillageSiegeSegmentSystem.restoreAllVisuals(level)" in ensure
    assert "VillagePlacedTurretSystem.initializeServer(server)" in ensure

    # Early sapper is still an objective threat, but no longer carries the old speed/tank/damage spike.
    attributes = section(enemy, "private static void applyArchetypeAttributes", "private static void applyArchetypeEffects")
    effects = section(enemy, "private static void applyArchetypeEffects", "private static String displayName")
    assert "archetype == Archetype.SAPPER" in attributes
    assert "Math.min(16.0, 8.5" in attributes
    assert "speed.setBaseValue(0.14)" in attributes
    assert "case SAPPER -> { }" in effects
    assert "case SAPPER -> 1.72f" in enemy
    scaling = section(raid, "private static void applyScaling", "private static void directEnemies")
    assert "boolean sapper" in scaling
    assert "healthTier - 2" in scaling and "strengthTier - 2" in scaling
    assert "day >= 5 && !sapper" in scaling

    # Mercenaries deploy before contact and are valid enemy combat targets.
    assert "public static void prepareNightDeployment" in deploy
    assert "center.offset(kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 12 : -12, 0, -84)" in deploy
    assert "force && zone == Deployment.GATE_FRONT" in deploy
    assert "golem.snapTo" in deploy
    assert "public static synchronized boolean isCombatMercenary" in merc
    assert "public static synchronized IronGolem nearestCombatMercenary" in merc
    routing = section(raid, "private static void directEnemies", "private static void directFlyingEnemy")
    assert "VillageMercenarySystem.isCombatMercenary" in routing
    assert "VillageMercenarySystem.nearestCombatMercenary" in routing

    # Player and bastion taunts are authoritative routing overrides, including objective-first archetypes.
    assert "FORCED_TAUNTS" in raid
    assert "public static int tauntEnemies" in raid
    assert routing.index("activeTauntTarget") < routing.index("ownsExteriorRouting")
    assert routing.index("activeTauntTarget") < routing.index("Archetype.TOWER_HUNTER")
    taunt = section(role_ability, "private static void tauntShout", "private static void healLowestAlly")
    assert "30.0 + specialRank * 3.0" in taunt
    assert "VillageRaidSystem.tauntEnemies" in taunt
    assert "시설·포탑을 우선 노리는 공성 병과도 도발" in role_skill
    bastion = section(merc, "private static void bastionControl", "private static void strikerPressure")
    assert "12.0 + Math.min(12.0, rank * 0.20)" in bastion
    assert "VillageRaidSystem.tauntEnemies" in bastion

    # Friendly projectiles pass through mercenaries and friendly damage is zeroed before RPG scaling.
    assert "blockFriendlyFire" in merc
    assert "event.setAmount(0.0f)" in merc
    assert "onProjectileImpact(ProjectileImpactEvent event)" in guardians
    projectile = section(guardians, "public void onProjectileImpact", "public void onIncomingDamage")
    assert "VillageMercenarySystem.isCombatMercenary" in projectile
    assert "event.setCanceled(true)" in projectile
    incoming = section(guardians, "public void onIncomingDamage", "public void onFinalDamage")
    assert "VillageMercenarySystem.blockFriendlyFire(event)" in incoming

    # Final-wave inaccessible stragglers cannot leave the raid permanently locked.
    assert "FINAL_STRAGGLER_RECOVERY_TICKS = 20 * 35" in raid
    assert "recoverFinalStragglers(server)" in raid
    recovery = section(raid, "private static void recoverFinalStragglers", "private static ServerPlayer nearestAnyCombatPlayer")
    assert "ACTIVE_ENEMIES.size() > 2" in recovery
    assert "VillageWorldSystem.northInnerApproach()" in recovery
    assert "잔존 적 유도" in recovery
    assert "포탑 안내" in raid

    # Party-wide investments consume shared supplies; personal loadout/progression may still use coins elsewhere.
    hire = section(merc, "public static synchronized String hire", "public static synchronized void captureNightSnapshot")
    assert "spendSupplies(cost)" in hire
    assert "spendCoins" not in hire
    turret_place = section(turret, "public static boolean handlePlacementClick", "public static String cancelPlacement")
    assert "TURRETS.size() >= capacity()" in turret_place
    assert "TURRETS.size() < capacity()" in turret_place
    assert "포탑 설치 경쟁 환불" in turret_place
    assert "spendSupplies(cost)" in turret_place
    assert "spendCoins" not in turret_place
    turret_maintenance = section(turret, "public static synchronized String repair(ServerPlayer player, int id)", "public static void tick")
    assert "spendSupplies(cost)" in turret_maintenance
    assert "spendCoins" not in turret_maintenance
    segment_repair = section(segment, "public static String repair", "public static String upgrade")
    segment_upgrade = section(segment, "public static String upgrade", "public static BlockPos attackPoint")
    assert "spendSupplies(cost)" in segment_repair and "spendCoins" not in segment_repair
    assert "spendSupplies(cost)" in segment_upgrade and "spendCoins" not in segment_upgrade
    research_upgrade = section(research, "public static synchronized String upgrade", "private static float curve")
    assert "spendSupplies(cost)" in research_upgrade and "spendCoins" not in research_upgrade

    # Personal coins can be deliberately contributed to the party supply pool at the storehouse.
    assert "exchangeCoinsForSupplies" in progression
    assert "coinCost = 25" in progression and "supplyGain = 50" in progression
    assert 'actions.add("exchange_supplies")' in controller
    assert '"exchange_supplies".equals' in shop or 'action.equals("exchange_supplies")' in shop
    assert 'case "exchange_supplies"' in controller

    # Daytime hunger is frozen without making the infirmary the owner of that rule.
    infirmary_tick = section(progression, "public static void tickInfirmary", "private static boolean isDaytime")
    assert "daytime && !VillageRespawnSystem.isDowned(player)" in infirmary_tick
    assert "setFoodLevel(20)" in infirmary_tick
    assert "setSaturation(5.0f)" in infirmary_tick

    # Night player-count and reward ownership are frozen at night start and survive reconnect timing.
    snapshot = section(progression, "public static synchronized void captureNightStartSnapshot", "public static synchronized int plannedRaidPlayerCount")
    assert "NIGHT_PARTICIPANTS.clear()" in snapshot
    assert "NIGHT_PARTICIPANTS.add(player.getUUID())" in snapshot
    assert "VillageMercenaryDeploymentSystem.prepareNightDeployment(server)" in snapshot
    assert 'NIGHT_PLAYER_PREFIX = "$night_player_"' in progression
    assert "nightParticipants(server)" in raid
    award = section(progression, "public static synchronized void awardRaidCoins", "public static synchronized String claimDailyBread")
    assert "for (UUID playerId : nightParticipants(server))" in award
    assert "COINS.put(playerId, coins(playerId) + granted)" in award
    assert "VillageCouncilState.grantExperience(server, playerId, xp)" in raid
    assert "for (UUID playerId : VillageProgressionSystem.nightParticipants(server))" in relic
    assert "server.getPlayerList().getPlayer(playerId)" in relic

    # Same-day retry provides bounded diminishing shared support instead of resetting accumulated growth.
    restart = section(progression, "public static synchronized void resetForRestart", "private static int claimRetrySupport")
    retry = section(progression, "private static int claimRetrySupport", "public static int upgradeCost")
    assert "claimRetrySupport()" in restart
    assert "MAX_RETRY_SUPPORT_CLAIMS = 3" in progression
    assert "int[] percent = {100, 60, 35}" in retry
    assert "supplies += granted" in retry
    assert "if (fromStart)" in restart and "else {" in restart
    assert "VillageMercenarySystem.restoreNightSnapshot(server)" in restart
    assert "VillageRespawnSystem.recoverAfterGameRestart(server)" in restart
    assert "public static void recoverAfterGameRestart" in respawn
    assert "RESPAWN_AT.clear()" in respawn
    assert "첫 3회 재도전" in ui_service

    # LAN vote lifecycle handles joins/leaves and duplicate casts; mutation packets get a short replay guard.
    assert "public static synchronized void onPlayerJoined" in council
    assert "public static synchronized void onPlayerLoggedOut" in council
    vote = section(council, "public static synchronized String vote", "public static synchronized ExperienceResult grantExperience")
    assert "activeProposal.votes().containsKey(player.getUUID())" in vote
    assert "effectiveOnlineCount(server, departingPlayer)" in council
    assert "LAST_MUTATION" in network
    assert "now - previous.gameTime() <= 4L" in network
    assert "isMutationAction" in network

    # Next-stage and cost information gets priority over generic prose in the observed clipped screens.
    assert "branch.description(level + 1)" in controller
    assert "비용: 공동 보급품" in controller
    assert "int textBottom = button.y() - 8" in detail
    assert "authoritative current/next values and cost" in detail
    assert "Math.min(2, lines.size())" in town

    print("[PASS] four-face wall stairs, pads and ranger posts occupy non-overlapping authored traffic lanes")
    print("[PASS] wall-top rail ownership is reduced to inner crenellation plus one exterior safety rail")
    print("[PASS] v0.18.37 saves receive geometry-only migration with segment/turret state reprojected")
    print("[PASS] early sapper movement, health and structure pressure are reduced without deleting its role")
    print("[PASS] frontline mercenaries predeploy outside and raid mobs can genuinely engage them")
    print("[PASS] player/bastion taunts override objective routing while friendly arrows and damage ignore mercenaries")
    print("[PASS] final-wave inaccessible stragglers are recovered and zero-turret nights explain where to build")
    print("[PASS] shared defenses use shared supplies and turret confirmation rechecks capacity atomically")
    print("[PASS] storehouse supply conversion, daytime hunger lock and bounded retry support are wired")
    print("[PASS] frozen night participants receive raid rewards by UUID despite disconnect timing")
    print("[PASS] LAN vote join/leave/replay edge cases are server-authoritative")
    print("[PASS] observed next-stage/cost UI information is prioritized in detail layouts")
    print("[PASS] v0.18.38 LAN playtest hardening contract complete")


if __name__ == "__main__":
    main()
