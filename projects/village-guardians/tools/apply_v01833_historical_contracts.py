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

    replace_once(
        TOOLS / "test_v0187_balance_ui.py",
        '''    # Town hall is detail-first and exposes all three building operations in one bounded screen.\n    for token in ("시설 기능", "수리 · ", "강화 · ", '\"repair:\" + f.id()', '\"upgrade:\" + f.id()'):\n        assert token in town\n    assert "VillageConfirmScreen" in town\n    assert "selectedFacility" in town and "selectedRole" in town\n    assert "height / 11" in safe and "38, 56" in safe\n''',
        '''    # Town hall remains detail-first, but production ownership is maintenance-only.\n    buttons = town.split("private List<ButtonSpec> facilityButtons", 1)[1].split("private String functionAction", 1)[0]\n    assert '\"repair:\" + f.id()' in buttons and '\"upgrade:\" + f.id()' in buttons\n    assert "functionAction" not in buttons and "open_funding" not in buttons and "open_tower_control" not in buttons\n    assert "VillageConfirmScreen" in town\n    assert "selectedFacility" in town\n    render = town.split("public void extractRenderState", 1)[1].split("private void drawFrame", 1)[0]\n    assert "drawTabs(" not in render\n    assert "height / 11" in safe and "38, 56" in safe\n''',
        "v0.18.7 town-hall operation ownership contract",
    )

    replace_once(
        TOOLS / "test_v0187_balance_ui.py",
        '    print("[PASS] town hall repair/upgrade/function actions fit inside hotbar-safe UI")\n',
        '    print("[PASS] town hall repair/upgrade-only actions fit inside hotbar-safe UI")\n',
        "v0.18.7 town-hall pass label",
    )

    replace_once(
        TOOLS / "test_v0188_risk_ui_cleanup.py",
        '    assert "pane.width() < 230" in town and "actionTop" in town\n',
        '    assert "pane.width() < 260" in town and "actionTop" in town\n'
        '    assert "available / 2" in town and "gap = 7" in town\n',
        "v0.18.8 responsive town-hall width contract",
    )

    print("[PATCH] v0.18.33 historical contracts accept practical firing bays and maintenance-only responsive town hall")


if __name__ == "__main__":
    main()
