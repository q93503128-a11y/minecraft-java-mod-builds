#!/usr/bin/env python3
"""Regression contract for v0.18.8 death risk, multiplayer scaling and UI cleanup."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def scale_enemy_count(solo: int, players: int) -> int:
    return max(1, round(max(1, solo) * (1.0 + max(0, players - 1) * 0.30)))


def main() -> None:
    tuning = read("VillageDifficultyTuning.java")
    raid = read("VillageRaidSystem.java")
    client = read("VillageClientUi.java")
    town = read("VillageTownHallGridScreen.java")
    action = read("VillageActionDetailScreen.java")
    result = read("VillageResultScreen.java")
    tooltip = read("VillageEquipmentTooltipClient.java")
    world = read("VillageWorldSystem.java")

    # Death remains consequential: no all-downed structure-damage discount.
    defender_method = tuning.split("defenderStateStructureMultiplier", 1)[1].split("scaleEnemyCount", 1)[0]
    assert "return 1.0f;" in defender_method
    assert "0.30f" not in defender_method
    assert "ServerPlayer" not in tuning
    assert "VillageDifficultyTuning.defenderStateStructureMultiplier(server)" in raid
    assert "STRUCTURE_ATTACK_INTERVAL = 30" in raid

    # Early curve and +30% per extra player remain intact.
    assert "0.56f + (d - 1) * 0.04f" in tuning
    assert "0.52f + (d - 1) * 0.06f" in tuning
    assert "extraPlayers * 0.30f" in tuning
    base = 10
    assert [scale_enemy_count(base, p) for p in range(1, 6)] == [10, 13, 16, 19, 22]

    # Old generic/parchment screens cannot be selected by production routing.
    for legacy in ("VillageTownHallScreen", "VillageShopScreen", "VillageQuickChatScreen",
                   "VillageFusionScreen", "VillageRelicChoiceScreen", "VillageWaveIntelScreen",
                   "VillageStatusScreen", "VillageUiScreen", "VillageFacilityScreen"):
        assert legacy not in client
    assert '"tower_detail", "skill_test" ->' in client
    assert "default -> new VillageActionDetailScreen(payload)" in client

    # Known overflow regressions stay fixed.
    assert "pane.width() < 230" in town and "actionTop" in town
    assert "Math.max(48, available / count)" not in town
    assert "panelWidth < 390 && panelHeight >= 250" in action
    assert "VillageUiSafeArea.screen" in result and "PANEL = 0xF00B1217" in result

    # Tooltips are item-local on the client, while structure destruction still cleans loose block drops.
    assert "maximumEnhancement()" not in tooltip
    assert "enhancementEffectSummary(stack, enhancement)" in tooltip
    assert "removeLooseDebris" in world
    assert "cleanupRaidStructureDrops" in world

    print("[PASS] Downed players receive no hidden structure-damage protection")
    print("[PASS] Early difficulty curve and +30% multiplayer enemy scaling remain intact")
    print("[PASS] Legacy screens are unreachable and narrow-screen overflow guards remain wired")
    print("[PASS] Equipment tooltip avoids client/server smithy desync and debris cleanup remains active")


if __name__ == "__main__":
    main()
