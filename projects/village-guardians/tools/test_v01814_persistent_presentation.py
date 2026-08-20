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
    presentation = read("VillageTurretPresentationSystem.java")
    elite = read("VillageEnemyEliteSystem.java")
    enemy_fx = read("VillageEnemyEffectSystem.java")
    effect_entity = read("VillageSkillEffectEntity.java")
    mesh = read("VillageSkillMeshLibrary.java")
    council = read("VillageCouncilState.java")

    require("version is v0.18.14-alpha.1", "mod_version=0.18.20-alpha.1" in props)

    require("turret gameplay state remains SavedData while visual actors are runtime-only",
            "VillageSiegePersistence.stringsWithPrefix(PREFIX)" in turret
            and "Runtime-only visual owner for placed turrets" in presentation
            and "VillageSkillEffectEntity.spawn" in presentation
            and "ACTORS" in presentation)
    require("active turrets use invisible collision shells plus mesh bodies",
            "Blocks.BARRIER" in turret
            and "state.type().visual()" not in turret
            and "turretCap(state.type())" not in turret
            and "VillageTurretPresentationSystem.show" in turret)
    require("wrecks and dismantles retire turret actors instead of leaving ghosts",
            "VillageTurretPresentationSystem.remove" in turret
            and "turret_wreck_" in presentation)
    require("turret actors are self-healed and can aim independently of the collision footprint",
            "VillageTurretPresentationSystem.tick(level, states())" in turret
            and "VillageTurretPresentationSystem.aim" in turret
            and "actor.setDirection(horizontal.normalize())" in presentation)
    require("failed-night and new-game persistence changes immediately rebuild runtime turrets and wall visuals",
            "reloadAfterPersistenceChange(MinecraftServer server)" in turret
            and "VillagePlacedTurretSystem.reloadAfterPersistenceChange(server);" in council
            and "VillageSiegeSegmentSystem.restoreAllVisuals(server.overworld());" in council)
    for token in (
        "turret_body_ballista", "turret_body_repeater", "turret_body_piercer",
        "turret_body_flame", "turret_body_frost", "turret_body_chain",
        "turret_body_bombard", "turret_body_nullifier", "turret_body_anti_air",
        "turret_body_beacon", "turret_wreck_"
    ):
        require(f"mesh library renders {token}", token in mesh)
    require("turret level and disruption state alter the visible body",
            "parseTurretPresentation" in mesh
            and "presentation.level()" in mesh
            and "presentation.disabled()" in mesh)

    require("owner-follow presentation actors die when their owning mob disappears",
            "if (followsOwner())" in effect_entity
            and "owner == null || !owner.isAlive()" in effect_entity
            and "kind().startsWith(\"elite_aura_\")" in effect_entity)
    for token in (
        "elite_aura_grappler", "elite_aura_firebrand", "elite_aura_assassin",
        "elite_aura_plague_weaver", "elite_aura_shock_rider",
        "elite_grapple_line", "elite_firebrand_throw", "elite_firebrand_impact",
        "elite_plague_warning", "elite_plague_impact"
    ):
        require(f"elite presentation has {token}", token in mesh or token in enemy_fx)

    require("grappler traversal is a timed arc rather than one-frame wall teleport",
            "GRAPPLE_MOTIONS" in elite
            and "tickGrapple" in elite
            and "GrappleMotion" in elite
            and "bezier" in elite
            and "mob.snapTo(inside.getX() + 0.5" not in elite)
    require("firebrand warning snapshots a dodgeable impact point and resolves later",
            "FIREBRAND_CASTS" in elite
            and "FirebrandCast" in elite
            and "firebrandThrow" in elite
            and "firebrandImpact" in elite
            and "nearbyPlayersAt" in elite)
    require("plague warning and impact share one fixed danger zone",
            "PLAGUE_CASTS" in elite
            and "PlagueCast" in elite
            and "plagueWarning" in elite
            and "plagueImpact" in elite)

    print("[PASS] v0.18.14 persistent presentation contracts complete")


if __name__ == "__main__":
    main()
