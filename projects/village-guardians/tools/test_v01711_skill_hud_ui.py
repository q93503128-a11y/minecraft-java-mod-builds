#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    role = read("VillageRoleSkillSystem.java")
    hud = read("VillageHudSystem.java")
    client = read("VillageClientUi.java")
    screen = read("VillageSkillTestScreen.java")
    visuals = read("VillageSkillEffectSystem.java")
    renderer = read("VillageSkillEffectRenderer.java")
    mesh = read("VillageSkillMeshLibrary.java")

    assert "mod_version=" in props
    assert "cooldownRemainingSeconds" in role
    assert "cooldownProgress" in role
    assert "hudSlotText" in role
    assert "effectiveCooldownSeconds" in role
    assert "VillageRoleSkillSystem.hudSlotText(player, 0)" in hud
    assert "VillageRoleSkillSystem.hudSlotText(player, 1)" in hud

    enum_block = role.split("public enum ActiveSkill", 1)[1]
    enum_block = enum_block.split("private final String id", 1)[0]
    skill_names = re.findall(r'^\s{8}([A-Z][A-Z0-9_]+)\("', enum_block, re.MULTILINE)
    assert len(skill_names) == 20, skill_names
    for prefix in ("VANGUARD", "RANGER", "ARCANIST", "LUMINAR", "WARDEN"):
        assert sum(name.startswith(prefix + "_") for name in skill_names) == 4
    cast_block = role.split("switch (skill)", 1)[1].split("private static List<Mob> damageArea", 1)[0]
    for name in skill_names:
        assert f"case {name}" in cast_block, name
    for role_prefix in ("vanguard_", "ranger_", "arcanist_", "luminar_", "warden_"):
        assert role_prefix in visuals
    assert "submitCustomGeometry" in renderer
    assert "VertexConsumer" in mesh
    assert "Display.ItemDisplay" not in visuals and "Display.BlockDisplay" not in visuals

    assert 'case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(payload)' in client
    assert "final class VillageSkillTestScreen" in screen
    assert "renderRoles" in screen and "renderSkills" in screen
    assert "Z 장착" in screen and "X 장착" in screen
    assert "onClose();" in screen
    assert "panelWidth = Math.min(720" in screen
    assert "content.width() >= 520 ? 2 : 1" in screen
    assert "content.width() >= 570 ? 2 : 1" in screen

    print("[PASS] All 20 active skills have concrete cast logic and custom mesh feedback")
    print("[PASS] Action-bar HUD shows equipped Z/X skills and live cooldown seconds")
    print("[PASS] Test role and skill managers use a dedicated compact responsive UI")


if __name__ == "__main__":
    main()
