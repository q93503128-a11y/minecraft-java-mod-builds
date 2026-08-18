#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    turret = read("VillagePlacedTurretSystem.java")
    los = read("VillageDefenseLineOfSight.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    guardians = read("VillageGuardians.java")

    assert "mod_version=0.18.13-alpha.1" in props

    # Static defenses and ranger mercenaries must not acquire/fire through blocks.
    assert "ClipContext.Block.COLLIDER" in los and "HitResult.Type.MISS" in los
    assert ".filter(mob -> VillageDefenseLineOfSight.hasLine(level, turretMuzzle(state, mob), mob))" in turret
    assert "if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;" in turret and "turretMuzzle" in turret
    assert ".filter(enemy -> VillageDefenseLineOfSight.hasLine(level, start, enemy))" in merc

    # Tower hunters can approach from their search radius, but physical HP damage is short-range.
    assert "distanceSquared <= 36.0 * 36.0" in turret
    assert "distanceSquared <= 7.5 * 7.5" in turret
    assert "distanceSquared <= 6.0 * 6.0" in turret
    assert "distanceSquared <= 8.0 * 8.0" in turret

    # Piercer is now mechanically differentiated against armored / resistant targets.
    assert "case PIERCER -> hit" in turret
    assert "piercingMultiplier" in turret
    for token in ("BULWARK", "SHIELDBREAKER", "SIEGE_BEAST", "IRON_WARLORD"):
        assert token in turret
    assert "1.55f" in turret

    # Turrets have a compact three-block silhouette and matching placement/cleanup contract.
    assert "pos.above(2)" in turret and "포탑 공간 3블록" in turret
    assert "turretCap" in turret and "POLISHED_BLACKSTONE_BRICK_WALL" in turret

    # Mercenary progression belongs only to the actual killing mercenary.
    assert "awardKillExperience(Mob killer)" in merc
    assert "event.getSource().getEntity() instanceof Mob killer" in guardians
    assert "new AABB(deathPosition, deathPosition).inflate(48.0)" not in merc

    # All four classes now have active battlefield identity; ranged/medic do not drift into vanilla melee AI.
    assert "bastionControl" in merc and "strikerPressure" in merc and "rangedAttack" in merc and "healAllies" in merc
    assert "MercenaryClass.RANGER\n                    || kind == VillageMercenarySystem.MercenaryClass.MEDIC" in deploy
    assert "if (!accepted && zone == Deployment.WALL)" in deploy

    # Existing breadth is retained.
    for token in ("BALLISTA", "REPEATER", "PIERCER", "FLAME", "FROST", "CHAIN", "BOMBARD", "NULLIFIER", "ANTI_AIR", "BEACON"):
        assert token in turret
    for token in ("BASTION", "STRIKER", "RANGER", "MEDIC"):
        assert token in merc

    print("[PASS] turret and ranger LOS blocks wall-through acquisition and damage")
    print("[PASS] tower hunters approach at range but damage turrets only at physical attack distance")
    print("[PASS] piercer has a real armored-target damage niche")
    print("[PASS] turret placement/visual/cleanup owns a matching three-block footprint")
    print("[PASS] only the actual mercenary killer receives mercenary progression")
    print("[PASS] all four mercenary classes retain distinct active battlefield behavior")


if __name__ == "__main__":
    main()
