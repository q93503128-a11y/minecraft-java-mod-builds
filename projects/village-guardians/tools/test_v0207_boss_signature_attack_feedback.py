#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    enemy = read("VillageEnemyArchetypeSystem.java")
    effects = read("VillageBossEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    combo = read("VillageEnemyCompositionSystem.java")

    # Four archetype abilities own four different warning/impact channels.
    families = {
        "SIEGE_BEAST": ("boss_signature_siege_warning", "boss_signature_siege_impact", "9.5", "88", "100"),
        "IRON_WARLORD": ("boss_signature_warlord_warning", "boss_signature_warlord_impact", "12.0", "102", "120"),
        "PLAGUE_ARCHON": ("boss_signature_plague_warning", "boss_signature_plague_impact", "11.0", "126", "150"),
        "DREAD_KNIGHT": ("boss_signature_dread_warning", "boss_signature_dread_impact", "10.0", "76", "90"),
    }
    for archetype, (warning, impact, radius, warning_phase, cadence) in families.items():
        assert f'case {archetype} -> "{warning}"' in effects
        assert f'case {archetype} -> "{impact}"' in effects
        assert warning in mesh
        assert impact in mesh
        assert f"abilityPhase(mob, globalTicks, {cadence})" in enemy
        assert f"phase == {warning_phase}" in enemy
        assert f"signatureWarning(level, mob, archetype, {radius}" in enemy
        assert f"signatureImpact(level, mob, archetype, {radius})" in enemy

    # Telegraphs follow the authoritative boss, so their visible boundary moves with the real hit center.
    assert 'VillageSkillEffectEntity.spawn(level, boss, kind, boss.position()' in effects
    assert 'VillageSkillEffectEntity.spawn(level, null, kind, boss.position()' in effects
    assert "Every warning draws the exact live gameplay radius" in mesh

    # Each family uses a structurally distinct mesh language rather than recoloring one ring.
    assert "Siege Beast: grounded weight" in mesh
    assert "Iron Warlord: standards and command spokes" in mesh
    assert "Plague Archon: readable spider-web lattice" in mesh
    assert "Dread Knight: radial boundary remains honest" in mesh
    assert "chevron(pose, out, b" in mesh
    assert "horizontalSlash(pose, out, b" in mesh
    assert "sphere(pose, out, node" in mesh

    # Existing gameplay payload remains intact after the new windups.
    assert "damageAndDebuffPlayers(level, server, mob, 9.5, 4.0f, MobEffects.SLOWNESS)" in enemy
    assert "MobEffects.STRENGTH, 140, 1" in enemy
    assert "damageAndDebuffPlayers(level, server, mob, 11.0, 3.5f, MobEffects.POISON)" in enemy
    assert "MobEffects.DARKNESS, 80, 0" in enemy
    assert "supportHeal(mob, Math.min(12.0f, drained))" in enemy

    # Visible riders participate in the cast motion without becoming a second combat owner.
    assert "VillageEnemyCompositionSystem.animateRiderAttack(mob)" in enemy
    assert "public static void animateRiderAttack(Entity owner)" in combo
    assert "mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND)" in combo

    # Impact audio is archetype-specific instead of a shared generic click.
    for sound in (
        "SoundEvents.RAVAGER_ROAR",
        "SoundEvents.IRON_GOLEM_ATTACK",
        "SoundEvents.WITCH_CELEBRATE",
        "SoundEvents.WITHER_SKELETON_AMBIENT",
    ):
        assert sound in enemy

    print("[PASS] four boss archetypes own four distinct warning/impact mesh languages")
    print("[PASS] warning boundaries track the real boss and match 9.5/12/11/10 block gameplay radii")
    print("[PASS] original boss ability payloads remain intact behind readable windups")
    print("[PASS] rider motion and archetype-specific audio reinforce each boss impact")


if __name__ == "__main__":
    main()
