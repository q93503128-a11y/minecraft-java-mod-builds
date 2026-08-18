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
    raid = read("VillageRaidSystem.java")
    attack = read("VillageAttackPlanSystem.java")
    enemy = read("VillageEnemyArchetypeSystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    merc = read("VillageMercenarySystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    guardians = read("VillageGuardians.java")
    research = read("VillageDefenseResearchSystem.java")

    require("version is v0.18.13-alpha.1", "mod_version=0.18.15-alpha.1" in props)

    require("raid enemies own authoritative wave metadata before entity join",
            "ACTIVE_WAVES" in raid
            and "ACTIVE_WAVES.put(mob.getUUID(), wave);" in raid
            and "public static int waveOf(Mob mob)" in raid)
    require("attack plan no longer parses gameplay state from display names",
            "parseWave" not in attack
            and "ChatFormatting" not in attack
            and attack.count("VillageRaidSystem.waveOf(mob)") >= 2)

    require("downed players are excluded from generic raid pursuit and archetype abilities",
            "|| VillageRespawnSystem.isDowned(player)) continue;" in raid
            and "!VillageRespawnSystem.isDowned(player)" in enemy)

    require("structure pressure is staggered per attacker instead of one global burst tick",
            "Math.floorMod(structureAttackTicks + id.hashCode(), STRUCTURE_ATTACK_INTERVAL) == 0" in raid
            and "Math.floorMod(attackTicks + id.hashCode(), 30) == 0" in attack)

    require("tower hunter owns one coherent nearest placed-turret objective",
            "public static synchronized TurretState nearestActiveTurret(Vec3 origin, double range)" in turret
            and "disableNearestActiveTurret(mob.position(), 48.0, 20 * 7)" in enemy
            and "archetype == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER" in raid
            and "VillagePlacedTurretSystem.nearestActiveTurret(mob.position(), 48.0)" in raid
            and "mob.getNavigation().moveTo(turretCenter.x, turretCenter.y, turretCenter.z, 1.14)" in raid
            and "disableRandomActiveTurret" not in turret
            and "disableRandomActiveTurret" not in enemy)
    require("turret pressure layer no longer fights raid navigation ownership",
            "Navigation ownership lives in VillageRaidSystem" in turret
            and "mob.getNavigation().moveTo(state.pos()" not in turret)
    require("tower hunter disrupts real placed turrets instead of retired fixed tower kinds",
            "VillageTowerSpecializationSystem.disableRandomInstalledTower" not in enemy
            and "disableNearestActiveTurret" in turret
            and "isDisabled(state.id())" in turret)

    require("legacy duplicate research bonus firing path is retired",
            not (JAVA / "VillageTowerResearchBonusSystem.java").exists()
            and "VillageTowerResearchBonusSystem" not in guardians
            and "VillageDefenseResearchSystem.towerDamageMultiplier()" in turret
            and "1.0f + level(Branch.TOWER) * 0.10f" in research)

    require("bombard is a delayed snapshot-position shell with real impact resolution",
            "PENDING_BOMBARDS" in turret
            and "combatTicks + 12" in turret
            and "resolveBombards(level)" in turret
            and "VillageDefenseEffectSystem.bombardImpact" in turret
            and "case BOMBARD -> queueBombard" in turret)
    require("bombard can arc over cover while direct-fire turrets still require LOS",
            "List<Mob> nearby = VillageRaidSystem.activeEnemiesNear" in turret
            and "state.type() == TurretType.BOMBARD" in turret
            and "nearby.stream().filter(mob -> VillageDefenseLineOfSight.hasLine" in turret)

    require("automated defense visuals use synchronized procedural mesh actors",
            "VillageSkillEffectEntity.spawn(level, null" in effects
            and "turret_ballista_shot" in effects
            and "turret_bombard_arc" in effects
            and "merc_ranger_shot" in effects
            and "siege_structure_impact" in effects)
    for token in (
        "turret_ballista_shot", "turret_repeater_shot", "turret_piercer_shot",
        "turret_flame_shot", "turret_frost_shot", "turret_chain_shot",
        "turret_bombard_arc", "turret_bombard_impact", "turret_nullifier_shot",
        "turret_antiair_shot", "turret_beacon_pulse", "merc_ranger_shot",
        "merc_bastion_guard", "merc_striker_pressure", "merc_medic_pulse",
        "siege_structure_impact"):
        require(f"mesh renderer has {token}", token in mesh)
    require("defense mesh fades preserve packed ARGB channels",
            "private static int withAlpha(int color, int alpha)" in mesh
            and "(color & 0x00FFFFFF) | (clampInt(alpha) << 24)" in mesh
            and "color & 0xFFFFFF00" not in mesh
            and "rgba((color >> 24)" not in mesh)

    require("turret straight-line particle spam is replaced by mesh shot plus impact feedback",
            "VillageDefenseEffectSystem.turretShot" in turret
            and "for (int i = 0; i <= 8; i++)" not in turret)
    require("four mercenary roles expose synchronized combat feedback",
            "mercenaryGuardPulse" in merc
            and "mercenaryStrikerPressure" in merc
            and "mercenaryRangerShot" in merc
            and "mercenaryHealPulse" in merc)

    print("[PASS] v0.18.13 siege integration contracts complete")


if __name__ == "__main__":
    main()
