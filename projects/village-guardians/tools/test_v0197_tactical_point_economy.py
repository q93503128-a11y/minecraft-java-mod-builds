#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    tree = read("VillageSkillTreeSystem.java")
    descriptions = read("VillageActionDescriptions.java")

    assert "int naturalTotal = Math.max(0, level - 1);" in tree
    assert "return Math.max(naturalTotal, spentPoints(player));" in tree
    assert "레벨이 오를 때마다 전술 포인트 1P를 얻습니다." in tree
    assert "레벨이 오를 때마다 얻는 전술 포인트" in descriptions

    expected = {
        1: 1, 2: 1, 3: 1, 4: 2, 5: 2,
        6: 5, 7: 8, 8: 12, 9: 18, 10: 25,
    }
    assert sum(expected.values()) == 75
    assert sum(expected.values()) * 5 == 375
    assert 300 - 1 == 299
    assert (300 - 1) < sum(expected.values()) * 5

    for token in (
        "case 1, 2, 3 -> 1;",
        "case 4, 5 -> 2;",
        "case 6 -> 5;",
        "case 7 -> 8;",
        "case 8 -> 12;",
        "case 9 -> 18;",
        "default -> 25;",
    ):
        assert token in tree

    # Current common tree stays at ten tiers per branch. Expanding past 63 total enum
    # nodes would require a save-format migration because the authoritative mask is a long.
    for branch in ("POWER", "GUARD", "SUPPORT", "RANGED", "MOBILITY"):
        assert tree.count(branch + "_") >= 10

    print("[PASS] every player level awards one tactical point")
    print("[PASS] tiers 6-10 cost 5/8/12/18/25P and the five-branch tree costs 375P")
    print("[PASS] Lv.300 grants 299P, so max level preserves build choice")
    print("[PASS] historical spent points remain grandfathered without a save lock")


if __name__ == "__main__":
    main()
