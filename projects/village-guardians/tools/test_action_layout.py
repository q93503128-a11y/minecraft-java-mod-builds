#!/usr/bin/env python3
"""Verify the active compact UI layouts and reject retired runtime screen regressions."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOWN = (JAVA / "VillageTownHallGridScreen.java").read_text(encoding="utf-8")
ACTION = (JAVA / "VillageActionDetailScreen.java").read_text(encoding="utf-8")
SHOP = (JAVA / "VillageShopCatalogScreen.java").read_text(encoding="utf-8")
COMMAND = (JAVA / "VillageCommandCenterScreen.java").read_text(encoding="utf-8")
RESULT = (JAVA / "VillageResultScreen.java").read_text(encoding="utf-8")
CLIENT = (JAVA / "VillageClientUi.java").read_text(encoding="utf-8")
COMMON_TREE = (JAVA / "VillageSkillTreeScreen.java").read_text(encoding="utf-8")
ROLE_TREE = (JAVA / "VillageRoleProgressScreen.java").read_text(encoding="utf-8")

RETIRED_CLASSES = (
    "VillageTownHallScreen",
    "VillageShopScreen",
    "VillageQuickChatScreen",
    "VillageFusionScreen",
    "VillageRelicChoiceScreen",
    "VillageWaveIntelScreen",
    "VillageStatusScreen",
    "VillageUiScreen",
    "VillageFacilityScreen",
)


def clamp(value: int, minimum: int, maximum: int) -> int:
    return max(minimum, min(maximum, value))


def safe_rect(width: int, height: int) -> tuple[int, int, int, int]:
    side = clamp(width // 52, 7, 16)
    top = clamp(height // 80, 6, 12)
    bottom_padding = clamp(height // 11, 38, 56)
    return side, top, width - side, height - bottom_padding


def town_geometry(width: int, height: int) -> tuple[int, int, int]:
    safe_left, _, safe_right, _ = safe_rect(width, height)
    safe_width = safe_right - safe_left
    panel_width = min(820, max(300, safe_width - 12))
    panel_width = min(panel_width, safe_width)
    gap = 8
    content_width = max(1, panel_width - 28 - gap)
    list_width = clamp(panel_width * 27 // 100, 110, 205)
    list_width = min(list_width, max(90, content_width - 120))
    detail_width = panel_width - 28 - gap - list_width
    return panel_width, list_width, detail_width


def facility_button_widths(detail_width: int) -> list[int]:
    inner = max(1, detail_width - 28)
    if detail_width < 230:
        return [inner, inner, inner]
    gap = 5
    available = max(3, inner - gap * 2)
    base = max(1, available // 3)
    remainder = max(0, available - base * 3)
    return [base + (1 if remainder > 0 else 0),
            base + (1 if remainder > 1 else 0), base]


def action_geometry(width: int, height: int) -> tuple[int, int, bool]:
    safe_left, safe_top, safe_right, safe_bottom = safe_rect(width, height)
    safe_width = safe_right - safe_left
    safe_height = safe_bottom - safe_top
    panel_width = min(760, max(280, safe_width - 24))
    panel_height = min(360, max(210, safe_height - 16))
    panel_width = min(panel_width, safe_width)
    panel_height = min(panel_height, safe_height)
    available_height = max(1, panel_height - 63)
    stacked = panel_width < 390 and panel_height >= 250 and available_height >= 170
    if stacked:
        list_height = clamp(available_height * 38 // 100, 72, max(72, available_height - 94))
        detail_height = available_height - list_height - 7
        return panel_width, detail_height, True
    list_width = clamp(panel_width * 30 // 100, 105, 220)
    list_width = min(list_width, max(86, panel_width - 26 - 7 - 112))
    detail_width = panel_width - 26 - 7 - list_width
    return panel_width, detail_width, False


def shop_geometry(width: int, height: int) -> tuple[int, int, int, bool]:
    safe_left, safe_top, safe_right, safe_bottom = safe_rect(width, height)
    safe_width = safe_right - safe_left
    top = safe_top + 72
    bottom = safe_bottom - 18
    content_height = max(1, bottom - top)
    inner_width = max(1, safe_width - 14)
    gap = 8
    stacked = safe_width < 390 and content_height >= 190
    if stacked:
        list_height = clamp(content_height * 42 // 100, 78, max(78, content_height - 104))
        detail_height = content_height - list_height - gap
        return inner_width, inner_width, detail_height, True
    list_width = clamp(inner_width * 38 // 100, 105, 360)
    list_width = min(list_width, max(86, inner_width - gap - 120))
    detail_width = inner_width - gap - list_width
    return inner_width, list_width, detail_width, False


def main() -> None:
    # Retired classes may remain temporarily as inert migration history, but production routing must never open them.
    for retired in RETIRED_CLASSES:
        assert retired not in CLIENT, f"retired UI routed again: {retired}"
    assert not (JAVA / "VillageTownHallScreen.java").exists()
    assert not (JAVA / "VillageShopScreen.java").exists()

    assert 'FACILITIES("시설 관리")' in TOWN and 'ROLES("직업 배치")' in TOWN
    assert '"repair:" + f.id()' in TOWN and '"upgrade:" + f.id()' in TOWN
    assert "pane.width() < 230" in TOWN
    assert "actionTop" in TOWN and "enableScissor" in TOWN
    assert "Math.max(48, available / count)" not in TOWN

    for width, height in ((320, 180), (360, 202), (420, 224), (560, 299), (840, 448), (1680, 896)):
        _, _, detail = town_geometry(width, height)
        assert detail >= 120, (width, height, detail)
        button_widths = facility_button_widths(detail)
        assert all(value > 0 for value in button_widths)
        if detail >= 230:
            assert sum(button_widths) + 10 <= detail - 28, (width, detail, button_widths)
        else:
            assert max(button_widths) <= detail - 28, (width, detail, button_widths)

        _, action_detail, stacked = action_geometry(width, height)
        if stacked:
            assert action_detail >= 94, (width, height, action_detail)
        else:
            assert action_detail >= 112, (width, height, action_detail)

        _, shop_list, shop_detail, shop_stacked = shop_geometry(width, height)
        if shop_stacked:
            assert shop_detail >= 104, (width, height, shop_detail)
        else:
            assert shop_list >= 86, (width, height, shop_list)
            assert shop_detail >= 120, (width, height, shop_detail)

    assert "panelWidth < 390 && panelHeight >= 250" in ACTION
    assert "pane.width() - 28" in ACTION
    assert "safe.width() < 390 && contentHeight >= 190" in SHOP
    assert "actionTop" in SHOP and "actionButton(pane)" in SHOP
    for tab in ("ALL(\"전체\"", "EQUIPMENT(\"장비\"", "ARMOR(\"방어구\"",
                "CONSUMABLE(\"소모품\"", "SALE(\"판매\""):
        assert tab in SHOP
    assert "VillageConfirmScreen" in SHOP
    assert "PANEL = 0xF00B1217" in RESULT and "VillageUiSafeArea.screen" in RESULT
    assert "PANEL = 0xFFF1E9D7" not in RESULT
    assert "STATUS" in COMMAND and "FACILITY" in COMMAND

    assert "renderDetail" not in COMMON_TREE
    assert "Bubble" in COMMON_TREE
    assert "renderTreeFooter" not in ROLE_TREE
    assert "renderSkillFooter" not in ROLE_TREE
    assert "TreeBubble" in ROLE_TREE and "SkillBubble" in ROLE_TREE and "SkillGrid" in ROLE_TREE

    print("[PASS] Retired screens cannot re-enter production client routing")
    print("[PASS] Town-hall function/repair/upgrade buttons stay inside narrow detail panes")
    print("[PASS] Short narrow action screens choose side-by-side layout instead of crushed vertical panes")
    print("[PASS] Shop detail/list panes stay valid on short GUI heights and narrow widths")
    print("[PASS] Result modal and active shop/command surfaces use current safe-area UI language")
    print("[PASS] Growth trees retain anchored popovers without legacy footer panels")


if __name__ == "__main__":
    main()
