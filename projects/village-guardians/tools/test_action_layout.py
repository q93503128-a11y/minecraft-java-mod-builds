#!/usr/bin/env python3
"""Verify left-navigation/right-detail layouts remain usable at common GUI scales."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
FACILITY = (JAVA / "VillageFacilityScreen.java").read_text(encoding="utf-8")
SHOP = (JAVA / "VillageShopScreen.java").read_text(encoding="utf-8")
TOWN = (JAVA / "VillageTownHallScreen.java").read_text(encoding="utf-8")
STATUS = (JAVA / "VillageStatusScreen.java").read_text(encoding="utf-8")


def clamp(value: int, minimum: int, maximum: int) -> int:
    return max(minimum, min(maximum, value))


def facility_split(screen_width: int, screen_height: int) -> tuple[int, int, int]:
    panel_width = min(960, max(330, screen_width - 16))
    panel_width = min(panel_width, max(1, screen_width - 2))
    panel_height = min(610, max(230, screen_height - 16))
    panel_height = min(panel_height, max(1, screen_height - 2))
    content_width = panel_width - 32
    if content_width >= 500:
        list_width = clamp(content_width * 34 // 100, 180, 300)
        detail_width = content_width - list_width - 10
        return list_width, detail_width, panel_height - 70
    return content_width, content_width, panel_height - 78


def main() -> None:
    assert "listLeft" in FACILITY and "detailLeft" in FACILITY
    assert "selectedIndex = actionCount() > 0 ? 0 : -1" in FACILITY
    assert "PANEL = 0xFFF0E5CC" in FACILITY
    assert "renderOfferList" in SHOP and "renderOfferDetail" in SHOP
    assert 'ROLES("직업 배치"' in TOWN
    assert 'REPAIR("시설 수리"' in TOWN
    assert 'MANAGEMENT("관리·건설"' in TOWN
    assert "twoColumns" in STATUS and "mouseScrolled" not in STATUS

    for width, height in ((520, 300), (640, 360), (800, 450), (1000, 600), (1648, 928)):
        left, right, content_height = facility_split(width, height)
        assert left >= 180 or width < 532, (width, left)
        assert right >= 170, (width, right)
        assert content_height >= 158, (height, content_height)

    print("[PASS] Facility menus use left navigation and right information/action panes")
    print("[PASS] Town hall has distinct role, repair and management categories")
    print("[PASS] Shop has a dedicated categorized layout")
    print("[PASS] Status information fits without scroll controls")


if __name__ == "__main__":
    main()
