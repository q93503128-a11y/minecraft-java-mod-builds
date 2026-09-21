#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    wave = read("VillageWaveTrait.java")
    plan = read("VillageAttackPlanSystem.java")
    verify = (ROOT / "tools/verify_jar.py").read_text(encoding="utf-8")

    # Preserve the established opening campaign while giving days 20-99 authored chapter pools.
    assert "if (safeDay <= 19) return selectOpening(safeDay, safeWave);" in wave
    for pool in (
        "BLACK_WAVE", "SKY_INVASION", "NECRO_SIEGE", "IRON_ECLIPSE",
        "STORM_LEGION", "ABYSS_MARCH", "DOOM_OFFENSIVE", "FINAL_WAR",
    ):
        assert f"private static final VillageWaveTrait[] {pool}" in wave
    assert "return selectFromPool(pool, safeDay, safeWave);" in wave
    assert "int stride = coprimeStride(pool.length, day);" in wave
    assert "greatestCommonDivisor(stride, length) != 1" in wave

    # Every authored late-campaign night must expose a broad doctrine spread instead of
    # collapsing into one or two repeated traits through modular arithmetic resonance.
    def gcd(left: int, right: int) -> int:
        a, b = max(1, abs(left)), max(1, abs(right))
        while b:
            a, b = b, a % b
        return a

    def stride_for(length: int, day: int) -> int:
        stride = 2 + day % (length - 1)
        while gcd(stride, length) != 1:
            stride += 1
            if stride >= length:
                stride = 1
        return stride

    for day in range(20, 100):
        length = 7 if day >= 80 else 6
        offset = (day * 31 + day // 10 * 7) % length
        stride = stride_for(length, day)
        sequence = [(offset + (wave_index - 1) * stride) % length for wave_index in range(1, 8)]
        assert len(set(sequence)) >= 6, (day, sequence)

    # The hundredth day is a deliberate seven-wave climax rather than another pseudo-random roll.
    final_sequence = (
        "BREACH_STORM, SKY_SIEGE, HUNTER_NET, DEATH_CHORUS, "
        "IRON_TIDE, CATACLYSM, FINAL_HOST"
    )
    assert final_sequence in wave
    assert (
        "if (safeDay == VillageCampaignProgression.CAMPAIGN_END_DAY) "
        "return finalSiegeTrait(safeWave);"
    ) in wave
    assert "Math.min(FINAL_SIEGE.length - 1, wave - 1)" in wave

    # Scout/intel copy must communicate the real late-campaign doctrine, especially the finale.
    for text in (
        "파성 집중 + 저주·균열 혼성",
        "공중 공성 + 원거리 교란 + 파성 별동대",
        "사령 지원망 + 포탑 사냥 + 파성 압박",
        "중장갑 방진 + 파쇄 + 사령 지원 전열",
        "폭풍·공중 기동 + 다전선 사격",
        "균열·재앙 혼성 + 사령 지원",
        "재앙 혼성군 + 중장갑·공중·지원 동시 압박",
        "종말 군세 + 모든 후반 병과 혼성",
        "7단계 최종 대공성 · 파성→공중→교란→사령→철갑→재앙→종말 · 4~7웨이브 우두머리 연전",
    ):
        assert text in plan

    # JAR acceptance must protect the current content counts rather than the old 12/20 era.
    assert "EXPECTED_ENUM_TOKENS" in verify
    assert "Fourteen enemy archetypes and nineteen wave traits are bundled" in verify
    assert "Thirty-six equipment offers and sixty active role skills are bundled" in verify
    assert "twelve wave traits" not in verify
    assert "Twenty active skills" not in verify

    print("[PASS] days 1-19 retain their opening doctrine selector")
    print("[PASS] days 20-99 traverse at least six chapter doctrines before repeating")
    print("[PASS] day 100 escalates through seven named doctrines and ends on FINAL_HOST")
    print("[PASS] late-campaign scout copy and JAR content acceptance match the current 100-day game")


if __name__ == "__main__":
    main()
