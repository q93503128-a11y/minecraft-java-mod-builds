#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
spell = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java").read_text(encoding="utf-8")
client = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneClient.java").read_text(encoding="utf-8")
old_paths = [
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/block/MagicCircleBlock.java",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/gameplay/MagicCircleInteractionHandler.java",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneCircleWorldData.java",
    ROOT / "src/main/resources/assets/arcanecircle/blockstates/magic_circle.json",
]
for path in old_paths:
    if path.exists(): raise SystemExit(f"obsolete magic-circle implementation still exists: {path}")
for token in ["arcane_dart", "greater_ward", "fireball", "rift_step"]:
    if token not in spell: raise SystemExit(f"missing spell: {token}")
for token in ["InputConstants.KEY_C", "InputConstants.KEY_R", "InputConstants.KEY_Z", "InputConstants.KEY_X"]:
    if token not in client: raise SystemExit(f"missing hotkey: {token}")
catalog = json.loads((ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json").read_text(encoding="utf-8"))
if catalog["implemented_circles"] != [1, 2, 3] or catalog["future_max_circle"] != 9:
    raise SystemExit("circle contract mismatch")
print("Arcane Circle Ninefold static contract: PASS")
