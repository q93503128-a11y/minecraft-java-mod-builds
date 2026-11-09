#!/usr/bin/env python3
"""Hash-pinned, reversible OFFLINE removal of 39 original Drehmal weapon rewards.

Only operate on a staged, unactivated official 2.2.2f world COPY. No original
Drehmal function/command code is reused in TURNBOUND. No gameplay/world release.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import io
import json
import os
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
ORIGINAL_PACK_SHA = "f074d2927ea2947f14fe0b2836332695087e5bbf2e7c3b08f94f2e4f36c6fc74"
AUX_SHA = "9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef"
GRAPH_SHA = "e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826"
PACK = "hi_drehmal.zip"
BACKUP = "turnbound-weapon-reward-preview-backup"
RECEIPT = "turnbound-weapon-reward-preview.json"

def digest(data):
    return hashlib.sha256(data).hexdigest()

def file_hash(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        while part := f.read(4 * 1024 * 1024):
            h.update(part)
    return h.hexdigest()

def staged(world):
    world = Path(world).resolve(strict=True)
    level, marker = world / "level.dat", world / "turnbound-stage.json"
    if any(not p.is_file() or p.is_symlink() for p in (level, marker)):
        raise ValueError("Only a staged COPY of the original world is accepted")
    if file_hash(level) != LEVEL_SHA:
        raise ValueError("Unexpected original world level.dat")
    info = json.loads(marker.read_text(encoding="utf-8"))
    if info.get("schema") != 1 or info.get("source_level_sha256") != LEVEL_SHA:
        raise ValueError("Staging marker does not match original world")
    if (world / "turnbound-world.marker").exists():
        raise ValueError("Refusing activated TURNBOUND world")
    pack = world / "datapacks" / PACK
    if not pack.is_file() or pack.is_symlink():
        raise ValueError("Original verified datapack absent")
    return world, pack

def manifest(aux, graph, count=39):
    if (aux.get("level_sha256") != LEVEL_SHA or
            graph.get("world_level_dat_sha256") != LEVEL_SHA or
            aux.get("world_bytes_modified") is not False):
        raise ValueError("Unverified source world")
    index = {x["id"]: x for x in graph["functions"]}
    reward_rows = aux["datapack_json_and_schedule"]["advancement_reward_records"]
    targets = {}
    for record in reward_rows:
        name = record["file"]
        if not (name.startswith("data/weapons/advancements/") and name.endswith(".json")):
            continue
        fid = record["reward_function_id"]
        if (record["pack"] != PACK or not isinstance(fid, str) or
                not fid.startswith("weapons:") or
                record["reward_field_names"] != ["function", "recipes"] or
                record["criteria_triggers"] != ["minecraft:inventory_changed"]):
            raise ValueError("Original weapon advancement has unexpected reward or trigger")
        func = index.get(fid)
        if (func is None or func["pack"] != PACK or func["called_by"] or func["calls"]
                or func["reachable_from_load_or_tick"]):
            raise ValueError("Original weapon function graph needs manual review")
        if name in targets:
            raise ValueError("Duplicate weapon advancement path")
        targets[name] = (record["file_sha256"], fid)
    if len(targets) != count:
        raise ValueError(f"Expected {count} original weapon advancement rewards, got {len(targets)}")
    return targets

def load_evidence(aux_path, graph_path):
    raw_a, raw_g = Path(aux_path).read_bytes(), Path(graph_path).read_bytes()
    if digest(raw_a) != AUX_SHA or digest(raw_g) != GRAPH_SHA:
        raise ValueError("Original analysis artifact hash mismatch")
    return manifest(json.loads(raw_a), json.loads(raw_g))

def repackage(pack, targets):
    if file_hash(pack) != ORIGINAL_PACK_SHA:
        raise ValueError("Datapack has changed; weapon preview needs untouched original pack")
    changed = 0
    with zipfile.ZipFile(pack) as source, io.BytesIO() as memory:
        filenames = source.namelist()
        if len(filenames) != len(set(filenames)):
            raise ValueError("Ambiguous duplicate ZIP paths")
        if not set(targets).issubset(set(filenames)):
            raise ValueError("Missing original advancement file")
        with zipfile.ZipFile(memory, "w") as output:
            output.comment = source.comment
            for member in source.infolist():
                original = source.read(member)
                updated = original
                if member.filename in targets:
                    expected_sha, fid = targets[member.filename]
                    if digest(original) != expected_sha:
                        raise ValueError("Original advancement source SHA drift")
                    document = json.loads(original)
                    if not isinstance(document, dict) or set(document.get("rewards", {})) != {"function", "recipes"}:
                        raise ValueError("Unrecognized advancement reward structure")
                    if document["rewards"]["function"] != fid:
                        raise ValueError("Advancement callback function differs from source report")
                    if not isinstance(document["rewards"]["recipes"], list):
                        raise ValueError("Unexpected recipe award")
                    if not isinstance(document.get("criteria"), dict):
                        raise ValueError("Advancement completion criteria absent")
                    doc = copy.deepcopy(document)
                    doc["rewards"] = {}
                    updated = (json.dumps(doc, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
                    if (json.loads(updated) | {"rewards": document["rewards"]}) != document:
                        raise ValueError("Unexpected advancement structural mutation")
                    changed += 1
                output.writestr(member, updated)
        if changed != len(targets):
            raise ValueError("Not all approved original weapon callbacks found")
        patched = memory.getvalue()
    with zipfile.ZipFile(pack) as source, zipfile.ZipFile(io.BytesIO(patched)) as output:
        if source.namelist() != output.namelist():
            raise ValueError("Datapack member list changed")
        for name in source.namelist():
            if name not in targets and source.read(name) != output.read(name):
                raise ValueError("Non-target datapack resource changed")
    return patched

def atomic_file(path, data):
    with tempfile.NamedTemporaryFile(dir=path.parent, prefix=".turnbound-", delete=False) as f:
        tmp = Path(f.name)
        f.write(data)
    try:
        os.replace(tmp, path)
    finally:
        tmp.unlink(missing_ok=True)

def apply(world, targets):
    world, pack = staged(world)
    backup_dir, receipt = world / BACKUP, world / RECEIPT
    if backup_dir.exists() or receipt.exists():
        raise ValueError("Existing weapon preview must be restored first")
    patched = repackage(pack, targets)
    before = file_hash(pack)
    backup_dir.mkdir()
    backup = backup_dir / (PACK + ".original")
    shutil.copy2(pack, backup)
    if file_hash(backup) != before:
        raise ValueError("Backup hash mismatch")
    record = {
        "schema": 1, "status": "OFFLINE_WEAPON_REWARD_PREVIEW",
        "advancement_count": len(targets),
        "pack_original_sha256": before,
        "pack_patched_sha256": digest(patched),
        "other_zip_members_byte_preserved": True,
        "advancement_completion_criteria_preserved": True,
        "level_dat_sha256": LEVEL_SHA,
        "map_gameplay_tested": False,
        "original_code_reused": False,
        "note": "Only original weapon advancement reward fields emptied; other execution paths remain."
    }
    # Write receipt before mutation so interrupted operations can restore safely.
    receipt.write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")
    atomic_file(pack, patched)
    if file_hash(pack) != record["pack_patched_sha256"]:
        raise ValueError("Patched weapon pack hash mismatch")
    return record

def restore(world):
    world, pack = staged(world)
    receipt = world / RECEIPT
    backup = world / BACKUP / (PACK + ".original")
    if not receipt.is_file() or not backup.is_file():
        raise ValueError("Missing weapon reward preview backup")
    info = json.loads(receipt.read_text(encoding="utf-8"))
    if (info.get("schema") != 1 or info.get("status") != "OFFLINE_WEAPON_REWARD_PREVIEW"
            or info.get("level_dat_sha256") != LEVEL_SHA):
        raise ValueError("Unrecognized preview receipt")
    if file_hash(pack) not in (info["pack_original_sha256"], info["pack_patched_sha256"]):
        raise ValueError("Working datapack edited since preview; refusing overwrite")
    if file_hash(backup) != info["pack_original_sha256"]:
        raise ValueError("Original backup corrupted")
    atomic_file(pack, backup.read_bytes())
    if file_hash(pack) != info["pack_original_sha256"]:
        raise ValueError("Restoration verification failed")
    receipt.unlink()
    backup.unlink()
    (world / BACKUP).rmdir()
    return {"status": "RESTORED_ORIGINAL_BYTES",
            "advancement_count": info["advancement_count"],
            "pack_sha256": file_hash(pack)}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("action", choices=("inspect", "apply", "restore"))
    p.add_argument("--world-copy",type=Path,required=True)
    p.add_argument("--source-aux",type=Path)
    p.add_argument("--source-graph",type=Path)
    p.add_argument("--output",type=Path,required=True)
    args=p.parse_args()
    world=args.world_copy.resolve(strict=True)
    out=args.output.resolve()
    if out==world or world in out.parents:
        p.error("Output must not be inside world")
    if args.action=="restore":
        result=restore(world)
    else:
        if not args.source_aux or not args.source_graph:
            p.error("Both --source-aux and --source-graph required")
        targets=load_evidence(args.source_aux,args.source_graph)
        if args.action=="inspect":
            _, pack=staged(world)
            result={"status":"READY_FOR_OFFLINE_PREVIEW",
                    "advancement_count":len(targets),
                    "patched_sha256":digest(repackage(pack,targets)),
                    "world_changed":False}
        else:
            result=apply(world,targets)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":result["status"],
                      "advancement_count":result["advancement_count"],
                      "original_code_reused":False},ensure_ascii=False))

if __name__=="__main__":
    try: main()
    except (OSError,ValueError,KeyError,zipfile.BadZipFile) as ex:
        print("Drehmal weapon reward separation blocked: "+str(ex),file=sys.stderr)
        sys.exit(2)
