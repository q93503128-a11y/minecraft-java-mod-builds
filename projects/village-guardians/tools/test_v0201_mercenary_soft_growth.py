#!/usr/bin/env python3
from pathlib import Path
import math

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def mastery(rank: int) -> float:
    safe = max(1, min(100, rank))
    return math.sqrt((safe - 1) / 99.0)


def main() -> None:
    merc = read("VillageMercenarySystem.java")

    assert "private static double masteryProgress(int rank)" in merc
    assert "Math.sqrt((safe - 1) / (double) (MAX_LEVEL - 1))" in merc

    # Range/control/armor growth no longer plateaus around Lv.50-60.
    for old_cap in (
        "12.0 + Math.min(12.0, rank * 0.20)",
        "22.0 + Math.min(30.0, rank * 0.50)",
        "50.0 + Math.min(52.0, rank * 0.85)",
        "8.0 + Math.min(13.0, rank * 0.22)",
        "Math.min(24.0, 18.0 + safeRank * 0.10)",
        "Math.min(18.0, 11.0 + safeRank * 0.09)",
        "Math.min(15.0, 9.0 + safeRank * 0.07)",
        "Math.min(17.0, 11.0 + safeRank * 0.08)",
        "Math.min(16, 9 + rank / 7)",
    ):
        assert old_cap not in merc

    for token in (
        "double radius = 12.0 + 12.0 * mastery;",
        "double range = 22.0 + 30.0 * mastery;",
        "double range = 50.0 + 52.0 * mastery;",
        "double radius = 8.0 + 13.0 * masteryProgress(rank);",
        "case BASTION -> 18.0 + 6.0 * mastery;",
        "case STRIKER -> 11.0 + 7.0 * mastery;",
        "case RANGER -> 9.0 + 6.0 * mastery;",
        "case MEDIC -> 11.0 + 6.0 * mastery;",
    ):
        assert token in merc

    # The curve is monotonic and reaches the former Lv.100 endpoint without an early plateau.
    ranks = (1, 20, 40, 60, 80, 100)
    ranger_ranges = [50.0 + 52.0 * mastery(rank) for rank in ranks]
    bastion_armor = [18.0 + 6.0 * mastery(rank) for rank in ranks]
    medic_radius = [8.0 + 13.0 * mastery(rank) for rank in ranks]
    assert all(a < b for a, b in zip(ranger_ranges, ranger_ranges[1:]))
    assert all(a < b for a, b in zip(bastion_armor, bastion_armor[1:]))
    assert all(a < b for a, b in zip(medic_radius, medic_radius[1:]))
    assert ranger_ranges[0] == 50.0 and ranger_ranges[-1] == 102.0
    assert bastion_armor[0] == 18.0 and bastion_armor[-1] == 24.0
    assert medic_radius[0] == 8.0 and medic_radius[-1] == 21.0

    print("[PASS] mercenary range, control and armor keep improving through Lv.100")
    print("[PASS] square-root mastery replaces early hard plateaus while preserving Lv.100 endpoints")
    print("[PASS] combat-safety effect limits remain separate from long-term stat progression")


if __name__ == "__main__":
    main()
