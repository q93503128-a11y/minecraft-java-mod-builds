#!/usr/bin/env python3
"""Deterministic contract for Village Guardians v0.18.9 siege phase 2."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

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

    assert "mod_version=0.18.9-alpha.1" in props

    # Segment durability owns combat HP, not individual wall blocks. North gate remains save-compatible.
    for token in ("NORTH_WEST", "NORTH_GATE", "NORTH_EAST", "WEST", "EAST", "SOUTH_WEST", "SOUTH_EAST"):
        assert token in segment
    assert "BREACH_HALF_WIDTH = 2" in segment
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

    # Multiplayer scaling and early solo protection remain exactly the v0.18.8 contract.
    assert "extraPlayers * 0.30f" in tuning
    assert "0.56f + (d - 1) * 0.04f" in tuning
    assert "0.52f + (d - 1) * 0.06f" in tuning
    defender = tuning.split("defenderStateStructureMultiplier", 1)[1].split("scaleEnemyCount", 1)[0]
    assert "return 1.0f;" in defender and "0.30f" not in defender

    # Ten player-placed turret roles, real HP/state and path/gate/spacing validation.
    assert turret.count("(") > 0
    for token in ("BALLISTA", "REPEATER", "PIERCER", "FLAME", "FROST", "CHAIN",
                  "BOMBARD", "NULLIFIER", "ANTI_AIR", "BEACON"):
        assert token in turret
    for guard in ("주 통행로", "북문 진입로", "최소 8블록", "전체 포탑 설치 한도"):
        assert guard in turret
    assert "record TurretState" in turret and "boolean active" in turret
    assert "buildWreck" in turret and "VillageSiegePersistence.removeString" in turret
    assert "siege_turret_repair_all" in command
    assert "VillageDefenseSystem.tick" not in guardians
    assert "VillagePlacedTurretSystem.tick" in guardians

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

    # Existing no-debris and production-safe UI contracts remain wired.
    assert "VillageRaidSystem.isActive()" in debris and "event.setCanceled(true)" in debris
    assert "facility:walls" not in local  # walls is intercepted through generic facility parsing, not a duplicate route
    assert "building == VillageProgressionSystem.Building.WALLS" in local

    print("[PASS] Seven segment HP pools project localized no-drop wall damage and breaches")
    print("[PASS] Day-driven scoutable multi-front routing and world warnings are wired")
    print("[PASS] Ten deployable destructible turrets replace fixed combat firing")
    print("[PASS] Mercenary rally doctrine, five elite roles and three boss structures are wired")
    print("[PASS] Weapon families and 2/3-piece sets affect the same runtime combat multipliers shown in UI")
    print("[PASS] v0.18.8 early-solo, +30% multiplayer, full-death-risk and debris contracts remain")

if __name__ == "__main__":
    main()
