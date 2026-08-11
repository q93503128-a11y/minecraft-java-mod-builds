#!/usr/bin/env python3
"""Regression checks for modal visibility at common Minecraft GUI-scaled viewport sizes."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def safe_rect(width: int, height: int) -> tuple[int, int, int, int]:
    side = clamp(width // 52, 7, 16)
    top = clamp(height // 80, 6, 12)
    bottom_padding = clamp(height // 70, 7, 14)
    return side, top, width - side, height - bottom_padding


def town_facility_geometry(width: int, height: int) -> tuple[int, int, int]:
    left, top, right, bottom = safe_rect(width, height)
    safe_width = right - left
    safe_height = bottom - top
    # One-line compact header on short viewports, matching VillageCommandCenterScreen.
    header_bottom = top + 19 + 11 + 5
    role_title = header_bottom + 3
    role_top = role_title + 12
    role_height = 34 if safe_height < 245 else 42
    facility_title = role_top + role_height + 7
    facility_top = facility_title + 12
    facility_bottom = bottom - 15
    columns = 4 if safe_width >= 350 else 3
    rows = (7 + columns - 1) // columns
    available = max(rows * 30, facility_bottom - facility_top)
    row_height = max(30, min(70, available // rows))
    return columns, rows, row_height


def main() -> None:
    safe_source = (JAVA / "VillageUiSafeArea.java").read_text(encoding="utf-8")
    command_source = (JAVA / "VillageCommandCenterScreen.java").read_text(encoding="utf-8")
    quick_source = (JAVA / "VillageQuickChatSafeScreen.java").read_text(encoding="utf-8")
    suppressor = (JAVA / "VillageUiHudSuppressor.java").read_text(encoding="utf-8")

    for width, height in ((420, 224), (560, 299), (840, 448), (1680, 896)):
        left, top, right, bottom = safe_rect(width, height)
        assert left >= 0 and top >= 0 and right <= width and bottom <= height
        assert bottom - top >= int(height * 0.90), (width, height, top, bottom)
        columns, rows, row_height = town_facility_geometry(width, height)
        if right - left >= 350:
            assert columns == 4
            assert rows == 2
        assert row_height >= 30

    assert "bottomReserve" not in safe_source
    assert "townFacilityColumns" in command_source
    assert "count <= 8" in command_source
    assert "drawSignalLabel" in quick_source
    assert "VanillaGuiLayers.CHAT" in suppressor and "VanillaGuiLayers.HOTBAR" in suppressor

    print("[PASS] Modal safe area retains at least 90% of GUI-scaled viewport height")
    print("[PASS] Seven town facilities use two rows at normal desktop GUI widths")
    print("[PASS] Quick-chat text zones and vanilla HUD suppression remain wired")


if __name__ == "__main__":
    main()
