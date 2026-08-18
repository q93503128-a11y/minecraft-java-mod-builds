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
    boss = read("VillageSiegeBossSystem.java")
    aspect = read("VillageBossAspectSystem.java")
    effects = read("VillageBossEffectSystem.java")
    entity = read("VillageSkillEffectEntity.java")
    mesh = read("VillageSkillMeshLibrary.java")

    require("version is v0.18.15-alpha.1", "mod_version=0.18.15-alpha.1" in props)

    require("boss doctrines diversify by day wave and actual boss archetype",
            "doctrineFor(" in boss
            and "VillageRaidSystem.waveOf(mob)" in boss
            and "type.ordinal()" in boss
            and "혼성 보스 교리" in boss)
    require("boss presence combines aspect and siege doctrine in one persistent owner-follow silhouette",
            "VillageBossAspectSystem.aspectOf(mob)" in boss
            and "VillageBossEffectSystem.presence" in boss
            and "boss_presence_" in effects
            and "kind().startsWith(\"boss_presence_\")" in entity
            and "renderBossPresence" in mesh)
    require("phase two adds a persistent readable boss layer plus transition burst",
            "VillageBossEffectSystem.phaseTwo" in boss
            and "boss_phase_two_" in effects
            and "boss_phase_two_burst" in effects
            and "kind().startsWith(\"boss_phase_two_\")" in entity)

    require("breach colossus warning and wall damage consume one fixed cast snapshot",
            "BREACH_CASTS" in boss
            and "new BreachCast(segment, target.immutable(), ticks + 10)" in boss
            and "BreachCast cast = BREACH_CASTS.remove" in boss
            and "VillageSiegeSegmentSystem.damage(server, cast.segment(), damage, cast.impact())" in boss
            and "breachWarning" in boss
            and "breachImpact" in boss)
    require("bone hierophant ritual uses one fixed world-space zone from warning through buff resolution",
            "RITUAL_CASTS" in boss
            and "new RitualCast(center, ticks + 20)" in boss
            and "activeEnemiesNear(level, cast.center(), 15.0" in boss
            and "ritualWarning" in boss
            and "ritualImpact" in boss)
    require("black marshal mark cannot jump to another player between warning and impact",
            "DUEL_CASTS" in boss
            and "new DuelCast(target.getUUID(), ticks + 35)" in boss
            and "level.getEntity(cast.target())" in boss
            and "duelMark" in boss
            and "duelImpact" in boss)

    require("bloodbound drain warning and damage share one fixed world-space center",
            "BLOOD_WARNINGS" in aspect
            and "BLOOD_WARNINGS.put(mob.getUUID(), center)" in aspect
            and "Vec3 center = BLOOD_WARNINGS.remove(mob.getUUID())" in aspect
            and "nearbyPlayersAt(server, level, center, 11.0)" in aspect
            and "bloodboundWarning" in aspect
            and "bloodboundImpact" in aspect)
    require("stormcaller keeps fixed dodgeable ground point and now uses mesh warning",
            "STORM_WARNINGS.put(mob.getUUID(), warningPos)" in aspect
            and "VillageBossEffectSystem.stormWarning" in aspect
            and "Vec3 strike = STORM_WARNINGS.remove(mob.getUUID())" in aspect)

    for token in (
        "boss_presence_breach_colossus", "boss_presence_bone_hierophant", "boss_presence_black_marshal",
        "boss_phase_two_", "boss_phase_two_burst", "boss_breach_warning", "boss_breach_windup",
        "boss_breach_impact", "boss_ritual_warning", "boss_ritual_impact", "boss_duel_mark",
        "boss_duel_impact", "boss_bloodbound_warning", "boss_bloodbound_impact", "boss_storm_warning"):
        require(f"boss procedural presentation includes {token}", token in effects or token in mesh)

    require("boss presentation follows live owners and rotates with boss facing",
            "kind().startsWith(\"boss_presence_\")" in entity
            and "kind().startsWith(\"boss_phase_two_\")" in entity
            and "private boolean tracksOwnerLook()" in entity)

    print("[PASS] v0.18.15 boss identity and cast-state contracts complete")


if __name__ == "__main__":
    main()
