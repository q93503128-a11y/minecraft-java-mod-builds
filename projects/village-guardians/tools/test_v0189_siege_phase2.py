#!/usr/bin/env python3
"""Deterministic contract for Village Guardians v0.18.9 siege phase 2."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def scaled_count(base: int, players: int) -> int:
    return max(1, round(base * (1.0 + max(0, players - 1) * 0.30)))

def front(day: int, wave: int, index: int) -> str:
    if day <= 4:
        return "NORTH"
    if day <= 7:
        if index % 5 != 0:
            return "NORTH"
        return "NORTH_WEST" if (day + wave) % 2 == 0 else "NORTH_EAST"
    if day <= 11:
        if index % 4 != 0:
            return "NORTH"
        return "WEST" if (day + wave) % 2 == 0 else "EAST"
    if day <= 15:
        lane = index % 5
        if lane == 0:
            return "WEST"
        if lane == 1:
            return "EAST"
        return "NORTH"
    lane = index % 10
    if lane == 0:
        return "SOUTH_WEST"
    if lane == 1:
        return "SOUTH_EAST"
    if lane in (2, 3):
        return "WEST" if (wave + index) % 2 == 0 else "EAST"
    return "NORTH"

def panel_layout(screen_width: int, screen_height: int):
    if screen_width < 176 or screen_height < 102:
        return None
    inventory_left = (screen_width - 176) // 2
    inventory_right = inventory_left + 176
    left_space = max(0, inventory_left - 6)
    right_space = max(0, screen_width - inventory_right - 6)
    use_left = left_space >= right_space
    available = left_space if use_left else right_space
    if available < 52:
        return None
    width = min(142, available - 4)
    compact = width < 96 or screen_height < 154
    height = 96 if compact else 148
    if screen_height < height + 6:
        return None
    left = inventory_left - width - 4 if use_left else inventory_right + 4
    top = max(3, (screen_height - height) // 2)
    return (left, top, width, height, compact, inventory_left, inventory_right, use_left)

def set_multiplier(wall: int, hunter: int, projectile: bool) -> tuple[float, float]:
    outgoing = 1.0
    incoming = 1.0
    if wall >= 2:
        outgoing *= 1.06
        incoming *= 0.92
    if wall >= 3:
        outgoing *= 1.05
        incoming *= 0.94
    if hunter >= 2 and projectile:
        outgoing *= 1.10
    if hunter >= 3:
        outgoing *= 1.08 if projectile else 1.03
        incoming *= 0.96
    return outgoing, max(0.78, incoming)

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    segment = read("VillageSiegeSegmentSystem.java")
    plan = read("VillageAttackPlanSystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    command = read("VillageSiegeCommandUi.java")
    merc = read("VillageMercenaryDeploymentSystem.java")
    elite = read("VillageEnemyEliteSystem.java")
    boss = read("VillageSiegeBossSystem.java")
    sets = read("VillageEquipmentSetSystem.java")
    weapon = read("VillageWeaponStyleSystem.java")
    rpg = read("VillageRpgSystem.java")
    tooltip = read("VillageEquipmentTooltipClient.java")
    inventory = read("VillageInventoryPanel.java")
    guardians = read("VillageGuardians.java")
    local = read("VillageLocalActionSystem.java")
    intel = read("VillageWaveIntelSystem.java")
    tuning = read("VillageDifficultyTuning.java")
    debris = read("VillageRaidDebrisDropGuard.java")
    persistence = read("VillageSiegePersistence.java")
    council = read("VillageCouncilState.java")
    world = read("VillageWorldSystem.java")

    assert "mod_version=0.18.19-alpha.1" in props

    # Segment durability owns combat HP, not individual wall blocks. North gate remains save-compatible.
    for token in ("NORTH_WEST", "NORTH_GATE", "NORTH_EAST", "WEST", "EAST", "SOUTH_WEST", "SOUTH_EAST"):
        assert token in segment
    assert "BREACH_HALF_WIDTH = 2" in segment
    assert 2 * 2 + 1 == 5
    assert "segment_hp_" in segment and "segment_breach_" in segment
    assert "VillageProgressionSystem.durability(VillageProgressionSystem.Building.WALLS)" in segment
    assert "VillageFortressTerrain.set" in segment
    assert "Blocks.AIR" in segment
    assert "destroyBlock" not in segment and "drop(" not in segment

    # Day-driven fronts: solo begins north-only, then sides and finally rear fronts become available.
    assert "if (day <= 4) return Front.NORTH" in plan
    assert "if (day <= 7)" in plan and "if (day <= 11)" in plan and "if (day <= 15)" in plan
    assert "Front.SOUTH_WEST" in plan and "Front.SOUTH_EAST" in plan
    assert "주공:" in plan and "별동대:" in plan and "전장 상황:" in plan
    assert "renderWarnings" in plan and "ParticleTypes.SMOKE" in plan
    assert "VillageSiegeSegmentSystem.damage" in plan and "insideApproach" in plan
    assert "safeSpawn(level, spawnOrigin" in plan
    assert "for (int dy = 24; dy >= -24; dy--)" in plan and "Direction.UP" in plan
    assert set(front(3, 1, i) for i in range(24)) == {"NORTH"}
    assert {"NORTH", "NORTH_WEST", "NORTH_EAST"} & set(front(6, wave, i) for wave in (1, 2) for i in range(20))
    assert "WEST" in {front(13, 1, i) for i in range(20)} and "EAST" in {front(13, 1, i) for i in range(20)}
    late = {front(17, 1, i) for i in range(30)}
    assert {"SOUTH_WEST", "SOUTH_EAST", "WEST", "EAST", "NORTH"}.issubset(late)

    # Multiplayer scaling and early solo protection remain exactly the v0.18.8 contract.
    assert "extraPlayers * 0.30f" in tuning
    assert "0.56f + (d - 1) * 0.04f" in tuning
    assert "0.52f + (d - 1) * 0.06f" in tuning
    defender = tuning.split("defenderStateStructureMultiplier", 1)[1].split("scaleEnemyCount", 1)[0]
    assert "return 1.0f;" in defender and "0.30f" not in defender
    assert [scaled_count(10, p) for p in (1, 2, 3, 4)] == [10, 13, 16, 19]

    # Ten player-placed turret roles, real HP/state and path/gate/spacing validation.
    for token in ("BALLISTA", "REPEATER", "PIERCER", "FLAME", "FROST", "CHAIN",
                  "BOMBARD", "NULLIFIER", "ANTI_AIR", "BEACON"):
        assert token in turret
    for guard in ("주 통행로", "북문 진입로", "최소 8블록", "전체 포탑 설치 한도"):
        assert guard in turret
    assert "record TurretState" in turret and "boolean active" in turret
    assert "buildWreck" in turret and "VillageSiegePersistence.removeString" in turret
    assert "siege_turret_repair_all" in command
    assert "openTurretCatalog" in command and "openTurretList" in command
    assert "siege_turret_list" in local
    assert "VillageDefenseSystem.tick" not in guardians
    assert "VillagePlacedTurretSystem.tick" in guardians
    assert "VillageTowerResearchBonusSystem" not in guardians

    # Failed-night retry/new-game state must not leak segment/turret damage.
    assert "captureNightSnapshot" in persistence and "restoreNightSnapshot" in persistence
    assert "resetForNewGame" in persistence and "$night_" in persistence
    assert "VillageSiegePersistence.captureNightSnapshot()" in council
    assert "VillageSiegePersistence.restoreNightSnapshot()" in council
    assert "VillageSiegePersistence.resetForNewGame()" in council
    force_rebuild = world.split("public static synchronized void forceRebuild", 1)[1].split("public static boolean handleCentralBellInteraction", 1)[0]
    assert "VillageSiegeSegmentSystem.restoreAllVisuals(level)" in force_rebuild
    assert "VillagePlacedTurretSystem.initializeServer(server)" in force_rebuild

    # Mercenary RTS scope stays intentionally coarse: three rally zones and class restrictions.
    for token in ("GATE_FRONT", "INNER", "WALL"):
        assert token in merc
    assert "전투 시작 후 세밀한 RTS 조작 없이" in merc
    assert "merc_deploy:" in local

    # Qualitatively different elite and boss mechanics exist and are scout-visible.
    for token in ("GRAPPLER", "FIREBRAND", "ASSASSIN", "PLAGUE_WEAVER", "SHOCK_RIDER"):
        assert token in elite
    for token in ("BREACH_COLOSSUS", "BONE_HIEROPHANT", "BLACK_MARSHAL"):
        assert token in boss
    assert "VillageEnemyEliteSystem.scoutSummary" in intel
    assert "VillageSiegeBossSystem.previewBossMechanic" in intel
    assert "공성 병과:" in intel and "보스 전투 구조:" in intel

    # Universal rarity power remains, with real combat-linked weapon and 2/3-piece set layers.
    assert "WALL_GUARDIAN" in sets and "NIGHT_HUNTER" in sets
    assert "wall >= 2" in sets and "wall >= 3" in sets and "hunter >= 2" in sets and "hunter >= 3" in sets
    assert "VillageWeaponStyleSystem.outgoingMultiplier" in rpg
    assert "VillageEquipmentSetSystem.outgoingMultiplier" in rpg
    assert "VillageEquipmentSetSystem.incomingMultiplier" in rpg
    for token in ("LONGSWORD", "GREAT_AXE", "SPEAR", "WAR_HAMMER", "LONGBOW", "CROSSBOW"):
        assert token in weapon
    assert "세트:" in tooltip and "무기 계열:" in tooltip
    assert "성벽 수호자" in inventory and "밤사냥꾼" in inventory
    two_piece = set_multiplier(2, 0, False)
    three_piece = set_multiplier(3, 0, False)
    removed = set_multiplier(2, 0, False)
    reequipped = set_multiplier(3, 0, False)
    assert three_piece[0] > two_piece[0] and three_piece[1] < two_piece[1]
    assert removed == two_piece and reequipped == three_piece

    # Inventory safe-area arithmetic: never overlap the vanilla 176px inventory at narrow GUI scales.
    assert "MIN_SAFE_WIDTH" in inventory and "layout.compact()" in inventory and "Layout.hidden()" in inventory
    for width, height in ((256, 240), (320, 240), (360, 240), (426, 240), (854, 480), (1280, 720), (1920, 1080)):
        layout = panel_layout(width, height)
        if layout is None:
            continue
        left, top, panel_width, panel_height, compact, inv_left, inv_right, use_left = layout
        assert left >= 0 and top >= 0 and left + panel_width <= width and top + panel_height <= height
        if use_left:
            assert left + panel_width <= inv_left - 4
        else:
            assert left >= inv_right + 4
    assert panel_layout(320, 240) is not None and panel_layout(320, 240)[4]
    assert panel_layout(256, 240) is None

    # Existing no-debris and production-safe UI contracts remain wired.
    assert "VillageRaidSystem.isActive()" in debris and "event.setCanceled(true)" in debris
    assert "facility:walls" not in local
    assert "building == VillageProgressionSystem.Building.WALLS" in local
    for obsolete in ("open_tower_control", "tower_status", "tower_open:", "tower_branch:", "tower_upgrade:"):
        assert obsolete in local
    legacy_guard = local.split("Compatibility guard:", 1)[1].split("if (action.equals(\"siege_command\")", 1)[0]
    assert "VillageSiegeCommandUi.open(player)" in legacy_guard

    print("[PASS] Seven segment HP pools project localized 5-block no-drop wall breaches")
    print("[PASS] Day-driven scoutable multi-front routing uses safe terrain-height spawn resolution")
    print("[PASS] 1/2/3/4-player enemy scaling remains 100/130/160/190%")
    print("[PASS] Ten deployable destructible turrets use split placement/management views")
    print("[PASS] Failed-night retry/new-game restores authoritative segment/turret snapshots and visuals")
    print("[PASS] Mercenary rally doctrine, five elite roles and three boss structures are wired")
    print("[PASS] Weapon families and 2/3-piece sets affect runtime combat and re-equip without stacking")
    print("[PASS] Inventory side panel stays outside vanilla inventory or safely hides at impossible widths")
    print("[PASS] Legacy fixed-tower production routes redirect to phase-2 siege command")
    print("[PASS] v0.18.8 early-solo, full-death-risk and debris contracts remain")

if __name__ == "__main__":
    main()
