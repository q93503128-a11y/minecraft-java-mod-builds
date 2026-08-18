#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEST = ROOT / "tools/test_runtime_safety.py"


def main() -> None:
    text = TEST.read_text(encoding="utf-8")
    old = '''    assert "minecraft.gui.screen() != null" in skill_hud
    assert "guiHeight() - 98" in skill_hud
    assert "bottomReserve" not in safe_area
'''
    new = '''    assert "minecraft.gui.screen() != null" in skill_hud
    assert "graphics.guiHeight() - 112" in skill_hud
    assert "abilityCard" in skill_hud
    assert "bottomReserve" not in safe_area
'''
    if text.count(old) != 1:
        raise SystemExit(f"expected one legacy HUD safe-area contract, got {text.count(old)}")
    TEST.write_text(text.replace(old, new, 1), encoding="utf-8")
    print("[PASS] HUD safe-area regression migrated from fixed text baseline to taller ability-card baseline")


if __name__ == "__main__":
    main()
