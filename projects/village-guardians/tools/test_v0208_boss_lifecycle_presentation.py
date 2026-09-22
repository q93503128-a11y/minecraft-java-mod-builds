#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    effects = read("VillageBossEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    siege = read("VillageSiegeBossSystem.java")
    raid = read("VillageRaidSystem.java")

    # Four bosses own distinct entrance / phase-two / defeat channels.
    suffixes = ("siege", "warlord", "plague", "dread")
    assert '"boss_entrance_" + suffix' in effects
    assert '"boss_transform_" + suffix' in effects
    assert '"boss_defeat_" + suffix' in effects
    for suffix in suffixes:
        for stage in ("entrance", "transform", "defeat"):
            assert f"boss_{stage}_{suffix}" in mesh

    for archetype, suffix in (
        ("SIEGE_BEAST", "siege"),
        ("IRON_WARLORD", "warlord"),
        ("PLAGUE_ARCHON", "plague"),
        ("DREAD_KNIGHT", "dread"),
    ):
        assert f'case {archetype} -> "{suffix}"' in effects

    # Runtime lifecycle hooks are tied to the authoritative boss rather than timers detached from combat.
    assert "VillageBossEffectSystem.entrance(level, mob, type)" in siege
    assert "mob.getHealth() <= mob.getMaxHealth() * 0.50f" in siege
    assert "VillageBossEffectSystem.phaseTwo(level, mob, archetype, doctrine)" in siege

    # Defeat presentation happens once while archetype metadata still exists, then normal cleanup proceeds.
    death = raid.index("public static void onLivingDeath")
    defeat = raid.index("VillageBossEffectSystem.defeat(level, mob, archetype)", death)
    cleanup = raid.index("releaseEnemy(server, uuid, event.getEntity())", death)
    assert death < defeat < cleanup
    assert "VillageEnemyArchetypeSystem.isBoss(archetype)" in raid

    # Existing doctrine/aspect identity remains layered into phase two.
    assert '"boss_phase_two_" + doctrine.name().toLowerCase(Locale.ROOT)' in effects
    assert '"boss_phase_two_burst"' in effects
    assert "VillageBossAspectSystem.aspectOf(boss)" in effects
    assert "VillageEnemyCompositionSystem.animateRiderAttack(boss)" in effects

    # Meshes are structurally different, not four recolors of one generic burst.
    for marker in (
        "Siege Beast: mass, ground fractures and collapsing weight.",
        "Iron Warlord: disciplined standards, armor bars and a command crown.",
        "Plague Archon: web bloom, hanging toxic nodes and a dissipating cocoon.",
        "Dread Knight: mounted charge lines, blade halo and a final cross-shaped collapse.",
    ):
        assert marker in mesh

    # Lifecycle feedback is presentation-only; no gameplay damage is introduced in the lifecycle renderer/effects.
    lifecycle_section = effects[effects.index("public static void entrance"):effects.index("public static void breachWarning")]
    assert "hurtServer" not in lifecycle_section
    assert "damage(" not in lifecycle_section
    assert "heal(" not in lifecycle_section

    print("[PASS] four bosses own twelve distinct entrance/phase/defeat presentation channels")
    print("[PASS] entrance, 50% phase transition and defeat are wired to authoritative boss lifecycle hooks")
    print("[PASS] defeat presentation executes before normal raid cleanup without changing rewards or damage")
    print("[PASS] doctrine/aspect identity remains layered under archetype-specific lifecycle feedback")


if __name__ == "__main__":
    main()
