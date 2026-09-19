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
    enemy = read("VillageEnemyArchetypeSystem.java")
    raid = read("VillageRaidSystem.java")

    assert "mod_version=0.18.43-alpha.1" in props
    assert "현재 소스 버전 `0.18.43-alpha.1`" in readme
    assert "villageguardians-0.18.43-alpha.1.jar" in readme

    # finalizeSpawn is allowed to randomize vanilla zombies, but authored raid roles must win afterward.
    equip = section(enemy, "private static void equip", "private static void applyArchetypeAttributes")
    assert "zombie.stopRiding()" in equip
    assert "zombie.setBaby(archetype == Archetype.RUSHER || archetype == Archetype.SAPPER)" in equip

    # A stalled actor from wave 2 must be recoverable before maxWaves instead of being carried all night.
    tick = section(raid, "public static void tick", "public static void onLivingDeath")
    assert tick.index("recoverFrozenFinalEnemies(server)") < tick.index("if (wave >= maxWaves)")

    # Progress includes the opponent's HP, so a legitimately stationary melee exchange is not misclassified.
    recovery = section(raid, "private static void recoverFrozenFinalEnemies", "private static boolean shouldRecoverStalledEnemy")
    assert "previous.targetHealth()" in recovery
    assert "targetHealthChanged" in recovery
    assert "progressSnapshot(mob, 0)" in recovery

    # Close-but-deadlocked melee actors are no longer exempt, and runtime relocation is network-authoritative.
    stalled = section(raid, "private static boolean shouldRecoverStalledEnemy", "private static boolean isMeleePursuer")
    assert "if (isMeleePursuer(archetype))" in stalled
    assert "return true;" in stalled
    assert "mob.teleportTo(level" in recovery
    assert "mob.snapTo(rally" not in recovery
    assert "mob.stopRiding()" in recovery

    print("[PASS] ordinary GRUNT zombies cannot inherit random vanilla baby/jockey morphology")
    print("[PASS] stalled enemies are recovered on intermediate waves using target-health progress")
    print("[PASS] close frozen melee actors are no longer exempt and relocation is client/server synchronized")


if __name__ == "__main__":
    main()
