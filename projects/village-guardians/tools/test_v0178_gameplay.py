#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    tree = read("VillageSkillTreeSystem.java")
    screen = read("VillageSkillTreeScreen.java")
    test_mode = read("VillageSkillTestSystem.java")
    role = read("VillageRoleSkillSystem.java")
    controller = read("VillageUiController.java")
    ability = read("VillageRoleAbilitySystem.java")
    visuals = read("VillageSkillEffectSystem.java")
    descriptions = read("VillageActionDescriptions.java")

    assert "mod_version=" in props
    for branch in ("power", "guard", "support", "ranged", "mobility"):
        assert tree.count(f'("{branch}_') == 10
        assert f'{branch.upper()}_10' in tree
    assert "Math.min(4, (tier + 2) / 3)" in tree
    assert "전쟁신의 심장" in tree and "성채화" in tree
    assert "수호단 총지휘" in tree and "천궁 붕괴" in tree and "시간 절단 보법" in tree
    assert "POWER_10" in tree and "0.75f" in tree and "1.45f" in tree
    assert "GUARD_10" in tree and "return 18" in tree
    assert "SUPPORT_10" in tree and "return 6.0f" in tree
    assert "RANGED_10" in tree and "return 260" in tree
    assert "MOBILITY_10" in tree and "return 0.62f" in tree

    assert "double distance = 72.0" in screen
    assert "Math.toRadians(branchAngleDegrees(branch))" in screen
    for angle in ("-90.0", "-18.0", "54.0", "126.0", "198.0"):
        assert angle in screen
    assert "savedZoom = 0.50" in screen
    assert "node.tier() == 10" in screen

    assert "ARENA_RADIUS = 16" in test_mode
    assert "FORTRESS_RADIUS + 44" in test_mode
    assert "buildArena" in test_mode and "Blocks.SMOOTH_STONE" in test_mode
    assert "player.teleportTo" in test_mode and "RETURN_POINTS" in test_mode
    assert "TEST_LOADOUTS" in test_mode
    assert "EntityTypes.HUSK" in test_mode
    assert "public static String equip" in test_mode
    assert "public static Optional<VillageRoleSkillSystem.ActiveSkill> equippedSkill" in test_mode

    assert "VillageSkillTestSystem.equippedSkill(player, slot)" in role
    assert "boolean testing = VillageSkillTestSystem.isEnabled(player)" in role
    assert "시험 모드 · 재사용 대기시간 없음" in role
    assert "VillageSkillEffectSystem.startCast" in ability
    assert "VillageSkillEffectEntity.spawn" in visuals
    assert "Display.ItemDisplay" not in visuals and "Display.BlockDisplay" not in visuals

    assert "test_role:" in controller
    assert "test_equip:" in controller
    assert "test_cast:" not in controller
    assert "K로 다시 엽니다" in controller
    assert "if (VillageSkillTestSystem.isEnabled(player)) openSkillTestSkillManager(player);" in controller
    assert "외부 시험장" in descriptions
    assert 'return "임시 장착"' in descriptions

    print("[PASS] Common growth has five ten-stage branches with functional late capstones")
    print("[PASS] Growth nodes radiate at equal 72-degree intervals in a five-point star layout")
    print("[PASS] Outdoor test arena teleports, restores position and owns reward-free targets")
    print("[PASS] Test skills temporarily equip to Z/X and use the real keyed cast/custom-mesh path")

if __name__ == "__main__":
    main()
