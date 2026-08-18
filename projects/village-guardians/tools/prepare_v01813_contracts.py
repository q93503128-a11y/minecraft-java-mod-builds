#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one legacy contract in {path.name}, got {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    test_0189 = ROOT / "tools/test_v0189_siege_phase2.py"
    replace_once(test_0189,
        '    assert "VillageTowerResearchBonusSystem.tick" in guardians\n',
        '    assert "VillageTowerResearchBonusSystem" not in guardians\n')

    test_01812 = ROOT / "tools/test_v01812_quality_audit.py"
    replace_once(test_01812,
        '            and "arcStart = mob.position().add" in turret)\n',
        '            and "Vec3 arcEnd = mob.position().add" in turret\n            and "VillageDefenseEffectSystem.turretShot(level, TurretType.CHAIN, arcStart, arcEnd)" in turret\n            and "arcStart = arcEnd" in turret)\n')

    print("[PASS] v0.18.9 turret regression now rejects retired duplicate research firing")
    print("[PASS] v0.18.12 chain regression now requires shared mesh/damage hop coordinates")


if __name__ == "__main__":
    main()
