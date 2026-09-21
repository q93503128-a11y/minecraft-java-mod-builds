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

    assert "boss ? bossForWaveSlot(day, wave, index)" in enemy
    assert "private static Archetype bossForWaveSlot(int day, int wave, int bossIndex)" in enemy
    assert "private static Archetype bossForSlot(int day, int bossIndex)" in enemy
    for archetype in ("SIEGE_BEAST", "IRON_WARLORD", "PLAGUE_ARCHON", "DREAD_KNIGHT"):
        assert f"Archetype.{archetype}" in enemy
    assert "first + Math.max(0, bossIndex)" in enemy
    assert "return bossForSlot(day, 0);" in enemy

    assert "VillageBossAspectSystem.previewText(day, wave, index)" in intel
    assert "VillageSiegeBossSystem.previewDoctrine(day, wave, archetype)" in intel
    assert "public static String previewDoctrine(" in boss
    assert 'doctrine.displayName() + " · " + doctrine.description()' in boss

    # Day 100 preserves four total boss encounters, but stages one unique boss on waves 4-7.
    assert "return wave >= 4 && wave <= 7 ? 1 : 0;" in war
    assert "case 4 -> Archetype.PLAGUE_ARCHON;" in enemy
    assert "case 5 -> Archetype.IRON_WARLORD;" in enemy
    assert "case 6 -> Archetype.SIEGE_BEAST;" in enemy
    assert "case 7 -> Archetype.DREAD_KNIGHT;" in enemy

    print("[PASS] single-boss days preserve the original day rotation through slot zero")
    print("[PASS] day100 stages the four boss archetypes across waves 4-7 instead of stacking clones")
    print("[PASS] daytime intel previews each boss archetype, aspect and authoritative boss doctrine")


if __name__ == "__main__":
    main()
