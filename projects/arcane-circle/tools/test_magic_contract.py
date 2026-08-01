#!/usr/bin/env python3
from __future__ import annotations
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
expected = {
    "FLAME_NOVA": ["FIRE_ESSENCE", "RING_FORM", "BURST_FUEL"],
    "FROST_SEAL": ["FROST_ESSENCE", "SEAL_FORM", "CALM_FUEL"],
    "ARCANE_PULSE": ["VOID_ESSENCE", "PULSE_FORM", "LIGHT_FUEL"],
}
source = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellRecipe.java").read_text("utf-8")
for spell, runes in expected.items():
    if spell not in source: raise SystemExit(f"missing spell: {spell}")
    for rune in runes:
        if rune not in source: raise SystemExit(f"{spell} missing rune {rune}")
for path in [
    ROOT / "src/main/resources/assets/arcanecircle/blockstates/magic_circle.json",
    ROOT / "src/main/resources/assets/arcanecircle/models/block/magic_circle.json",
    ROOT / "src/main/resources/assets/arcanecircle/items/magic_circle.json",
    ROOT / "src/main/resources/data/arcanecircle/recipe/magic_circle.json",
    ROOT / "src/main/resources/data/arcanecircle/loot_table/blocks/magic_circle.json",
    ROOT / "src/main/resources/pack.mcmeta",
]: json.loads(path.read_text("utf-8"))
texture = ROOT / "src/main/resources/assets/arcanecircle/textures/block/magic_circle.png"
if not texture.is_file() or texture.stat().st_size < 300: raise SystemExit("magic circle texture missing or unexpectedly small")
print("Arcane Circle static contract: PASS")
