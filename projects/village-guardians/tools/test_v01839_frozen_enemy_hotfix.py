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
    raid = read("VillageRaidSystem.java")

    assert "mod_version=0.18.39-alpha.1" in props
    assert "현재 소스 버전 `0.18.39-alpha.1`" in readme
    assert "villageguardians-0.18.39-alpha.1.jar" in readme

    # Spawned raid actors must never begin as static/invulnerable entities.
    spawn = section(raid, "private static void spawnWave", "private static void applyScaling")
    assert "mob.setNoAi(false)" in spawn
    assert "mob.setInvulnerable(false)" in spawn

    # Every server tick repairs impossible combat flags before routing owns the mob.
    tick = section(raid, "public static void tick", "public static void onLivingDeath")
    assert tick.index("repairInvalidEnemyFlags(server)") < tick.index("directEnemies(server)")
    repair = section(raid, "private static void repairInvalidEnemyFlags", "private static void recoverFrozenFinalEnemies")
    assert "mob.isNoAi()" in repair
    assert "mob.isInvulnerable()" in repair
    assert "mob.setNoAi(false)" in repair
    assert "mob.setInvulnerable(false)" in repair
    assert "VillageGuardians.LOGGER.warn" in repair

    # 0.18.38's distance/LOS guess is gone. Final recovery is based on actual lack of progress.
    assert "FINAL_STRAGGLER_RECOVERY_TICKS" not in raid
    assert "FINAL_STRAGGLER_RECOVERY_INTERVAL" not in raid
    assert "FINAL_ENEMY_STALL_TICKS = 20 * 12" in raid
    recovery = section(raid, "private static void recoverFrozenFinalEnemies", "private static boolean shouldRecoverStalledEnemy")
    assert "previous.position()" in recovery
    assert "previous.health()" in recovery
    assert "lastProgressTick()" in recovery
    assert "distanceToSqr(nearest) > 24.0 * 24.0" not in recovery
    assert "!nearest.hasLineOfSight(mob)" not in recovery
    assert "전투 상태 복구" in recovery

    stalled = section(raid, "private static boolean shouldRecoverStalledEnemy", "private static boolean isMeleePursuer")
    assert "mob.getNavigation().isDone()" in stalled
    assert "isMeleePursuer(archetype)" in stalled
    assert "VillageFortressBuildings.isTouchingStructure" in stalled

    # Runtime state is cleaned with the authoritative enemy lifecycle.
    release = section(raid, "private static void releaseEnemy", "private record TauntState")
    assert "FINAL_ENEMY_PROGRESS.remove(uuid)" in release
    clear = section(raid, "private static void clearState", "}")
    assert "FINAL_ENEMY_PROGRESS.clear()" in raid

    assert "고립되어 북문 안쪽 전선으로 재진입" not in raid
    print("[PASS] v0.18.39 frozen/invulnerable final-enemy recovery uses combat state and progress, not distance/LOS")


if __name__ == "__main__":
    main()
