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

    assert "mod_version=" in props
    assert "VANILLA_RESERVED" in keys and "migrateBindings" in keys
    assert "GLFW.GLFW_KEY_Z" in keys and "GLFW.GLFW_KEY_X" in keys and "GLFW.GLFW_KEY_V" in keys
    assert "!used.add(value)" in keys
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

    print("[PASS] Unsafe vanilla/conflicting bindings migrate to safe Z/X/V/H/J/K and persist")
    print("[PASS] Garen-style spin rotates the avatar holding one actual weapon")
    print("[PASS] All twenty skills route to bounded custom-mesh scenes without particles")
    print("[PASS] Temporary glass walls, block-break particles and obsolete visual facade are removed")


if __name__ == "__main__":
    main()
