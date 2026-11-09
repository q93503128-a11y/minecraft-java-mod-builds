#!/usr/bin/env python3
"""Hash-pinned, reversible AUTO-ROOT isolation for a STAGED COPY of Drehmal 2.2.2f.

Not a full terrain-only conversion. Does not rewrite scheduled functions, command
blocks, advancements or story triggers; structural mechanisms may stop working.
No final world or external copyrighted assets are published.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

OFFICIAL_LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
TAGS = {
    "data/minecraft/tags/functions/load.json":
        ("4b2789058085092121ea797e9bb70e403d09bb71b8b157c5ee41984d9e6c6ff6", ["core:init"]),
    "data/minecraft/tags/functions/tick.json":
        ("1ae2707f97b3a97d1244dcb0d2ce315250a2c39b7ccf7ce08d5b2e02895ddd09", ["core:main", "entities:main"]),
}
PACK_NAME = "hi_drehmal.zip"
BACKUP_DIR = "turnbound-preview-backup"
RECEIPT_FILE = "turnbound-auto-root-preview.json"

def hash_bytes(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()

def file_hash(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        while block := stream.read(4 * 1024 * 1024):
            h.update(block)
    return h.hexdigest()

def check_staged_world(world: Path, expected_level: str):
    world = world.resolve(strict=True)
    if not (world / "level.dat").is_file() or (world / "level.dat").is_symlink():
        raise ValueError("Not a regular extracted Minecraft world")
    level = file_hash(world / "level.dat")
    if level != expected_level:
        raise ValueError("level.dat does not match pinned original release")
    marker = world / "turnbound-stage.json"
    if not marker.is_file() or marker.is_symlink():
        raise ValueError("World must be staged with drehmal_pack_boundary.py stage")
    m = json.loads(marker.read_text(encoding="utf-8"))
    if m.get("schema") != 1 or m.get("source_level_sha256") != level:
        raise ValueError("Staging receipt does not match original map")
    if (world / "turnbound-world.marker").exists():
        raise ValueError("Refusing to alter activated world; this is offline staging only")
    pack = world / "datapacks" / PACK_NAME
    if not pack.is_file() or pack.is_symlink():
        raise ValueError("Expected original Drehmal datapack ZIP absent")
    return world, pack

def verify_source(pack: Path, expected_tags: dict) -> dict:
    with zipfile.ZipFile(pack) as z:
        names = z.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Datapack contains ambiguous duplicate ZIP members")
        existing = {}
        for name, (expected_sha, expected_values) in expected_tags.items():
            raw = z.read(name)
            if hash_bytes(raw) != expected_sha:
                raise ValueError(f"Original function tag hash does not match: {name}")
            tag = json.loads(raw)
            if not isinstance(tag, dict) or tag.get("values") != expected_values or tag.get("replace", False):
                raise ValueError(f"Unexpected original entrypoints: {name}")
            existing[name] = raw
        return existing

def apply(world: Path, expected_level: str = OFFICIAL_LEVEL_SHA, expected_tags: dict = TAGS):
    world, pack = check_staged_world(world, expected_level)
    receipt = world / RECEIPT_FILE
    backup_dir = world / BACKUP_DIR
    if receipt.exists() or backup_dir.exists():
        raise ValueError("Preview already exists or has unfinished backup; refusing to overwrite")
    verify_source(pack, expected_tags)
    old_hash = file_hash(pack)
    backup_dir.mkdir()
    backup = backup_dir / (PACK_NAME + ".original")
    shutil.copy2(pack, backup)
    if file_hash(backup) != old_hash:
        raise ValueError("Original pack backup verification failed")
    tmp = pack.with_name(PACK_NAME + ".turnbound-new")
    try:
        with zipfile.ZipFile(pack) as source, zipfile.ZipFile(tmp, "w") as target:
            target.comment = source.comment
            for entry in source.infolist():
                data = source.read(entry)
                if entry.filename in expected_tags:
                    original = json.loads(data)
                    original["values"] = []
                    data = (json.dumps(original, separators=(",", ":")) + "\n").encode("utf-8")
                target.writestr(entry, data)
        with zipfile.ZipFile(pack) as source, zipfile.ZipFile(tmp) as result:
            if source.namelist() != result.namelist():
                raise ValueError("ZIP entries unexpectedly changed")
            for name in source.namelist():
                orig, changed = source.read(name), result.read(name)
                if name in expected_tags:
                    if json.loads(changed).get("values") != []:
                        raise ValueError("Automatic entrypoint not disabled")
                elif orig != changed:
                    raise ValueError("Unrelated original map asset was modified")
        patched_hash = file_hash(tmp)
        record = {
            "schema": 1,
            "status": "OFFLINE_PREVIEW_ONLY",
            "original_pack_sha256": old_hash,
            "patched_pack_sha256": patched_hash,
            "pinned_level_dat_sha256": expected_level,
            "disabled_auto_entrypoints": {
                "minecraft:load": ["core:init"],
                "minecraft:tick": ["core:main", "entities:main"]
            },
            "other_functions_and_command_blocks_untouched": True,
            "not_a_playtested_map": True,
            "warning": "Direct command-block calls, scheduled functions, advancements and broken doors remain unverified."
        }
        # Never publish the working copy itself; only metadata can be exported.
        os.replace(tmp, pack)
        receipt.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return record
    finally:
        tmp.unlink(missing_ok=True)

def restore(world: Path, expected_level: str = OFFICIAL_LEVEL_SHA):
    world, pack = check_staged_world(world, expected_level)
    receipt = world / RECEIPT_FILE
    backup = world / BACKUP_DIR / (PACK_NAME + ".original")
    if not receipt.is_file() or not backup.is_file():
        raise ValueError("No verified isolated-world backup to restore")
    record = json.loads(receipt.read_text(encoding="utf-8"))
    if record.get("schema") != 1 or record.get("status") != "OFFLINE_PREVIEW_ONLY":
        raise ValueError("Unexpected restoration receipt")
    if file_hash(pack) != record["patched_pack_sha256"]:
        raise ValueError("Working pack changed after preview; refusal prevents data loss")
    if file_hash(backup) != record["original_pack_sha256"]:
        raise ValueError("Original backup hash mismatch")
    tmp = pack.with_name(PACK_NAME + ".turnbound-restore")
    try:
        shutil.copy2(backup, tmp)
        if file_hash(tmp) != record["original_pack_sha256"]:
            raise ValueError("Restoration copy verification failed")
        os.replace(tmp, pack)
        receipt.unlink()
        backup.unlink()
        (world / BACKUP_DIR).rmdir()
        return {"status": "RESTORED", "pack_sha256": file_hash(pack)}
    finally:
        tmp.unlink(missing_ok=True)

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("action", choices=("inspect", "apply", "restore"))
    p.add_argument("--world-copy", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args()
    world = a.world_copy.resolve(strict=True)
    out = a.output.resolve()
    if world == out or world in out.parents:
        p.error("Report path must be outside the map copy")
    if a.action == "inspect":
        _, pack = check_staged_world(world, OFFICIAL_LEVEL_SHA)
        matches = verify_source(pack, TAGS)
        result = {"status": "READY_FOR_OFFLINE_PREVIEW", "tag_paths": list(matches),
                  "pack_sha256": file_hash(pack), "not_a_full_gameplay_separation": True}
    elif a.action == "apply":
        result = apply(world)
    else:
        result = restore(world)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))

if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, zipfile.BadZipFile, KeyError) as error:
        print("DREHMAL AUTO-ROOT PREVIEW REFUSED: " + str(error), file=sys.stderr)
        sys.exit(2)
