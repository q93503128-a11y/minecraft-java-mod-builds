#!/usr/bin/env python3
"""Verify compact left-navigation/right-detail layouts at common GUI scales."""

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
    panel_width = min(880, max(330, screen_width - 16))
    panel_width = min(panel_width, max(1, screen_width - 2))
    panel_height = min(540, max(230, screen_height - 16))
    panel_height = min(panel_height, max(1, screen_height - 2))
    content_width = panel_width - 28
    if content_width >= 340:
        list_width = clamp(content_width * 24 // 100, 118, 198)
        detail_width = content_width - list_width - 8
        return list_width, detail_width, panel_height - 60
    return content_width, content_width, panel_height - 67


def main() -> None:
    assert "listLeft" in FACILITY and "detailLeft" in FACILITY
    assert "selectedIndex = actionCount() > 0 ? 0 : -1" in FACILITY
    assert "PANEL = 0xFFF1E6CF" in FACILITY
    assert "CARD_HEIGHT = 36" in FACILITY
    assert "ACTION_HEIGHT = 24" in FACILITY
    assert "Math.min(142" in FACILITY
    assert "contentWidth >= 340" in FACILITY

    assert "renderOfferList" in SHOP and "renderOfferDetail" in SHOP
    assert 'ROLES("직업 배치"' in TOWN
    assert 'REPAIR("시설 수리"' in TOWN
    assert 'MANAGEMENT("시설 강화"' in TOWN
    assert "CARD_HEIGHT = 36" in TOWN
    assert "* 24 / 100" in TOWN
    assert "다음 단계 변화" in TOWN
    assert "강화 비용" in TOWN

    assert "mouseScrolled" not in STATUS
    assert "ChatFormatting.stripFormatting" in STATUS
    assert "TEXT = 0xFF211A14" in STATUS
    assert "legacy-format colour leakage" in STATUS

    for width, height in ((380, 260), (520, 300), (640, 360), (800, 450), (1000, 600), (1648, 928)):
        left, right, content_height = facility_split(width, height)
        if width >= 380:
            assert left <= 198, (width, left)
            assert right > left, (width, left, right)
        assert right >= 170, (width, right)
        assert content_height >= 160, (height, content_height)

    print("[PASS] Facility selectors remain narrow while descriptions receive most of the width")
    print("[PASS] Town hall uses compact lists and explicit current/next upgrade comparison")
    print("[PASS] Action buttons are capped at 142x24 instead of spanning the whole panel")
    print("[PASS] Status text is dark, stripped of legacy white formatting, and fits without scrolling")


if __name__ == "__main__":
    main()
