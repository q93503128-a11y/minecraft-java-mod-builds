#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one stale contract in {path.name}, got {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    runtime = ROOT / "tools/test_runtime_safety.py"
    replace_once(runtime,
        '    assert "VillageTowerResearchBonusSystem.tick" in guardians\n',
        '    assert "VillagePlacedTurretSystem.tick" in guardians\n'
        '    assert "VillageTowerResearchBonusSystem" not in guardians\n')
    print("[PASS] runtime safety now requires production placed-turret tick and rejects retired duplicate research firing")


if __name__ == "__main__":
    main()
