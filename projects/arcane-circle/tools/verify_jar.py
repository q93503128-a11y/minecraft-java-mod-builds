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
    "kr/moonseungjun/arcanecircle/network/WorldMagicPayload.class",
    "kr/moonseungjun/arcanecircle/magic/WorldMagicService.class",
    "kr/moonseungjun/arcanecircle/client/WorldMagicTracker.class",
    "kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.class",
    "kr/moonseungjun/arcanecircle/client/GrimoireScreen.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneHud.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneRegaliaRenderer.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneCastingPerformance.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.class",
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
    retired = ['CodexVisualLanguage', 'ArcaneSigilDetailGrammar', 'LowCircleVisualIdentity', 'MidCircleVisualIdentity', 'FifthCircleVisualIdentity', 'SixthCircleVisualIdentity', 'ArchmageVisualIdentity', 'RangeReactivePresentation', 'SpellVisualSignature', 'CastingSilhouetteRenderer', 'RobeRegaliaRenderer']
    leaked = [n for n in names if any(n.endswith('/'+c+'.class') or ('/'+c+'$') in n for c in retired)]
    if leaked:
        raise SystemExit(f"retired presentation bytecode leaked: {sorted(leaked)}")
    index = json.loads(archive.read("data/arcanecircle/spell_catalog/index.json"))
    version = index.get("version")
    if not isinstance(version, str) or not version:
        raise SystemExit("catalog version missing")
    if jar.name != f"arcanecircle-{version}.jar":
        raise SystemExit(f"JAR/version mismatch: {jar.name} vs {version}")
    if index.get("implemented_circles") != list(range(1, 10)) or index.get("direct_spells") != 90:
        raise SystemExit("JAR catalogue is not the full 1-9 circle world")
    notice = archive.read("META-INF/THIRD_PARTY_NOTICES.md").decode("utf-8")
    if "Creative Commons Attribution 4.0" not in notice:
        raise SystemExit("SRD attribution missing from JAR")

digest = hashlib.sha256(jar.read_bytes()).hexdigest()
checksum = jar.with_name(jar.name + ".sha256")
checksum.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
print(f"Arcane Circle v0.12.1 JAR verification: PASS ({len(names)} entries)")
print(f"SHA-256: {digest}")
