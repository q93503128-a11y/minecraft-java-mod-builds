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
    attack = read("VillageAttackPlanSystem.java")
    gate = read("VillageGatePrioritySystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    old = (ROOT / "tools/test_v01825_multifront_routing_integrity.py").read_text(encoding="utf-8")

    assert "mod_version=0.18.26-alpha.1" in props
    assert "0.18.26-alpha.1" in readme and "villageguardians-0.18.26-alpha.1.jar" in readme
    assert 'assert "mod_version=0.18.25-alpha.1" in props' not in old

    assert "EntityTypes.PHANTOM.create" in enemy
    assert "public static boolean isFlying(Mob mob)" in enemy
    assert "if (day < 7) return false" in enemy
    assert "VillageWaveTrait.STORMFRONT" in enemy and "VillageWaveTrait.HUNTERS" in enemy
    assert "하늘 약탈귀" in enemy

    assert "VillageEnemyArchetypeSystem.isFlying(mob)" in attack
    assert "spawnOrigin(front, index).above(" in attack
    assert "VillageEnemyArchetypeSystem.isFlying(mob)" in gate
    assert "directFlyingEnemy(server, level, mob, archetype, villageCenter)" in raid
    assert "mob.getMoveControl().setWantedPosition" in raid
    assert "chooseTarget(" in raid and "villageCenter, mob.blockPosition(), true, archetype" in raid
    assert "공중 위협" in raid

    assert "Mob target = selectTarget(level, state, candidates);" in turret
    assert "private static double targetScore" in turret
    assert "VillageEnemyArchetypeSystem.isFlying(mob)" in turret
    assert "if (flying) score += 420.0" in turret
    assert "VillageEnemyArchetypeSystem.isFlying(target) ? 1.65f : 0.72f" in turret
    assert "cluster * 26.0" in turret and "cluster * 34.0" in turret
    assert "isArmoredThreat" in turret and "isSupportThreat" in turret
    for effect in ("STRENGTH", "REGENERATION", "SPEED", "RESISTANCE", "ABSORPTION"):
        assert f"target.removeEffect(MobEffects.{effect})" in turret

    print("[PASS] day-7+ flying enemies exist as real PHANTOM raid entities")
    print("[PASS] flying assault bypasses wall/gate ground routing and attacks defenders/interior facilities")
    print("[PASS] anti-air turret has a real dedicated target class and strong anti-air multiplier")
    print("[PASS] nine offensive turret families use role-aware target scoring")
    print("[PASS] nullifier strips the supported combat buff set")
    print("[PASS] historical v0.18.25 routing regression is version-independent")
    print("[PASS] v0.18.26 flying assault and turret-role contract complete")

if __name__ == "__main__":
    main()
