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
required = {
    "META-INF/neoforge.mods.toml",
    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",
    "kr/moonseungjun/arcanecircle/ArcaneCircleClient.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneHud.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneRenderUtil.class",
    "kr/moonseungjun/arcanecircle/client/GrimoireScreen.class",
    "kr/moonseungjun/arcanecircle/item/ArcaneStaffItem.class",
    "kr/moonseungjun/arcanecircle/magic/MagicPlayerData.class",
    "kr/moonseungjun/arcanecircle/magic/SpellCastingService.class",
    "kr/moonseungjun/arcanecircle/network/ArcaneNetwork.class",
    "kr/moonseungjun/arcanecircle/network/QueueFusionPayload.class",
    "kr/moonseungjun/arcanecircle/network/CommitFusionPayload.class",
    "kr/moonseungjun/arcanecircle/registry/ModItems.class",
    "assets/arcanecircle/lang/ko_kr.json",
    "data/arcanecircle/spell_catalog/index.json",
}
for staff in staffs:
    required.update({
        f"assets/arcanecircle/items/{staff}.json",
        f"assets/arcanecircle/models/item/{staff}.json",
        f"assets/arcanecircle/textures/item/{staff}.png",
    })
for staff in staffs[1:]:
    required.add(f"data/arcanecircle/recipe/{staff}.json")

forbidden = {
    "kr/moonseungjun/arcanecircle/network/FuseSpellPayload.class",
    "pack.mcmeta",
    "staff-textures.json",
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
    if index.get("version") != "0.5.0-alpha.1":
        raise SystemExit("wrong catalog version in JAR")
    if index.get("cooldown_storage") != "persistent_world_saved_data":
        raise SystemExit("persistent cooldown contract missing from JAR")

    texture_hashes = set()
    for staff in staffs:
        raw = archive.read(f"assets/arcanecircle/textures/item/{staff}.png")
        if raw[:8] != b"\x89PNG\r\n\x1a\n":
            raise SystemExit(f"invalid PNG signature in JAR: {staff}")
        width, height = struct.unpack(">II", raw[16:24])
        if (width, height) != (32, 32):
            raise SystemExit(f"wrong staff texture dimensions in JAR: {staff}={width}x{height}")
        texture_hashes.add(hashlib.sha256(raw).hexdigest())
        item = json.loads(archive.read(f"assets/arcanecircle/items/{staff}.json"))
        model = json.loads(archive.read(f"assets/arcanecircle/models/item/{staff}.json"))
        if item.get("model", {}).get("model") != f"arcanecircle:item/{staff}":
            raise SystemExit(f"borrowed item model remains in JAR: {staff}")
        if model.get("textures", {}).get("layer0") != f"arcanecircle:item/{staff}":
            raise SystemExit(f"texture mapping mismatch in JAR: {staff}")
    if len(texture_hashes) != len(staffs):
        raise SystemExit("duplicate staff textures in JAR")

    for staff in staffs[1:]:
        recipe = json.loads(archive.read(f"data/arcanecircle/recipe/{staff}.json"))
        if recipe.get("result", {}).get("id") != f"arcanecircle:{staff}":
            raise SystemExit(f"wrong staff recipe result in JAR: {staff}")

digest = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name + ".sha256").write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
print(f"Arcane Circle v0.5 JAR verification: PASS ({len(names)} entries, 9 unique staff textures)")
print(f"SHA-256: {digest}")
