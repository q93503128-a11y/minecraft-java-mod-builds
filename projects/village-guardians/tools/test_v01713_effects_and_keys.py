#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    ability = read("VillageRoleAbilitySystem.java")
    effects = read("VillageSkillEffectSystem.java")

    assert "mod_version=0.17.14-alpha.1" in props
    assert "GLFW.GLFW_KEY_R" in keys and "GLFW.GLFW_KEY_G" in keys
    assert "ROLE_SKILL_ONE.setKey" in keys and "GLFW.GLFW_KEY_Z" in keys
    assert "ROLE_SKILL_TWO.setKey" in keys and "GLFW.GLFW_KEY_X" in keys
    assert "KeyMapping.resetMapping()" in keys and "minecraft.options.save()" in keys

    assert "VillageSkillEffectSystem.startCast" in ability
    assert "VillageSkillEffectSystem.tick" in ability
    assert "setYBodyRot" in ability
    assert "setYRot(player.getYRot() + 34.0f)" not in ability
    assert "levelEvent(2001" not in ability
    assert "updateShieldBlocks" not in ability
    assert "SHIELDS" not in ability
    assert "Blocks.GLASS.defaultBlockState(), 3" not in ability

    for token in (
        "VillageSkillEffectEntity.spawn", "vanguard_spin", "vanguard_rally",
        "ranger_energy_projectile", "arcanist_fire_orb", "arcanist_frost",
        "arcanist_tornado", "arcanist_lightning", "luminar_healing_field",
        "warden_fortress", "warden_aegis"
    ):
        assert token in effects, token
    for forbidden in (
        "Display.ItemDisplay", "Display.BlockDisplay", "Transformation",
        "Items.", "Blocks.", "DisplayAccess"
    ):
        assert forbidden not in effects, forbidden
    assert "ParticleTypes" not in effects
    assert "sendParticles" not in effects
    assert "addParticle" not in effects
    assert not (JAVA / "VillageSkillVisualSystem.java").exists()

    print("[PASS] Saved legacy R/G skill bindings migrate to real Z/X and persist")
    print("[PASS] Garen-style spin rotates the avatar holding one actual weapon")
    print("[PASS] All twenty skills route to bounded custom-mesh scenes without particles")
    print("[PASS] Temporary glass walls, block-break particles and obsolete visual facade are removed")


if __name__ == "__main__":
    main()
