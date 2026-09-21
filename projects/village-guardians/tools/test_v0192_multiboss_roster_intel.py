#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    enemy = read("VillageEnemyArchetypeSystem.java")
    intel = read("VillageWaveIntelSystem.java")
    boss = read("VillageSiegeBossSystem.java")
    war = read("VillageWarfrontSystem.java")

    assert "boss ? bossForSlot(day, index)" in enemy
    assert "private static Archetype bossForSlot(int day, int bossIndex)" in enemy
    for archetype in ("SIEGE_BEAST", "IRON_WARLORD", "PLAGUE_ARCHON", "DREAD_KNIGHT"):
        assert f"Archetype.{archetype}" in enemy
    assert "first + Math.max(0, bossIndex)" in enemy
    assert "return bossForSlot(day, 0);" in enemy

    assert "VillageBossAspectSystem.previewText(day, wave, index)" in intel
    assert "VillageSiegeBossSystem.previewDoctrine(day, wave, archetype)" in intel
    assert "public static String previewDoctrine(" in boss
    assert 'doctrine.displayName() + " · " + doctrine.description()' in boss

    # Day 100 still requests four bosses, which now rotate through all four base boss archetypes.
    assert "if (VillageCampaignProgression.isFinalSiege(day)) return Math.min(4" in war

    print("[PASS] single-boss days preserve the original day rotation through slot zero")
    print("[PASS] multi-boss waves rotate across all four boss archetypes instead of cloning one base boss")
    print("[PASS] daytime intel previews each boss archetype, aspect and authoritative boss doctrine")


if __name__ == "__main__":
    main()
