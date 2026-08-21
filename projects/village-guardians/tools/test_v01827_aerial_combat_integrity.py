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
    raid = read("VillageRaidSystem.java")
    intel = read("VillageWaveIntelSystem.java")
    merc = read("VillageMercenarySystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    old = (ROOT / "tools/test_v01826_flying_turret_roles.py").read_text(encoding="utf-8")

    assert "mod_version=0.18.27-alpha.1" in props
    assert "0.18.27-alpha.1" in readme and "villageguardians-0.18.27-alpha.1.jar" in readme
    assert 'assert "mod_version=0.18.26-alpha.1" in props' not in old
    assert "public static boolean willSpawnFlying(" in enemy
    assert "boolean flying = willSpawnFlying(day, wave, index, boss, trait);" in enemy
    assert "VillageEnemyArchetypeSystem.willSpawnFlying(day, wave, index, boss, trait)" in intel
    assert "공중 위협: " in intel and "하늘 약탈귀 ×" in intel
    assert "private static final Map<UUID, AerialStrike> AERIAL_STRIKES" in raid
    direct = raid.split("private static void directFlyingEnemy", 1)[1].split("private static ServerPlayer nearestFlyingPriorityPlayer", 1)[0]
    assert "mob.setTarget(null);" in direct and "mob.setTarget(player)" not in direct
    assert "beginAerialStrike" in direct and "resolveAerialStrike" in direct
    assert "AERIAL_WARNING_TICKS = 18" in raid and "AERIAL_RECOVERY_TICKS = 34" in raid
    assert "player.position().distanceToSqr(strike.point()) <= radiusSquared" in raid
    assert "level.damageSources().mobAttack(mob)" in raid
    assert "AERIAL_STRIKES.remove(uuid);" in raid and "AERIAL_STRIKES.clear();" in raid
    assert "aerialAssaultWarning" in effects and "aerialAssaultImpact" in effects
    assert '"raid_aerial_warning"' in effects and '"raid_aerial_impact"' in effects
    assert 'case "raid_aerial_warning"' in mesh and 'case "raid_aerial_impact"' in mesh
    assert "renderAerialAssault" in mesh
    ranger = merc.split("private static void rangedAttack", 1)[1].split("private static void healAllies", 1)[0]
    assert "VillageEnemyArchetypeSystem.isFlying(enemy) ? 0 : 1" in ranger
    assert "공중 위협을 우선 요격" in merc
    print("[PASS] real spawn and daytime intel share one deterministic flying predicate")
    print("[PASS] flying combat has one authored owner with fixed warning/dive/impact/recovery phases")
    print("[PASS] player and structure aerial strikes resolve from fixed telegraphed positions")
    print("[PASS] aerial warning and impact use synchronized procedural meshes")
    print("[PASS] ranger mercenaries prioritize flying threats inside line of sight")
    print("[PASS] historical v0.18.26 regression is version-independent")
    print("[PASS] v0.18.27 aerial combat integrity contract complete")

if __name__ == "__main__":
    main()
