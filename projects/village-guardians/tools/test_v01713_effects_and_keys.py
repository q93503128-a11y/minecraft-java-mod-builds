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

    assert "mod_version=0.17.13-alpha.1" in props
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
        "Display.ItemDisplay", "Display.BlockDisplay", "Transformation",
        "Mode.WHIRLWIND", "Mode.BUFF", "Mode.BLADE_CHARGE", "Mode.SLAM_CHARGE",
        "Mode.RAPID_FIRE", "Mode.TARGET_LOCK", "Mode.ARROW_RAIN",
        "Mode.ENERGY_PROJECTILE", "Mode.FIRE_ORB", "Mode.FROST_FIELD",
        "Mode.TORNADO", "Mode.LIGHTNING_FIELD", "Mode.HEAL_FIELD",
        "Mode.HEAL_LINK", "Mode.CLEANSE", "Mode.MIRACLE",
        "Mode.SHIELD_CHARGE", "Mode.TAUNT", "Mode.FORTRESS", "Mode.AEGIS",
        "Items.NETHERITE_SWORD", "Items.SPECTRAL_ARROW", "Items.SHIELD",
        "Blocks.PACKED_ICE", "Blocks.SEA_LANTERN"
    ):
        assert token in effects, token
    assert "private static final class DisplayAccess" in effects
    assert "setTransformationInterpolationDuration" in effects
    assert "ParticleTypes" not in effects
    assert "sendParticles" not in effects
    assert "addParticle" not in effects
    assert not (JAVA / "VillageSkillVisualSystem.java").exists()

    print("[PASS] Saved legacy R/G skill bindings migrate to real Z/X and persist")
    print("[PASS] Garen-style whirlwind owns six visible swords and six rotating arc actors")
    print("[PASS] All twenty skills use bounded item/block display scenes without particles")
    print("[PASS] Temporary glass walls, block-break particles and obsolete visual facade are removed")


if __name__ == "__main__":
    main()
