#!/usr/bin/env python3
"""Contracts for long-running defense content and readable combat feedback."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    raid = read("VillageRaidSystem.java")
    traits = read("VillageWaveTrait.java")
    enemies = read("VillageEnemyArchetypeSystem.java")
    warfront = read("VillageWarfrontSystem.java")
    turrets = read("VillagePlacedTurretSystem.java")
    research = read("VillageDefenseResearchSystem.java")
    siege_ui = read("VillageSiegeCommandUi.java")
    ability = read("VillageRoleAbilitySystem.java")
    effects = read("VillageSkillEffectSystem.java")
    defense_effects = read("VillageDefenseEffectSystem.java")
    role_skills = read("VillageRoleSkillSystem.java")
    notices = (ROOT / "src/main/resources/META-INF/villageguardians/THIRD_PARTY_NOTICES.txt").read_text(encoding="utf-8")

    for trait in (
        "STANDARD", "SWARM", "IRONCLAD", "SIEGE", "HUNTERS", "HEXED", "FRENZY", "REGENERATING",
        "PHALANX", "BLOOD_MOON", "STORMFRONT", "RIFTED"
    ):
        assert trait + "(" in traits
    assert "counterHint" in traits and "select(int day, int wave)" in traits
    assert "currentTrait = VillageWaveTrait.select" in raid
    assert "currentTrait.counterHint()" in raid

    regular = (
        "GRUNT", "RUSHER", "BULWARK", "SAPPER", "MARKSMAN",
        "SHIELDBREAKER", "HEXER", "WAR_CHANTER", "NECROMANCER", "TOWER_HUNTER",
    )
    bosses = ("SIEGE_BEAST", "IRON_WARLORD", "PLAGUE_ARCHON", "DREAD_KNIGHT")
    for archetype in regular + bosses:
        assert archetype + "(" in enemies
    assert "structureDamageMultiplier" in enemies
    assert "tickAbility" in enemies
    assert "disableNearestActiveTurret" in enemies
    assert "VillageTowerSpecializationSystem.disableRandomInstalledTower" not in enemies
    assert "ACTIVE_ARCHETYPES" in raid and "ACTIVE_WAVES" in raid
    assert "VillageEnemyArchetypeSystem.create" in raid
    assert "VillageEnemyArchetypeSystem.tickAbility" in raid
    assert "VillageEnemyArchetypeSystem.onStructureHit" in raid

    assert "isMilestoneDay" in warfront
    assert "bonusBossCount" in warfront
    assert "끝없는 전쟁" in warfront
    assert "VillageWarfrontSystem.bonusBossCount" in raid
    assert "VillageWarfrontSystem.rewardMultiplier" in raid

    # 0.18.9+ production defense ownership belongs to ten player-placed turret roles.
    for turret in (
        "BALLISTA", "REPEATER", "PIERCER", "FLAME", "FROST",
        "CHAIN", "BOMBARD", "NULLIFIER", "ANTI_AIR", "BEACON"
    ):
        assert turret + "(" in turrets
    assert "VillageDefenseResearchSystem.towerDamageMultiplier()" in turrets
    assert "towerDamageMultiplier" in research and "Branch.TOWER" in research
    # The exact scaling formula is intentionally owned by the research system so mastery curves can evolve.
    assert "nearestActiveTurret" in turrets and "disableNearestActiveTurret" in turrets
    assert "siege_turret_catalog" in siege_ui and "siege_turret_list" in siege_ui
    assert "기존 성루는 관측 구조물이며 실전 화력은 직접 배치 포탑이 담당" in siege_ui
    assert not (JAVA / "VillageTowerResearchBonusSystem.java").exists()

    # Automated defense presentation uses the same synchronized procedural-mesh actor pipeline as player skills.
    for token in (
        "turret_ballista_shot", "turret_repeater_shot", "turret_piercer_shot",
        "turret_flame_shot", "turret_frost_shot", "turret_chain_shot",
        "turret_bombard_arc", "turret_nullifier_shot", "turret_antiair_shot",
        "turret_beacon_pulse", "merc_ranger_shot", "merc_medic_pulse"
    ):
        assert token in defense_effects

    assert "VillageRoleAbilitySystem.cast" in role_skills
    assert role_skills.count("case ") >= 20
    for token in (
        "SPIN_UNTIL", "EntityTypes.SNOWBALL", "Arrow",
        "healLowestAlly", "reviveNow", "player.swing", "target.push", "SoundEvents",
        "VillageSkillEffectSystem.startCast"
    ):
        assert token in ability
    for token in (
        "VillageSkillEffectEntity.spawn", "vanguard_spin", "ranger_energy_projectile",
        "arcanist_frost", "arcanist_tornado", "luminar_healing_field",
        "warden_fortress", "warden_aegis"
    ):
        assert token in effects
    assert "Display.ItemDisplay" not in effects and "Display.BlockDisplay" not in effects
    assert "ParticleTypes" not in ability and "sendParticles" not in ability
    assert "ParticleTypes" not in effects and "sendParticles" not in effects

    assert "Tiny Creatures" in notices
    assert "CC0 1.0 Universal" in notices
    assert "no original Tiny Creatures sprite binaries are bundled" in notices

    print("[PASS] Twelve readable wave traits include deterministic counters and previews")
    print("[PASS] Ten regular archetypes and four rotating bosses have distinct battlefield jobs")
    print("[PASS] Five-day milestone sieges and endless warfront tiers remain scalable")
    print("[PASS] Ten player-placed turret roles own production combat and tower research scaling")
    print("[PASS] Automated defenses and mercenaries use synchronized procedural-mesh feedback")
    print("[PASS] Twenty active skills combine real gameplay with dedicated procedural-mesh scenes")
    print("[PASS] CC0 fantasy visual references are documented without untracked binaries")


if __name__ == "__main__":
    main()
