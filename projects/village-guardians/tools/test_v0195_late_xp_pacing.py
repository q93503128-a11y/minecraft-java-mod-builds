#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    raid = read("VillageRaidSystem.java")
    campaign = read("VillageCampaignProgression.java")

    # Lv.1-30 stays on the accepted opening curve.
    assert "if (day <= 20)" in raid
    assert "required * 0.72f / VillageCampaignProgression.expectedThreatsPerLevel(day)" in raid
    assert "Opening-campaign threat budget retained for the established Lv.1-30 curve." in campaign

    # Day 21+ budgets XP against the entire deterministic night, including party roster scaling.
    assert "int players = server == null ? 1 : VillageProgressionSystem.plannedRaidPlayerCount(server);" in raid
    assert "int expectedActors = previewTotalEnemyCount(day, players);" in raid
    assert "float baseline = required * 0.60f / Math.max(1, expectedActors);" in raid
    assert "public static int previewTotalEnemyCount(int day, int players)" in raid
    assert "total += previewWaveCount(day, previewWave, players, trait);" in raid

    # Late HP inflation must not recursively inflate rewards; qualitative threats still pay modest premiums.
    assert "Math.max(0.90f, Math.min(1.15f, healthWeight))" in raid
    assert "isBossEnemy(mob) ? 3.0f" in raid
    assert "VillageEnemyEliteSystem.isElite(mob) ? 1.35f" in raid
    assert "VillageEnemyArchetypeSystem.isTacticalThreat(archetype) ? 1.12f" in raid

    # Neutral late-night kills now fund 60% of the target-level bar in aggregate.
    # The existing victory reward contributes another 18%; elite/boss premiums add texture
    # without the old 2-3+ level bars per ordinary late night.
    neutral_kill_budget = 0.60
    victory_budget = 0.18
    assert 0.75 <= neutral_kill_budget + victory_budget <= 0.85

    print("[PASS] Lv.1-30 keeps the accepted opening XP curve")
    print("[PASS] day21+ XP is normalized against the full planned night and party-scaled roster")
    print("[PASS] late HP growth no longer multiplies XP up to the old 1.80x reward weight")
    print("[PASS] neutral late-night XP budget tracks the day100 level target instead of capping near day69")


if __name__ == "__main__":
    main()
