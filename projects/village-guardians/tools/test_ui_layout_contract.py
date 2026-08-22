#!/usr/bin/env python3
"""Regression checks for hotbar-safe Village Guardians modal viewports."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def safe_rect(width: int, height: int) -> tuple[int, int, int, int]:
    side = clamp(width // 52, 7, 16)
    top = clamp(height // 80, 6, 12)
    bottom_padding = clamp(height // 11, 38, 56)
    return side, top, width - side, height - bottom_padding


def centered_panel(width: int, height: int, max_width: int, max_height: int) -> tuple[int, int, int, int]:
    left, top, right, bottom = safe_rect(width, height)
    safe_width = right - left
    safe_height = bottom - top
    panel_width = min(max_width, max(280, safe_width - 24))
    panel_height = min(max_height, max(210, safe_height - 16))
    panel_width = min(panel_width, safe_width)
    panel_height = min(panel_height, safe_height)
    x = left + safe_width // 2 - panel_width // 2
    y = top + safe_height // 2 - panel_height // 2
    return x, y, x + panel_width, y + panel_height


def main() -> None:
    safe_source = (JAVA / "VillageUiSafeArea.java").read_text(encoding="utf-8")
    town = (JAVA / "VillageTownHallGridScreen.java").read_text(encoding="utf-8")
    action = (JAVA / "VillageActionDetailScreen.java").read_text(encoding="utf-8")
    victory = (JAVA / "VillageVictoryScreen.java").read_text(encoding="utf-8")
    quick_source = (JAVA / "VillageQuickChatSafeScreen.java").read_text(encoding="utf-8")
    suppressor = (JAVA / "VillageUiHudSuppressor.java").read_text(encoding="utf-8")

    for width, height in ((420, 224), (560, 299), (840, 448), (1680, 896)):
        left, top, right, bottom = safe_rect(width, height)
        assert left >= 0 and top >= 0 and right <= width and bottom <= height
        reserve = height - bottom
        assert 38 <= reserve <= 56, (width, height, reserve)
        assert bottom > top + 100, (width, height, top, bottom)

        # Generic action screen remains centered; town hall intentionally consumes more of the safe viewport.
        x0, y0, x1, y1 = centered_panel(width, height, 760, 360)
        assert x0 >= left and y0 >= top
        assert x1 <= right and y1 <= bottom
        town_width = min(940, max(1, right - left))
        town_height = min(500, max(1, bottom - top))
        town_left = left + (right - left) // 2 - town_width // 2
        town_top = top + max(0, ((bottom - top) - town_height) // 2)
        assert town_left >= left and town_top >= top
        assert town_left + town_width <= right and town_top + town_height <= bottom

    assert "height / 11" in safe_source and "38, 56" in safe_source
    assert "panelWidth = Math.min(940" in town and "panelHeight = Math.min(500" in town
    assert "panelWidth * 31 / 100" in town and "gap = 10" in town
    assert "panelWidth = Math.min(760" in action and "panelHeight = Math.min(360" in action
    assert "VillageUiSafeArea.screen" in victory
    assert "drawSignalLabel" in quick_source
    assert "VanillaGuiLayers.CHAT" in suppressor and "VanillaGuiLayers.HOTBAR" in suppressor

    print("[PASS] Modal safe area reserves 38-56 GUI pixels for the vanilla hotbar")
    print("[PASS] Town hall and facility-detail panels remain fully inside the safe viewport")
    print("[PASS] Victory and quick-chat dedicated layouts remain wired")


if __name__ == "__main__":
    main()
