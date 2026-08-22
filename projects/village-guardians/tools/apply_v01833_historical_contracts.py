#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    if "mod_version=0.18.33-alpha.1" not in props:
        raise RuntimeError("historical contract migration must run after v0.18.33 source patch")

    replace_once(
        TOOLS / "test_v01818_growth_consumables.py",
        '    assert "groundY + 3" in fortress and "Math.floorMod(dx, 6) == 0" in fortress\n',
        '    assert "isFiringBayOffset" in fortress and "phase == 0 || phase == 1 || phase == 11" in fortress\n'
        '    assert "firingBay && y >= 3 && y <= 4" in fortress\n',
        "v0.18.18 wall firing-port historical contract",
    )

    print("[PATCH] v0.18.33 historical wall contract now accepts practical 3x2 firing bays")


if __name__ == "__main__":
    main()
