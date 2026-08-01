#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
import sys
import zipfile
from pathlib import Path

jar = Path(sys.argv[1])
if not jar.is_file():
    raise SystemExit(f"missing JAR: {jar}")

required = {
    "META-INF/neoforge.mods.toml",
    "META-INF/THIRD_PARTY_NOTICES.md",
    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",
    "kr/moonseungjun/arcanecircle/magic/SpellCatalog.class",
    "kr/moonseungjun/arcanecircle/magic/SpellSigilService.class",
    "kr/moonseungjun/arcanecircle/magic/HighCircleSpellEffects.class",
    "kr/moonseungjun/arcanecircle/magic/SpellWorldLore.class",
    "kr/moonseungjun/arcanecircle/world/ArcaneWorldData.class",
    "kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.class",
    "kr/moonseungjun/arcanecircle/world/ArcaneAcademyBuilder.class",
    "kr/moonseungjun/arcanecircle/world/MagicWorldService.class",
    "kr/moonseungjun/arcanecircle/network/PurchaseAcademyItemPayload.class",
    "kr/moonseungjun/arcanecircle/network/ChooseTraditionPayload.class",
    "data/arcanecircle/spell_catalog/index.json",
    "assets/arcanecircle/items/spellbook_meteor_swarm.json",
    "assets/arcanecircle/items/spellbook_wish.json",
    "assets/arcanecircle/items/spellbook_gate.json",
}

with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    name_set = set(names)
    missing = sorted(required - name_set)
    if missing:
        raise SystemExit(f"missing required entries: {missing}")
    if len(names) != len(name_set):
        raise SystemExit("duplicate ZIP entries")
    forbidden = [
        name for name in names
        if name.startswith("data/arcanecircle/recipe/")
        or "villager_trade" in name
        or name.endswith(".java")
        or name.startswith(("tools/", ".github/"))
    ]
    if forbidden:
        raise SystemExit(f"forbidden survival/development entries: {forbidden[:8]}")
    index = json.loads(archive.read("data/arcanecircle/spell_catalog/index.json"))
    if index.get("implemented_circles") != list(range(1, 10)) or index.get("direct_spells") != 90:
        raise SystemExit("JAR catalogue is not the full 1-9 circle world")
    notice = archive.read("META-INF/THIRD_PARTY_NOTICES.md").decode("utf-8")
    if "Creative Commons Attribution 4.0" not in notice:
        raise SystemExit("SRD attribution missing from JAR")

digest = hashlib.sha256(jar.read_bytes()).hexdigest()
checksum = jar.with_name(jar.name + ".sha256")
checksum.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
print(f"Arcane Circle v0.8 JAR verification: PASS ({len(names)} entries)")
print(f"SHA-256: {digest}")
