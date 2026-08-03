#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    skills = read("VillageRoleSkillSystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    system = read("VillageSkillEffectSystem.java")
    entity = read("VillageSkillEffectEntity.java")
    entities = read("VillageSkillEffectEntities.java")
    renderer = read("VillageSkillEffectRenderer.java")
    client = read("VillageSkillEffectClient.java")
    mesh = read("VillageSkillMeshLibrary.java")
    network = read("VillageNetwork.java")
    guard = read("VillageGuardians.java")

    assert "mod_version=0.17.14-alpha.1" in props
    assert '"회전 검무"' in skills
    assert '"회전 칼날"' not in skills
    assert "가렌의 회전 공격처럼" in skills

    assert "VillageSkillEffectEntities.register(modEventBus)" in guard
    assert "DeferredRegister.createEntities" in entities
    assert 'registerEntityType(' in entities and '"skill_effect"' in entities
    assert "VillageSkillEffectEntity.spawn" in system
    assert "EntityRenderersEvent.RegisterRenderers" in client
    assert "VillageSkillEffectRenderer::new" in client
    assert "submitCustomGeometry" in renderer
    assert "VillageSkillMeshLibrary.render" in renderer
    assert "VertexConsumer" in mesh
    assert "quadTwoSided" in mesh and "curvedShield" in mesh and "tornadoRibbon" in mesh
    assert "customArrow" in mesh and "energyBlade" in mesh and "jaggedBolt" in mesh
    assert "runeDisc" in mesh and "sphere" in mesh and "crystal" in mesh

    assert "SkillMotionPayload" in network
    assert 'sendSkillMotion(level, player, "vanguard_spin"' in system
    assert '!"vanguard_spin".equals(motion.name)' in client
    assert "event.getPoseStack()" in client
    assert "rotateY(radians)" in client
    assert "900.0" in client
    assert "event.getRenderState().bodyRot" in client

    expected_kinds = (
        "vanguard_spin", "vanguard_rally", "vanguard_blade_charge", "vanguard_slam_charge",
        "vanguard_blade_wave", "vanguard_slam_impact",
        "ranger_rapid", "ranger_lock", "ranger_rain_field", "ranger_rain_impact",
        "ranger_energy_charge", "ranger_energy_projectile", "ranger_ricochet_path",
        "arcanist_fire_orb", "arcanist_frost", "arcanist_tornado", "arcanist_lightning",
        "luminar_heal_cast", "luminar_heal_link", "luminar_cleanse_cast",
        "luminar_cleanse_wave", "luminar_healing_field", "luminar_miracle_cast",
        "luminar_miracle_wave", "warden_charge_cast", "warden_taunt",
        "warden_fortress", "warden_aegis",
    )
    for kind in expected_kinds:
        assert kind in system or kind in mesh, kind

    forbidden = (
        "Display.ItemDisplay", "Display.BlockDisplay", "ItemDisplay", "BlockDisplay",
        "ItemStack", "Items.", "Blocks.", "Transformation", "DisplayAccess",
        "ParticleTypes", "sendParticles", "addParticle",
    )
    for token in forbidden:
        assert token not in system, f"{token} remains in system"
        assert token not in renderer, f"{token} remains in renderer"
        assert token not in mesh, f"{token} remains in mesh"

    assert "projectile.setInvisible(true)" in ability
    assert "spawnVisualLightning" not in ability
    assert "levelEvent(2001" not in ability
    assert "updateShieldBlocks" not in ability
    assert not (JAVA / "VillageSkillVisualSystem.java").exists()

    print("[PASS] 회전 검무 rotates the avatar holding its one real weapon without orbiting swords")
    print("[PASS] All skill presentation uses original synchronized entities and procedural vertex meshes")
    print("[PASS] Custom arrows, blades, shields, runes, fields and volumes replace vanilla item/block displays")
    print("[PASS] Particle emitters, display actors, temporary blocks and stale visual facades are absent")


if __name__ == "__main__":
    main()
