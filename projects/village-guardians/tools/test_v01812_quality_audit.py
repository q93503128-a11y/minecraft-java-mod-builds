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

    require("version is v0.18.12-alpha.1", "mod_version=0.18.12-alpha.1" in props)

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
            and "arcStart = mob.position().add" in turret)

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

    require("elite instant effects now expose readable pre-cast telegraphs",
            "phase == 88" in elite
            and "phase == 82" in elite
            and "phase == 100" in elite
            and "ParticleTypes.ENCHANT" in elite
            and "ParticleTypes.SMOKE" in elite)
    require("elite pursuit chooses the nearest player instead of player-list order",
            elite.count("min(java.util.Comparator.comparingDouble(mob::distanceToSqr))") >= 2)

    require("boss aspect telegraphs lethal pulses and ignores downed players",
            "globalTicks % 100 == 85" in aspect
            and "globalTicks % 80 == 65" in aspect
            and "!VillageRespawnSystem.isDowned(player)" in aspect)


if __name__ == "__main__":
    main()
