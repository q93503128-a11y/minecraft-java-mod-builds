#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    war = read("VillageWarfrontSystem.java")
    enemy = read("VillageEnemyArchetypeSystem.java")
    raid = read("VillageRaidSystem.java")
    intel = read("VillageWaveIntelSystem.java")
    plan = read("VillageAttackPlanSystem.java")

    # The day-100 climax keeps seven authored doctrines and exactly four boss encounters.
    assert "if (VillageCampaignProgression.isFinalSiege(day))" in war
    assert "return wave >= 4 && wave <= 7 ? 1 : 0;" in war
    assert "if (maxWaves < 7) return 0;" in war

    # Bosses are authored to the matching late phases instead of being four bodies in one wave.
    assert "case 4 -> Archetype.PLAGUE_ARCHON;" in enemy
    assert "case 5 -> Archetype.IRON_WARLORD;" in enemy
    assert "case 6 -> Archetype.SIEGE_BEAST;" in enemy
    assert "case 7 -> Archetype.DREAD_KNIGHT;" in enemy
    assert "boss ? bossForWaveSlot(day, wave, index)" in enemy

    # Final-siege phase names are visible both before the night and when each wave arrives.
    for label in (
        "제1전선 · 파성 돌입",
        "제2전선 · 천공 압박",
        "제3전선 · 사냥망 전개",
        "제4전선 · 사령 결전",
        "제5전선 · 철갑 결전",
        "제6전선 · 재앙 결전",
        "최종전 · 종말 군세",
    ):
        assert label in war
    assert "VillageWarfrontSystem.finalSiegePhaseLabel(day, wave)" in raid
    assert "VillageWarfrontSystem.finalSiegePhaseLabel(day, wave)" in intel
    assert '" · §4우두머리 " + bossCount + "명§f"' in raid
    assert '"결전 단계: " + finalPhase' in intel

    # Scout copy must state the actual late boss gauntlet instead of the old simultaneous pile.
    assert "4~7웨이브 우두머리 연전" in plan
    assert "4~7웨이브에서 서로 다른 우두머리가 차례로 출전합니다." in war
    assert "모든 전선과 우두머리 공세가 동시에 압박합니다." not in war

    print("[PASS] day100 preserves seven authored doctrines and four total boss encounters")
    print("[PASS] waves 4-7 stage plague, iron, siege and dread bosses as a readable gauntlet")
    print("[PASS] daytime intel and live wave announcements expose each final-siege phase")
    print("[PASS] final-siege copy no longer claims all bosses arrive simultaneously")


if __name__ == "__main__":
    main()
