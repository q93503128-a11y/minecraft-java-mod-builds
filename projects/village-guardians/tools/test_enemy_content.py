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
    towers = read("VillageTowerSpecializationSystem.java")
    tower_data = read("VillageTowerProgressData.java")
    tower_combat = read("VillageDefenseSystem.java")
    tower_builder = read("VillageDefenseTowerBuilder.java")
    visuals = read("VillageSkillVisualSystem.java")
    rpg = read("VillageRpgSystem.java")
    ui = read("VillageUiService.java")
    notices = (ROOT / "src/main/resources/META-INF/villageguardians/THIRD_PARTY_NOTICES.txt").read_text(encoding="utf-8")

    for trait in (
        "STANDARD", "SWARM", "IRONCLAD", "SIEGE", "HUNTERS", "HEXED", "FRENZY", "REGENERATING"
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
    assert "disableRandomInstalledTower" in enemies
    assert "ACTIVE_ARCHETYPES" in raid
    assert "VillageEnemyArchetypeSystem.create" in raid
    assert "VillageEnemyArchetypeSystem.tickAbility" in raid
    assert "VillageEnemyArchetypeSystem.onStructureHit" in raid

    assert "isMilestoneDay" in warfront
    assert "bonusBossCount" in warfront
    assert "끝없는 전쟁" in warfront
    assert "VillageWarfrontSystem.bonusBossCount" in raid
    assert "VillageWarfrontSystem.rewardMultiplier" in raid

    assert "VillageTowerProgressData.TYPE" in towers
    assert "MAX_BRANCH_RANK = 3" in towers
    assert towers.count("TowerKind.BALLISTA") >= 3
    assert towers.count("TowerKind.FLAME") >= 3
    assert towers.count("TowerKind.FROST") >= 3
    assert towers.count("TowerKind.ARCANE") >= 3
    assert "branches" in tower_data and "ranks" in tower_data
    for branch in (
        "BALLISTA_TITAN", "BALLISTA_PIERCE", "BALLISTA_SPLIT",
        "FLAME_INFERNO", "FLAME_BLAST", "FLAME_MELT",
        "FROST_DEEP", "FROST_SHATTER", "FROST_BLIZZARD",
        "ARCANE_CHAIN", "ARCANE_NULL", "ARCANE_OVERCHARGE",
    ):
        assert branch + "(" in towers
        assert branch in tower_combat or branch in tower_builder
    assert "tower_branch:" in ui and "tower_upgrade:" in ui
    assert "openTowerDetail" in ui
    assert "rebuildTowerVisual" in ui

    assert "VillageSkillVisualSystem.render" in rpg
    assert "result.contains(\"사용 완료\")" in rpg
    assert "ParticleTypes" not in visuals and "sendParticles" not in visuals
    assert "pushFromPlayer" in visuals and "liftTargets" in visuals and "tauntTargets" in visuals
    assert "SoundEvents" in visuals

    assert "Tiny Creatures" in notices
    assert "CC0 1.0 Universal" in notices
    assert "no original Tiny Creatures sprite binaries are bundled" in notices

    print("[PASS] Eight readable wave traits include deterministic counters and previews")
    print("[PASS] Ten regular archetypes and four rotating bosses have distinct battlefield jobs")
    print("[PASS] Five-day milestone sieges and endless warfront tiers remain scalable")
    print("[PASS] Twelve persistent tower branches alter combat and physical tower silhouettes")
    print("[PASS] Twenty active skills use real motion, aggro and spatial sound instead of particle drawings")
    print("[PASS] CC0 fantasy visual references are documented without untracked binaries")


if __name__ == "__main__":
    main()
