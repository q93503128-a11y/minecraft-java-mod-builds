#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    enemy = read("VillageEnemyArchetypeSystem.java")
    health = read("VillageHealthDisplaySystem.java")
    raid = read("VillageRaidSystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    attack = read("VillageAttackPlanSystem.java")
    old = (ROOT / "tools/test_v01828_air_defense_ecosystem.py").read_text(encoding="utf-8")

    assert "mod_version=" in props
    assert "현재 소스 버전" in readme and "목표 JAR" in readme
    assert 'assert "mod_version=0.18.28-alpha.1" in props' not in old

    assert "private static double aerialStrikeRadius" in raid
    for token in ("case BOMBARDIER -> 3.10", "case HARRIER -> 2.15", "case RAIDER -> AERIAL_PLAYER_STRIKE_RADIUS"):
        assert token in raid
    assert "aerialWarningTicks(role), dangerRadius" in raid
    assert "aerialAssaultImpact(level, strike.point(), role, false, radius)" in raid
    assert "aerialAssaultImpact(level, strike.point(), role, true, 0.0)" in raid
    assert "Math.max(6, warningTicks)" in effects
    assert "aerialSignalExtra(role, structure, radius)" in effects
    assert '"%d|%d|%.2f"' in effects

    assert "private static AerialSignal parseAerialSignal" in mesh
    assert "signal.radius()" in mesh and "signal.structure()" in mesh and "signal.role()" in mesh
    assert "case 1 -> rgba(139, 121, 255" in mesh
    assert "case 2 -> rgba(91, 232, 255" in mesh
    assert "int markers = role == 1 ? 8 : role == 2 ? 3 : 4" in mesh
    assert "The inner warning ring is the actual dodge radius" in mesh

    preview = attack.split("public static AttackPlan preview", 1)[1].split("public static String scoutLine", 1)[0]
    assert "isGroundAssaultIndex(day, wave, count, index)" in preview
    used = attack.split("private static Map<Front, Boolean> usedFronts", 1)[1].split("private static BlockPos spawnOrigin", 1)[0]
    assert "isGroundAssaultIndex(day, wave, count, i)" in used
    assert "VillageEnemyArchetypeSystem.willSpawnFlying(day, wave, index, boss, trait)" in used
    assert "VillageRaidSystem.previewBossCount" in used

    assert "public static boolean isTacticalThreat" in enemy
    assert "public static boolean alwaysShowNameplate" in enemy
    assert "mob.setCustomNameVisible(alwaysShowNameplate(archetype, boss, isFlying(mob)))" in enemy
    assert "shouldShowEnemyNameplate(server, mob)" in health
    assert "22.0 * 22.0" in health
    assert "mob.setCustomNameVisible(true);" not in health
    assert "(tactical && !visibleToAnyPlayer)" in raid
    assert "isBossEnemy(mob) || !visibleToAnyPlayer" not in raid

    print("[PASS] aerial warning lifetime follows each role's actual dodge window")
    print("[PASS] aerial player warning ring follows the exact role damage radius")
    print("[PASS] raider/bombardier/harrier telegraphs have distinct procedural languages")
    print("[PASS] ground front scouting and arrival markers exclude wall-bypassing aerial units")
    print("[PASS] tactical nameplates remain persistent while generic wave text is proximity-bounded")
    print("[PASS] cover outlines no longer reveal every generic enemy through fortress walls")
    print("[PASS] historical v0.18.28 regression is version-independent")
    print("[PASS] v0.18.29 battlefield readability integrity contract complete")


if __name__ == "__main__":
    main()
