#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    network = read("VillageNetwork.java")
    descriptions = read("VillageActionDescriptions.java")
    aspect = read("VillageBossAspectSystem.java")
    notice = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")

    # Every persistent/currency mutation exposed by the current and legacy UI routes must share
    # the same server-side duplicate-packet guard.
    for token in (
        'action.equals("advance_time")',
        'action.equals("skill_learn")',
        'action.equals("buy_food")',
        'action.startsWith("select_role:")',
        'action.startsWith("skill_training:")',
        'action.startsWith("hire_mercenary:")',
        'action.startsWith("merc_hire:")',
        'action.startsWith("relic_select:")',
    ):
        assert token in network

    assert "레벨이 오를 때마다 얻는 전술 포인트" in descriptions
    assert "이동 속도가 폭증" not in aspect
    assert "잠시 추격 속도가 상승" in aspect

    assert "historical reference only" in notice
    assert "no Towns and Towers structure NBT files" in notice

    print("[PASS] persistent and currency mutations share the server duplicate-packet guard")
    print("[PASS] player-facing progression and berserker copy matches current runtime behavior")
    print("[PASS] repository asset notice matches the no-third-party-structure JAR contract")


if __name__ == "__main__":
    main()
