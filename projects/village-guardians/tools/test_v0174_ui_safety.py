from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    controller = read("VillageUiController.java")
    service = read("VillageUiService.java")
    local = read("VillageLocalActionSystem.java")
    town = read("VillageTownHallScreen.java")
    facility = read("VillageFacilityScreen.java")
    common_tree = read("VillageSkillTreeScreen.java")
    role_tree = read("VillageRoleProgressScreen.java")

    assert "mod_version=" in props
    assert "minecraft.gui.screen() != null" in keys
    assert "private static void drain(KeyMapping mapping)" in keys
    assert "minecraft.getConnection() == null" in keys

    skill_guard = controller.index('if (action.startsWith("skill_node:"))')
    role_guard = controller.index('if (action.startsWith("role_node:"))')
    assert "isNearSkillHall" in controller[skill_guard:skill_guard + 500]
    assert "isNearSkillHall" in controller[role_guard:role_guard + 600]
    assert "isNearTownHall(player) && !VillageLocationRules.isNear(player, building)" in controller
    assert "장비 합성은 대장간" in controller
    assert "장비 구매는 창고" in controller
    assert "용병 고용은 병영" in controller

    assert "Supplier<String> action" in service
    assert "action.get()" in service
    assert "() -> VillageProgressionSystem.learnNextSkill(player)" in service
    assert "자동 버프 건물" in local and "전투 훈련은 패시브" in local

    assert "font.width(normalized)" in town
    assert "font.width(normalized)" in facility
    assert "contentWidth < 260" in town
    assert "fitPopoverWidth(viewport.width(), 164, 246)" in common_tree
    assert "nodeX + nodeHalf < viewport.left()" in common_tree
    assert "fitPopoverWidth(view.width(), 176, 264)" in role_tree
    assert "horizontalPlacement" in role_tree
    assert "card.y() + card.size() < view.top()" in role_tree
    assert "below + bubbleHeight <= view.bottom() - 5" in role_tree

    print("Village Guardians v0.17.4 UI, location and shortcut contracts passed.")


if __name__ == "__main__":
    main()
