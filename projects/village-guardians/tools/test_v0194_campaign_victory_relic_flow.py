#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    raid = read("VillageRaidSystem.java")
    service = read("VillageUiService.java")
    ui = read("VillageUiController.java")
    descriptions = read("VillageActionDescriptions.java")

    assert "boolean finalCampaignVictory = VillageCampaignProgression.isFinalSiege(day);" in raid
    assert "VillageUiService.openCampaignVictoryForAll(server);" in raid
    assert "VillageUiService.openRepairSummaryForAll(server);" in raid
    assert "VillageRelicSystem.openPendingChoicesForParty(server);" in raid

    assert "public static void openCampaignVictoryForAll(MinecraftServer server)" in service
    assert "제100일 최종 대공성을 막아냈습니다." in service
    assert "정식 100일 캠페인을 완주했습니다." in service
    assert "제101일부터는 끝없는 전쟁입니다. 계속할 때만" in service
    assert 'List.of("open_relic_collection", "open_quick_chat")' in service

    assert "VillageRelicSystem.hasPendingChoice(player)" in ui
    assert "VillageRelicSystem.openChoice(player)" in ui
    assert "선택 대기 중인 보상이 있습니다." in ui
    assert 'case "open_relic_collection"' in descriptions
    assert "선택 대기 중인 보스 유물이 있으면 선택을 이어가고" in descriptions

    print("[PASS] day100 victory is presented as campaign completion before optional endless war")
    print("[PASS] ordinary raid victories retain their existing repair-summary and automatic relic flow")
    print("[PASS] closed pending relic choices remain recoverable from the status/relic entry point")


if __name__ == "__main__":
    main()
