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
    test = ROOT / "tools/test_v0189_siege_phase2.py"
    replace_once(test,
        '    assert "VillageTowerResearchBonusSystem.tick" in guardians\n',
        '    assert "VillageTowerResearchBonusSystem" not in guardians\n')
    print("[PASS] v0.18.9 turret regression now rejects retired duplicate research firing")


if __name__ == "__main__":
    main()
