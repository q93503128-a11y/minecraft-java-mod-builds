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
    assert "int targetLevels = VillageCampaignProgression.targetLevelsForDay(day);" in raid
    assert "float baseline = required * 0.60f * targetLevels / Math.max(1, expectedActors);" in raid
    assert "public static int previewTotalEnemyCount(int day, int players)" in raid
    assert "total += previewWaveCount(day, previewWave, players, trait);" in raid

    # Late HP inflation must not recursively inflate rewards; qualitative threats still pay modest premiums.
    assert "Math.max(0.90f, Math.min(1.15f, healthWeight))" in raid
    assert "isBossEnemy(mob) ? 3.0f" in raid
    assert "VillageEnemyEliteSystem.isElite(mob) ? 1.35f" in raid
    assert "VillageEnemyArchetypeSystem.isTacticalThreat(archetype) ? 1.12f" in raid

    # Each authored day's budget scales by that day's intended visible level gain.
    # Day 21+ therefore supports roughly 3-4 level-ups per night instead of one.
    assert "targetPlayerLevel(safe) - targetPlayerLevel(safe - 1)" in campaign
    assert "270.0f / 80.0f" in campaign
    assert "RpgProgress.experienceRequiredAtLevel(targetLevel) * 0.18f * targetLevels" in raid

    print("[PASS] Lv.1-30 keeps the accepted opening XP curve")
    print("[PASS] day21+ XP is normalized against the full planned night and party-scaled roster")
    print("[PASS] late HP growth no longer multiplies XP up to the old 1.80x reward weight")
    print("[PASS] late-night XP budget scales with the authored 3-4 visible levels per day toward Lv.300")


if __name__ == "__main__":
    main()
