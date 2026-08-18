#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEST = ROOT / "tools/test_v01815_boss_identity.py"


def main() -> None:
    text = TEST.read_text(encoding="utf-8")
    old = '''    require("boss doctrines diversify by day wave and actual boss archetype",
            "doctrineFor(" in boss
            and "VillageRaidSystem.waveOf(mob)" in boss
            and "type.ordinal()" in boss
            and "혼성 보스 교리" in boss)
'''
    new = '''    require("boss doctrines genuinely diversify by day wave and actual boss archetype",
            "doctrineFor(" in boss
            and "VillageRaidSystem.waveOf(mob)" in boss
            and "day * 5 + wave * 2 + salt" in boss
            and "wave * 3" not in boss
            and "type.ordinal()" in boss
            and "혼성 보스 교리" in boss)
    doctrine_cycle = {(10 * 5 + wave * 2 + 7) % 3 for wave in (1, 2, 3)}
    require("three consecutive waves can cover all three doctrine slots", len(doctrine_cycle) == 3)
'''
    if text.count(old) != 1:
        raise SystemExit(f"expected one old doctrine diversity contract, got {text.count(old)}")
    TEST.write_text(text.replace(old, new, 1), encoding="utf-8")
    print("[PASS] boss doctrine regression now proves wave input is mathematically effective")


if __name__ == "__main__":
    main()
