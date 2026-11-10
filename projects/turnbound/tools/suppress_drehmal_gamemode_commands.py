#!/usr/bin/env python3
"""Hash-pinned suppression of 29 DIRECT original gamemode commands, copy only.

All 26 overworld and 3 minecraft:space command blocks retain NBT coordinates
and redstone/block structure; only their original gamemode command strings
are emptied. No actual Minecraft player state is changed by this script.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import re
import shutil
import sys
from pathlib import Path
import patch_drehmal_terminus_commands as regions

LEVEL_SHA="44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
AUX_SHA="9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef"
RECEIPT="turnbound-original-gamemode-preview.json"
BACKUP="turnbound-original-gamemode-backup"
TARGETS=29
REGIONS=7

def digest(raw):
    return hashlib.sha256(raw).hexdigest()

def parse_targets(data,expected_count=TARGETS):
    if (data.get("level_sha256")!=LEVEL_SHA
            or data.get("world_bytes_modified") is not False):
        raise ValueError("Original official world audit source mismatch")
    source=data["command_blocks"]
    if source["scanned_command_blocks"]!=2232:
        raise ValueError("Unexpected command block inventory size")
    all_blocks=source["all_block_metadata"]
    if len(all_blocks)!=2232:
        raise ValueError("Incomplete command block inventory")
    selected=[]
    for block in all_blocks:
        if block.get("root_operation")!="gamemode":
            continue
        path=block["region_file"]
        dimension=block["dimension"]
        rel=Path(path)
        if (rel.is_absolute() or ".." in rel.parts or not rel.name.endswith(".mca")
                or not ((dimension=="minecraft:overworld" and
                         len(rel.parts)==2 and rel.parts[0]=="region") or
                        (dimension=="minecraft:space" and rel.parts[:4]==
                         ("dimensions","minecraft","space","region") and len(rel.parts)==5))):
            raise ValueError("Unexpected source dimension/region")
        coords=block.get("coordinates")
        digest_value=block.get("command_sha256")
        if (block.get("effective_operation")!="UNRECOGNIZED_gamemode" or
                block.get("command_actor")!="minecraft:command_block" or
                block.get("literal_function_ids")!=[] or
                not isinstance(coords,list) or len(coords)!=3 or
                any(type(v) is not int for v in coords) or
                type(block.get("chunk_slot")) is not int or
                not 0<=block["chunk_slot"]<1024 or
                not isinstance(digest_value,str) or
                re.fullmatch(r"[0-9a-f]{64}",digest_value) is None):
            raise ValueError("Unexpected direct gamemode command metadata")
        selected.append((path,block["chunk_slot"],tuple(coords),
                         digest_value,block["command_actor"]))
    if len(selected)!=expected_count or len(selected)!=len(set(selected)):
        raise ValueError("Original direct gamemode target count or identities changed")
    if expected_count==TARGETS:
        per_dimension=collections.Counter(
            "minecraft:space" if t[0].startswith("dimensions/") else "minecraft:overworld"
            for t in selected)
        if per_dimension!={"minecraft:overworld":26,"minecraft:space":3}:
            raise ValueError("Original direct gamemode locations changed")
        if len({t[0] for t in selected})!=REGIONS:
            raise ValueError("Original region count changed")
    return selected

def read_targets(path):
    raw=Path(path).read_bytes()
    if digest(raw)!=AUX_SHA:
        raise ValueError("Official command-block audit SHA mismatch")
    return parse_targets(json.loads(raw))

def validate_copy(world,original_level_required=True):
    world=Path(world).resolve(strict=True)
    marker=world/"turnbound-stage.json"
    if (not marker.is_file() or marker.is_symlink()
            or json.loads(marker.read_text(encoding="utf-8")).get("source_level_sha256")!=LEVEL_SHA
            or (world/"turnbound-world.marker").exists()):
        raise ValueError("Only a non-activated official staged COPY is allowed")
    if original_level_required:
        regions.validate_world(world)
    return world

def preflight(world,targets):
    world=validate_copy(world)
    by_region=collections.defaultdict(lambda:collections.defaultdict(list))
    for region,slot,coords,command_sha,actor in targets:
        by_region[region][slot].append((coords,command_sha,actor))
    prepared={}
    for relative,slots in sorted(by_region.items()):
        file=world/relative
        if not file.is_file() or file.is_symlink():
            raise ValueError("Missing or symlinked target region")
        before=file.read_bytes()
        after,changed=regions.patch_region(before,slots)
        if changed!=sum(len(v) for v in slots.values()):
            raise ValueError("Gamemode command patch count mismatch")
        prepared[relative]=(before,after)
    if len(prepared)!=REGIONS:
        raise ValueError("Expected seven distinct gamemode region files")
    return prepared

def apply(world,targets):
    world=validate_copy(world)
    receipt=world/RECEIPT
    backup=world/BACKUP
    if receipt.exists() or backup.exists():
        raise ValueError("Existing original gamemode preview must be restored first")
    planned=preflight(world,targets)
    backup.mkdir()
    for rel,(before,_) in planned.items():
        dest=backup/rel
        dest.parent.mkdir(parents=True,exist_ok=True)
        dest.write_bytes(before)
        if regions.file_sha(dest)!=digest(before):
            raise ValueError("Original gamemode region backup verification failed")
    report={
        "schema":1,"mode":"OFFLINE_DIRECT_GAMEMODE_COMMAND_PREVIEW",
        "target_count":len(targets),"region_count":len(planned),
        "regions":{p:{"before":digest(before),"after":digest(after)}
                   for p,(before,after) in planned.items()},
        "source_level_sha256":LEVEL_SHA,
        "original_world_modified":False,
        "original_drehmal_code_reused":False,
        "gameplay_tested":False,
        "mechanism_compatibility_tested":False
    }
    # Receipt durable before any patched region write (interrupted copy can restore).
    receipt.write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    for rel,(_,after) in planned.items():
        regions.write_atomic(world/rel,after)
        if regions.file_sha(world/rel)!=digest(after):
            raise ValueError("Gamemode patched region hash differs")
    return report

def verify(world):
    world=validate_copy(world,original_level_required=False)
    path=world/RECEIPT
    if not path.is_file() or path.is_symlink() or not (world/BACKUP).is_dir():
        raise ValueError("No direct gamemode preview/backup available")
    report=json.loads(path.read_text(encoding="utf-8"))
    if (report.get("schema")!=1 or report.get("mode")!="OFFLINE_DIRECT_GAMEMODE_COMMAND_PREVIEW"
            or report.get("target_count")!=TARGETS
            or report.get("region_count")!=REGIONS
            or report.get("source_level_sha256")!=LEVEL_SHA):
        raise ValueError("Unknown direct gamemode receipt")
    for rel,hashes in report["regions"].items():
        p=Path(rel)
        if (p.is_absolute() or ".." in p.parts or not p.name.endswith(".mca")
                or not ((len(p.parts)==2 and p.parts[0]=="region") or
                        (len(p.parts)==5 and p.parts[:4]==
                         ("dimensions","minecraft","space","region")))):
            raise ValueError("Unsafe gamemode region receipt path")
        if regions.file_sha(world/p)!=hashes["after"]:
            raise ValueError("Gamemode region patch differs from receipt")
        if regions.file_sha(world/BACKUP/p)!=hashes["before"]:
            raise ValueError("Gamemode region original backup differs")
    return report

def restore(world):
    world=validate_copy(world)
    report=verify(world)
    # Verification of ALL modified files precedes first restore write.
    for rel,hashes in report["regions"].items():
        p=Path(rel)
        if regions.file_sha(world/p) not in (hashes["before"],hashes["after"]):
            raise ValueError("Working gamemode region modified externally")
    for rel,hashes in report["regions"].items():
        p=Path(rel)
        regions.write_atomic(world/p,(world/BACKUP/p).read_bytes())
        if regions.file_sha(world/p)!=hashes["before"]:
            raise ValueError("Gamemode exact region restoration failed")
    (world/RECEIPT).unlink()
    shutil.rmtree(world/BACKUP)
    return {"status":"ORIGINAL_DIRECT_GAMEMODE_REGIONS_RESTORED",
            "target_count":report["target_count"],"region_count":report["region_count"]}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("action",choices=("inspect","apply","verify","restore"))
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--source-aux",type=Path)
    p.add_argument("--output",required=True,type=Path)
    args=p.parse_args()
    world=args.world_copy.resolve(strict=True)
    out=args.output.resolve()
    if out==world or world in out.parents:
        p.error("Metadata output must be outside world")
    if args.action in ("inspect","apply"):
        if not args.source_aux:
            p.error("--source-aux required")
        targets=read_targets(args.source_aux)
        if args.action=="inspect":
            planned=preflight(world,targets)
            result={"status":"GAMEMODE_PREFLIGHT_NO_MUTATIONS",
                    "target_count":len(targets),"region_count":len(planned)}
        else:
            result=apply(world,targets)
    elif args.action=="verify":
        result=verify(world)
    else:
        result=restore(world)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":result.get("status",result.get("mode")),
                      "targets":result["target_count"],
                      "gameplay_tested":False},ensure_ascii=False))

if __name__=="__main__":
    try:main()
    except (ValueError,OSError,KeyError,TypeError) as e:
        print("Original gamemode command isolation REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)
