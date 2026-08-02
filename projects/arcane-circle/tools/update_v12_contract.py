#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("test_magic_contract.py")
text = path.read_text(encoding="utf-8")
text = text.replace("0.11.0-alpha.1", "0.12.0-alpha.1")
text = text.replace("ninefold-arcana-11", "ninefold-arcana-12")
text = text.replace('"apply_v11_overhaul.py", "release-cast vector sigils"',
                    '"apply_v12_overhaul.py", "world geometry and charged fusion"')
old = '''need(hud, ["int slotSize = width >= 520 ? 36", "fitName", "chargingFraction",
    "drawCastingSigil", "partialRing"], "compact square spell HUD")
'''
new = '''need(hud, ["int slotSize = width >= 520 ? 25", "int gap = width >= 520 ? 6 : 5",
    "fitName", "tinyText", "chargingFraction", "fusionChargingFraction"],
    "compact separated spell HUD")
for obsolete_hud in ("drawCastingSigil", "partialRing"):
    if obsolete_hud in hud:
        raise SystemExit(f"screen-space casting circle remains: {obsolete_hud}")
'''
if old not in text:
    raise SystemExit("old v0.11 HUD contract block not found")
text = text.replace(old, new)
text = text.replace(
    'print("Arcane Circle v0.10 dense UI and release-cast vector sigils contract: PASS")',
    'print("Arcane Circle v0.12 nine-circle world geometry and charged fusion contract: PASS")')
path.write_text(text, encoding="utf-8")
print("v0.12 full magic-world contract updated")
