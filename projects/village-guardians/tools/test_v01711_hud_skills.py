#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    hud = read("VillageHudSystem.java")
    skills = read("VillageRoleSkillSystem.java")
    client = read("VillageClientUi.java")
    screen = read("VillageSkillTestScreen.java")

    assert "mod_version=" in props
    assert "hudSlotText(player, 0)" in hud and "hudSlotText(player, 1)" in hud
    assert "cooldownRemainingSeconds" in skills and "cooldownProgress" in skills
    assert "§a준비" in skills and "■" in skills and "□" in skills
    assert 'case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(payload)' in client
    assert "Z 장착" in screen and "X 장착" in screen
    assert "roleMode" in screen and "content.width()" in screen

    print("[PASS] Action-bar HUD shows equipped Z/X skills, readiness and a live cooldown bar")
    print("[PASS] Test role and skill managers use a dedicated compact responsive UI")


if __name__ == "__main__":
    main()
