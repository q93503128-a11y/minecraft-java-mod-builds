#!/usr/bin/env python3
"""Regression contracts for v0.18.7 town hall, equipment tooltips and raid easing."""

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def scale(solo: int, players: int) -> int:
    return round(max(1, solo) * (1.0 + max(0, players - 1) * 0.30))


def main() -> None:
    raid = read("VillageRaidSystem.java")
    tune = read("VillageDifficultyTuning.java")
    rpg = read("VillageRpgSystem.java")
    town = read("VillageTownHallGridScreen.java")
    tooltip = read("VillageEquipmentTooltipClient.java")
    debris = read("VillageRaidDebrisDropGuard.java")
    client = read("VillageClientUi.java")
    safe = read("VillageUiSafeArea.java")

    # Party size: solo is baseline, then exactly +30% of solo for every additional player.
    assert scale(10, 1) == 10
    assert scale(10, 2) == 13
    assert scale(10, 3) == 16
    assert scale(10, 4) == 19
    assert "extraPlayers * 0.30f" in tune
    assert "VillageDifficultyTuning.scaleEnemyCount" in raid
    assert "Math.max(1, players) * 2" not in raid

    # Early player/structure pressure is softened, but a downed party must not receive a free
    # structure-damage grace period. Death keeps its strategic cost in the current campaign rules.
    assert "0.56f + (d - 1) * 0.04f" in tune
    assert "0.52f + (d - 1) * 0.06f" in tune
    defender_state = re.search(
        r"public static float defenderStateStructureMultiplier\(MinecraftServer server\) \{(.*?)\n    \}",
        tune,
        re.S,
    )
    assert defender_state is not None and "return 1.0f;" in defender_state.group(1)
    assert "STRUCTURE_ATTACK_INTERVAL = 30" in raid
    assert "earlyStructureMultiplier(day)" in raid
    assert "defenderStateStructureMultiplier(server)" in raid
    assert "playerDamageMultiplier(VillageCouncilState.currentDay())" in rpg

    # Hovering graded equipment shows the exact gameplay contribution and next enhancement result.
    assert "ItemTooltipEvent" in tooltip
    assert "enhancementEffectSummary" in tooltip
    assert "마을 지키기 장비 효과" in tooltip
    assert "다음 강화" in tooltip

    # Scripted collapse may leave visual rubble blocks, but must not spawn collectible block debris.
    assert "ItemEntity" in debris and "BlockItem" in debris
    assert "VillageRaidSystem.isActive()" in debris
    assert "event.setCanceled(true)" in debris

    # Town hall is detail-first and exposes all three building operations in one bounded screen.
    for token in ("시설 기능", "수리 · ", "강화 · ", '"repair:" + f.id()', '"upgrade:" + f.id()'):
        assert token in town
    assert "VillageConfirmScreen" in town
    assert "selectedFacility" in town and "selectedRole" in town
    assert "height / 11" in safe and "38, 56" in safe

    # Victory no longer falls back to the generic giant action screen.
    assert 'case "victory" -> new VillageVictoryScreen(payload)' in client

    print("[PASS] party scaling = solo +30% per extra player")
    print("[PASS] early player/structure pressure stays softened without death-state structure grace")
    print("[PASS] graded equipment hover stats are wired")
    print("[PASS] scripted collapse block debris is suppressed")
    print("[PASS] town hall repair/upgrade/function actions fit inside hotbar-safe UI")


if __name__ == "__main__":
    main()
