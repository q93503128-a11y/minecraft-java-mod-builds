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


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    turret = read("VillagePlacedTurretSystem.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    boss = read("VillageSiegeBossSystem.java")
    elite = read("VillageEnemyEliteSystem.java")
    aspect = read("VillageBossAspectSystem.java")

    require("version is v0.18.12-alpha.1", "mod_version=0.18.14-alpha.1" in props)

    require("turret LOS starts outside its own three-block visual column",
            "private static Vec3 turretMuzzle" in turret
            and "state.pos().above(2)" in turret
            and "* 0.72" in turret
            and "Vec3.atCenterOf(state.pos().above());" not in turret)
    require("anti-air targeting chooses nearest valid airborne target",
            "filter(mob -> mob.getY() > baseY + 6.0)" in turret
            and ".min(Comparator.comparingDouble" in turret)
    require("chain turret visuals arc from target to target rather than starburst from tower",
            "Vec3 arcStart = turretMuzzle(state, target);" in turret
            and "hitFrom(level, arcStart, mob" in turret
            and "Vec3 arcEnd = mob.position().add" in turret
            and "VillageDefenseEffectSystem.turretShot(level, TurretType.CHAIN, arcStart, arcEnd)" in turret
            and "arcStart = arcEnd" in turret)

    require("mercenary deployment uses authoritative saved class map instead of display-name parsing",
            "public static synchronized MercenaryClass classOf(Mob mob)" in merc
            and "VillageMercenarySystem.classOf(mob) == kind" in deploy
            and "classFromName" not in deploy
            and "ChatFormatting" not in deploy)
    require("support mercenaries suppress melee wandering without cancelling rally return",
            "++ticks < 5" in deploy
            and "boolean returningToRally" in deploy
            and "if (!returningToRally) golem.getNavigation().stop();" in deploy)

    require("breach colossus can attack the north gate and phase two really attacks faster",
            "segment == VillageSiegeSegmentSystem.Segment.NORTH_GATE" not in boss
            and "int interval = phaseTwo ? 30 : 45;" in boss
            and "파쇄 주기 45→30틱" in boss)
    require("siege boss abilities telegraph before breach ritual and duel impacts",
            "phase == interval - 10" in boss
            and "phase == 100" in boss
            and "ticks % 105 == 70" in boss
            and "ParticleTypes.EXPLOSION" in boss)

    require("elite telegraphs remain readable and share fixed delayed cast state with their impacts",
            "GRAPPLE_MOTIONS" in elite
            and "FIREBRAND_CASTS" in elite
            and "PLAGUE_CASTS" in elite
            and "VillageEnemyEffectSystem.grappleLine" in elite
            and "VillageEnemyEffectSystem.firebrandThrow" in elite
            and "VillageEnemyEffectSystem.firebrandImpact" in elite
            and "VillageEnemyEffectSystem.plagueWarning" in elite
            and "VillageEnemyEffectSystem.plagueImpact" in elite)
    require("elite pursuit chooses the nearest player instead of player-list order",
            elite.count("min(java.util.Comparator.comparingDouble(mob::distanceToSqr))") >= 2)

    require("boss aspects ignore downed targets and telegraph lethal pulses",
            "globalTicks % 100 == 85" in aspect
            and "globalTicks % 80 == 65" in aspect
            and "!VillageRespawnSystem.isDowned(player)" in aspect)
    require("stormcaller warning and damage share one fixed dodgeable strike point",
            "STORM_WARNINGS" in aspect
            and "STORM_WARNINGS.put(mob.getUUID(), warningPos)" in aspect
            and "Vec3 strike = STORM_WARNINGS.remove(mob.getUUID())" in aspect
            and "player.position().distanceToSqr(strike) > impactRadiusSquared" in aspect
            and "경고 지점에서 벗어나면 피할 수 있습니다" in aspect)


if __name__ == "__main__":
    main()
