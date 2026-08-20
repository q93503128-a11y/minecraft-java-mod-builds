#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def require(label: str, condition: bool) -> None:
    if not condition:
        raise AssertionError(label)
    print(f"[PASS] {label}")

def method_body(text: str, start: str, end: str) -> str:
    a = text.index(start)
    b = text.index(end, a)
    return text[a:b]

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    raid = read("VillageRaidSystem.java")
    attack = read("VillageAttackPlanSystem.java")
    elite = read("VillageEnemyEliteSystem.java")
    boss = read("VillageSiegeBossSystem.java")
    defense = read("VillageDefenseSystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    guardians = read("VillageGuardians.java")

    require("release metadata remains present", "mod_version=" in props)

    identity = method_body(raid, "public static boolean isRaidEnemy(Entity entity)",
                           "public static boolean isBossEnemy")
    require("raid identity is authoritative metadata/tag based, never display-name based",
            "ACTIVE_ENEMIES.contains(uuid)" in identity
            and "ACTIVE_ARCHETYPES.containsKey(uuid)" in identity
            and "ACTIVE_WAVES.containsKey(uuid)" in identity
            and "entityTags().contains(RAID_ENEMY_TAG)" in identity
            and "getCustomName" not in identity
            and "웨이브" not in identity)

    release = method_body(raid, "private static void releaseEnemy", "private static void clearState")
    require("individual raid enemy release forgets front elite and boss transient ownership immediately",
            "VillageAttackPlanSystem.forget(uuid);" in release
            and "VillageEnemyEliteSystem.forget(uuid);" in release
            and "VillageSiegeBossSystem.forget(uuid);" in release)
    clear = method_body(raid, "private static void clearState", "}")
    require("raid end clears front elite and boss state without waiting for another night",
            "VillageAttackPlanSystem.clearRaidState();" in raid
            and "VillageEnemyEliteSystem.clearRaidState();" in raid
            and "VillageSiegeBossSystem.clearRaidState();" in raid
            and "public static void clearRaidState()" in attack
            and "public static void clearRaidState()" in elite
            and "public static void clearRaidState()" in boss)
    require("game-over cannot retrigger a fake night-front warning after cleanup",
            "if (VillageProgressionSystem.isGameOver())" in attack
            and "lastPhase = VillageCouncilState.currentPhase();" in attack)

    require("retired fixed-corner tower combat implementation is physically absent",
            "TOWER_TICKS" not in defense
            and "fireBallista" not in defense
            and "fireFlame" not in defense
            and "fireFrost" not in defense
            and "fireArcane" not in defense
            and "public static void tick(MinecraftServer server)" not in defense
            and "VillageDefenseSystem.tick(" not in guardians
            and "Production combat is owned exclusively by VillagePlacedTurretSystem" in defense)

    require("front warnings and actual wave arrivals use synchronized procedural mesh actors",
            "raidFrontWarning" in effects
            and "raidFrontArrival" in effects
            and "VillageDefenseEffectSystem.raidFrontWarning" in attack
            and "VillageDefenseEffectSystem.raidFrontArrival" in attack
            and "VillageAttackPlanSystem.renderWaveArrival(level, day, wave, count);" in raid
            and "raid_front_warning" in mesh
            and "raid_front_arrival" in mesh
            and "renderRaidFrontSignal" in mesh)
    require("legacy smoke/fire remains only as secondary atmosphere rather than the only warning language",
            "sendParticles(ParticleTypes.SMOKE" in attack
            and "sendParticles(ParticleTypes.FLAME" in attack
            and attack.index("VillageDefenseEffectSystem.raidFrontWarning") < attack.index("sendParticles(ParticleTypes.SMOKE"))

    print("[PASS] v0.18.21 raid lifecycle and front presentation contracts complete")

if __name__ == "__main__":
    main()
