#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
import struct
import sys
import zipfile
from pathlib import Path

jar = Path(sys.argv[1])
if not jar.is_file():
    raise SystemExit(f"missing JAR: {jar}")

staffs = [
    "novice_staff", "ember_staff", "glacial_staff", "zephyr_staff", "aegis_staff",
    "verdant_staff", "rift_staff", "sage_staff", "archmage_staff",
]
spellbooks = [
    "mend", "blink", "stone_skin", "lightning_arc", "mana_lance",
    "greater_ward", "flame_wave", "ice_lance", "arcane_sight", "levitation",
    "meteor_shard", "blizzard_field", "thunder_prison", "mass_mend", "spatial_gate",
    "inferno_domain", "absolute_zero", "tempest_domain", "aegis_citadel", "arcane_annihilation",
]
required = {
    "META-INF/neoforge.mods.toml",
    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",
    "kr/moonseungjun/arcanecircle/ArcaneCircleClient.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneHud.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneRenderUtil.class",
    "kr/moonseungjun/arcanecircle/client/GrimoireScreen.class",
    "kr/moonseungjun/arcanecircle/item/ArcaneStaffItem.class",
    "kr/moonseungjun/arcanecircle/item/SpellbookItem.class",
    "kr/moonseungjun/arcanecircle/item/BeginnerGrimoireItem.class",
    "kr/moonseungjun/arcanecircle/magic/MagicPlayerData.class",
    "kr/moonseungjun/arcanecircle/magic/SpellCastingService.class",
    "kr/moonseungjun/arcanecircle/network/ArcaneNetwork.class",
    "kr/moonseungjun/arcanecircle/network/BeginCastPayload.class",
    "kr/moonseungjun/arcanecircle/network/ReleaseCastPayload.class",
    "kr/moonseungjun/arcanecircle/network/QueueFusionPayload.class",
    "kr/moonseungjun/arcanecircle/network/CommitFusionPayload.class",
    "kr/moonseungjun/arcanecircle/registry/ModItems.class",
    "assets/arcanecircle/lang/ko_kr.json",
    "assets/arcanecircle/items/beginner_grimoire.json",
    "data/arcanecircle/spell_catalog/index.json",
    "data/arcanecircle/villager_trade/librarian/level_1/beginner_grimoire.json",
}
for level in range(1, 6):
    required.add(f"data/minecraft/tags/villager_trade/librarian/level_{level}.json")
for staff in staffs:
    required.update({
        f"assets/arcanecircle/items/{staff}.json",
        f"assets/arcanecircle/models/item/{staff}.json",
        f"assets/arcanecircle/textures/item/{staff}.png",
    })
for staff in staffs[1:]:
    required.add(f"data/arcanecircle/recipe/{staff}.json")
for spell_id in spellbooks:
    item_id = f"spellbook_{spell_id}"
    required.update({
        f"assets/arcanecircle/items/{item_id}.json",
        f"data/arcanecircle/recipe/{item_id}.json",
    })

forbidden = {
    "kr/moonseungjun/arcanecircle/network/CastSpellPayload.class",
    "kr/moonseungjun/arcanecircle/network/FuseSpellPayload.class",
    "pack.mcmeta",
    "staff-textures.json",
    "spellbooks.json",
}

with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    name_set = set(names)
    missing = sorted(required - name_set)
    if missing:
        raise SystemExit(f"missing required entries: {missing}")
    remains = sorted(forbidden & name_set)
    if remains:
        raise SystemExit(f"obsolete/development runtime entries remain: {remains}")
    if len(names) != len(name_set):
        raise SystemExit("duplicate ZIP entries")
    if any(name.endswith(".java") or name.startswith(("tools/", ".github/")) for name in names):
        raise SystemExit("development files leaked into JAR")

    index = json.loads(archive.read("data/arcanecircle/spell_catalog/index.json"))
    if index.get("version") != "0.6.0-alpha.1":
        raise SystemExit("wrong catalog version in JAR")
    if index.get("implemented_circles") != [1, 2, 3, 4, 5]:
        raise SystemExit("five-circle catalog contract missing from JAR")
    if index.get("casting_mode") != "hold_number_show_sigil_release_to_cast":
        raise SystemExit("hold-release casting contract missing from JAR")
    if index.get("spellbooks") != 20:
        raise SystemExit("spellbook count mismatch in JAR")

    texture_hashes = set()
    for staff in staffs:
        raw = archive.read(f"assets/arcanecircle/textures/item/{staff}.png")
        if raw[:8] != b"\x89PNG\r\n\x1a\n":
            raise SystemExit(f"invalid PNG signature in JAR: {staff}")
        width, height = struct.unpack(">II", raw[16:24])
        if (width, height) != (32, 32):
            raise SystemExit(f"wrong staff texture dimensions in JAR: {staff}={width}x{height}")
        texture_hashes.add(hashlib.sha256(raw).hexdigest())
    if len(texture_hashes) != len(staffs):
        raise SystemExit("duplicate staff textures in JAR")

    for spell_id in spellbooks:
        item_id = f"spellbook_{spell_id}"
        recipe = json.loads(archive.read(f"data/arcanecircle/recipe/{item_id}.json"))
        if recipe.get("result", {}).get("id") != f"arcanecircle:{item_id}":
            raise SystemExit(f"wrong spellbook recipe result in JAR: {spell_id}")

    for level in range(1, 6):
        tag = json.loads(archive.read(f"data/minecraft/tags/villager_trade/librarian/level_{level}.json"))
        if not tag.get("values"):
            raise SystemExit(f"empty librarian trade tag for level {level}")

digest = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name + ".sha256").write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
print(f"Arcane Circle v0.6 JAR verification: PASS ({len(names)} entries, 20 spellbooks, 9 unique staff textures)")
print(f"SHA-256: {digest}")
