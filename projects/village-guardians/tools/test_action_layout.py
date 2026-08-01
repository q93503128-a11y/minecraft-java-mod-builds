#!/usr/bin/env python3
"""Verify the action menu never collapses under common Minecraft GUI scales."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCREEN = (ROOT / "src/main/java/kr/moonseungjun/villageguardians/VillageUiScreen.java").read_text(encoding="utf-8")
CARD_HEIGHT = 44


def clamp(value: int, minimum: int, maximum: int) -> int:
    return max(minimum, min(maximum, value))


def action_height(screen_height: int) -> tuple[int, int, int, int]:
    panel_height = min(760, max(210, screen_height - 8))
    panel_height = min(panel_height, max(1, screen_height - 2))
    content_height = panel_height - 58
    gap = 6
    body_height = clamp(content_height // 5, 38, 62)
    footer_height = clamp(content_height // 4, 50, 72)
    available = content_height - body_height - footer_height - gap * 2
    if available < CARD_HEIGHT + 12:
        missing = CARD_HEIGHT + 12 - available
        body_cut = min(max(0, body_height - 32), (missing + 1) // 2)
        body_height -= body_cut
        missing -= body_cut
        footer_height -= min(max(0, footer_height - 44), missing)
        available = content_height - body_height - footer_height - gap * 2
    return content_height, body_height, available, footer_height


def main() -> None:
    assert "selectedIndex = actionCount() > 0 ? 0 : -1" in SCREEN
    assert "actionHeight < CARD_HEIGHT + 12" in SCREEN
    assert "default -> new VillageUiScreen(payload)" not in SCREEN  # routing belongs to VillageClientUi

    # 200 logical pixels is already smaller than the supplied 1648x928 capture
    # at ordinary GUI scales. Every practical size must show at least one card.
    for height in (200, 210, 220, 240, 270, 300, 360, 480, 720):
        content, body, actions, footer = action_height(height)
        assert content > 0, (height, content)
        assert body >= 32, (height, body)
        assert footer >= 44, (height, footer)
        assert actions >= CARD_HEIGHT + 12, (height, actions)

    print("[PASS] Action viewport remains at least one full card high from 200px logical height")
    print("[PASS] Summary and detail panes shrink before action controls")


if __name__ == "__main__":
    main()
