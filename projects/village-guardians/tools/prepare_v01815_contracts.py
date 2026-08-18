#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one contract in {path.name}, got {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    old_test = ROOT / "tools/test_v01812_quality_audit.py"
    replace_once(old_test,
'''    require("siege boss abilities telegraph before breach ritual and duel impacts",
            "phase == interval - 10" in boss
            and "phase == 100" in boss
            and "ticks % 105 == 70" in boss
            and "ParticleTypes.EXPLOSION" in boss)
''',
'''    require("siege boss telegraphs and impacts share explicit delayed cast state",
            "BREACH_CASTS" in boss
            and "RITUAL_CASTS" in boss
            and "DUEL_CASTS" in boss
            and "BreachCast" in boss
            and "RitualCast" in boss
            and "DuelCast" in boss
            and "VillageBossEffectSystem.breachWarning" in boss
            and "VillageBossEffectSystem.ritualWarning" in boss
            and "VillageBossEffectSystem.duelMark" in boss)
''')

    boss_test = ROOT / "tools/test_v01815_boss_identity.py"
    replace_once(boss_test,
'''    for token in (
        "boss_presence_breach_colossus", "boss_presence_bone_hierophant", "boss_presence_black_marshal",
        "boss_phase_two_", "boss_phase_two_burst", "boss_breach_warning", "boss_breach_windup",
        "boss_breach_impact", "boss_ritual_warning", "boss_ritual_impact", "boss_duel_mark",
        "boss_duel_impact", "boss_bloodbound_warning", "boss_bloodbound_impact", "boss_storm_warning"):
        require(f"boss procedural presentation includes {token}", token in effects or token in mesh)
''',
'''    require("boss presence kinds are composed dynamically from doctrine ids",
            '"boss_presence_" + doctrine.name().toLowerCase' in effects
            and '"boss_phase_two_" + doctrine.name().toLowerCase' in effects
            and "renderBossPresence" in mesh
            and "breach_colossus" in mesh
            and "bone_hierophant" in mesh)
    for token in (
        "boss_phase_two_burst", "boss_breach_warning", "boss_breach_windup",
        "boss_breach_impact", "boss_ritual_warning", "boss_ritual_impact", "boss_duel_mark",
        "boss_duel_impact", "boss_bloodbound_warning", "boss_bloodbound_impact", "boss_storm_warning"):
        require(f"boss procedural presentation includes {token}", token in effects or token in mesh)
''')

    print("[PASS] v0.18.12 boss telegraph regression migrated to fixed cast-state semantics")
    print("[PASS] v0.18.15 dynamic boss presence contract matches runtime doctrine composition")


if __name__ == "__main__":
    main()
