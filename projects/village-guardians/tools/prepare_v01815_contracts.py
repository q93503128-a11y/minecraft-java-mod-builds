#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEST = ROOT / "tools/test_v01812_quality_audit.py"


def main() -> None:
    text = TEST.read_text(encoding="utf-8")
    old = '''    require("siege boss abilities telegraph before breach ritual and duel impacts",
            "phase == interval - 10" in boss
            and "phase == 100" in boss
            and "ticks % 105 == 70" in boss
            and "ParticleTypes.EXPLOSION" in boss)
'''
    new = '''    require("siege boss telegraphs and impacts share explicit delayed cast state",
            "BREACH_CASTS" in boss
            and "RITUAL_CASTS" in boss
            and "DUEL_CASTS" in boss
            and "BreachCast" in boss
            and "RitualCast" in boss
            and "DuelCast" in boss
            and "VillageBossEffectSystem.breachWarning" in boss
            and "VillageBossEffectSystem.ritualWarning" in boss
            and "VillageBossEffectSystem.duelMark" in boss)
'''
    if text.count(old) != 1:
        raise SystemExit(f"expected one old boss presentation contract, got {text.count(old)}")
    TEST.write_text(text.replace(old, new, 1), encoding="utf-8")
    print("[PASS] v0.18.12 boss telegraph regression migrated to fixed cast-state semantics")


if __name__ == "__main__":
    main()
