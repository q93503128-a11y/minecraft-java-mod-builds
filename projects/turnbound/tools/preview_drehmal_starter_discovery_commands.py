#!/usr/bin/env python3
"""Reversible COPY-ONLY experiment: 21 original starter item + 3 discovery grants.

This isolates a tightly pinned source *candidate* on a non-activated copied
Drehmal 2.2.2f world. No original code is used in TURNBOUND. It does NOT
establish that the command blocks are safe to suppress in a playable build.
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
import suppress_drehmal_gamemode_commands as gamemode

LEVEL_SHA="44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
DIRECT_REPORT_SHA="235e9f2d695f01b84efc36beae5683cc0a8600559a2fd599303be761639b793e"
RECEIPT="turnbound-starter-discovery-copy-preview.json"
BACKUP="turnbound-starter-discovery-original-regions"
EXPECTED_STARTER={"minecraft:white_bed":3,"minecraft:bread":6,
                  "minecraft:stone_pickaxe":6,"minecraft:leather_boots":6}
EXPECTED_ADVANCEMENTS={
    "discoveries:homligrave":"region/r.4.-6.mca",
    "primordial:enteryavhlix":"region/r.52.-1.mca",
    "discoveries:divineruins":"region/r.53.-1.mca",
}
STARTER_REGION="region/r.51.-1.mca"
TARGET_COUNT=24
REGION_COUNT=4
REVIEW_STATUS="EXPERIMENT_ONLY_STRUCTURE_CHAINS_NOT_PLAYTESTED"

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def load_candidates(path):
    raw=Path(path).read_bytes()
    if sha(raw)!=DIRECT_REPORT_SHA:
        raise ValueError("Official direct gameplay report SHA drift")
    data=json.loads(raw)
    if (data.get("status")!="READ_ONLY_DIRECT_GAMEPLAY_MECHANISM_TRIAGE"
            or data.get("source_level_sha256")!=LEVEL_SHA
            or data.get("world_modified") is not False
            or data.get("automatic_suppression_allowed") is not False
            or data.get("reverified_target_regions")!=72
            or data.get("candidates_missing_physical_coordinates")!=6):
        raise ValueError("Original direct gameplay analysis identity/scope differs")
    return select_candidates(data)

def select_candidates(data):
    targets=[]
    starter=collections.Counter()
    advances=collections.Counter()
    for row in data.get("physical_candidates",[]):
        operation=row.get("operation")
        dimension=row.get("dimension")
        coords=row.get("coordinates")
        path=row.get("region_file")
        resource=row.get("resource_id")
        selected=False
        if (operation=="give" and path==STARTER_REGION and
                isinstance(coords,list) and len(coords)==3 and
                26457<=coords[0]<=26465 and 207<=coords[1]<=214 and
                coords[2] in (-260,-288) and resource in EXPECTED_STARTER):
            starter[resource]+=1
            selected=True
        elif (operation=="advancement" and
              resource in EXPECTED_ADVANCEMENTS and
              path==EXPECTED_ADVANCEMENTS[resource]):
            advances[resource]+=1
            selected=True
        if not selected:
            continue
        if (dimension!="minecraft:overworld" or
                row.get("actor")!="minecraft:command_block" or
                row.get("approved_for_removal") is not False or
                row.get("physical_coordinate_review")!="KNOWN" or
                type(row.get("chunk_slot")) is not int or not 0<=row["chunk_slot"]<1024 or
                not isinstance(coords,list) or len(coords)!=3 or
                any(type(v) is not int for v in coords) or
                not isinstance(row.get("command_sha256"),str) or
                re.fullmatch(r"[0-9a-f]{64}",row["command_sha256"]) is None):
            raise ValueError("Candidate source evidence is not safe for copy-only preview")
        if operation=="give" and row.get("target_class")!="NEAREST_PLAYER":
            raise ValueError("Unexpected starter item recipient")
        if operation=="advancement" and (
                row.get("target_class")!="ALL_PLAYERS" or
                row.get("advancement_action")!="grant"):
            raise ValueError("Unexpected original discovery award command")
        targets.append((path,row["chunk_slot"],tuple(coords),
                        row["command_sha256"],row["actor"]))
    if (len(targets)!=TARGET_COUNT or len(set(targets))!=TARGET_COUNT
            or dict(starter)!=EXPECTED_STARTER
            or set(advances)!=set(EXPECTED_ADVANCEMENTS)
            or any(count!=1 for count in advances.values())
            or len({x[0] for x in targets})!=REGION_COUNT):
        raise ValueError("Pinned starter/discovery isolated candidate set drifted")
    return targets

def validate_copy(world,composite=False,original_level_required=True):
    world=gamemode.validate_copy(world,original_level_required=original_level_required)
    if composite:
        for prerequisite in (regions.RECEIPT,gamemode.RECEIPT,
                             "turnbound-auto-root-preview.json",
                             "turnbound-weapon-reward-preview.json"):
            if not (world/prerequisite).is_file() or (world/prerequisite).is_symlink():
                raise ValueError("Incomplete five-boundary prerequisite for offline experiment")
        return world
    for suspect in (regions.RECEIPT,regions.BACKUP,
                    gamemode.RECEIPT,gamemode.BACKUP,
                    "turnbound-weapon-reward-preview.json",
                    "turnbound-saved-reward-schedule-preview.json"):
        if (world/suspect).exists():
            raise ValueError("Other preview is active; stage an independent fresh world copy")
    return world

def preflight(world,targets,composite=False):
    world=validate_copy(world,composite=composite)
    if (world/RECEIPT).exists() or (world/BACKUP).exists():
        raise ValueError("Existing starter/discovery preview must be restored")
    grouped=collections.defaultdict(lambda:collections.defaultdict(list))
    for rel,slot,pos,digest,actor in targets:
        grouped[rel][slot].append((pos,digest,actor))
    changes={}
    for rel,slots in sorted(grouped.items()):
        file=world/rel
        if not file.is_file() or file.is_symlink():
            raise ValueError("Missing/symlinked original target region")
        before=file.read_bytes()
        after,count=regions.patch_region(before,slots)
        if count!=sum(len(x) for x in slots.values()):
            raise ValueError("Incomplete command isolation")
        changes[rel]=(before,after)
    if len(changes)!=REGION_COUNT:
        raise ValueError("Unexpected target region count")
    return changes

def apply(world,targets,composite=False):
    world=validate_copy(world,composite=composite)
    changes=preflight(world,targets,composite=composite)
    backup=world/BACKUP
    receipt=world/RECEIPT
    backup.mkdir()
    for rel,(original,_) in changes.items():
        path=backup/rel
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(original)
        if regions.file_sha(path)!=sha(original):
            raise ValueError("Original copied region backup failed hash check")
    record={
        "schema":1,"mode":"OFFLINE_STARTER_AND_DISCOVERY_EXPERIMENT",
        "target_count":len(targets),"starter_item_commands":21,
        "advancement_grant_commands":3,"region_count":len(changes),
        "source_level_sha256":LEVEL_SHA,
        "source_direct_report_sha256":DIRECT_REPORT_SHA,
        "regions":{name:{"before":sha(old),"after":sha(new)}
                   for name,(old,new) in changes.items()},
        "world_modified":"COPIED_WORLD_ONLY",
        "original_drehmal_code_reused":False,
        "structural_mechanisms_tested":False,
        "gameplay_tested":False,
        "review_status":REVIEW_STATUS
    }
    # Backup and manifest are persisted prior to the first target-file change.
    receipt.write_text(json.dumps(record,indent=2)+"\n",encoding="utf-8")
    for rel,(_,updated) in changes.items():
        regions.write_atomic(world/rel,updated)
        if regions.file_sha(world/rel)!=sha(updated):
            raise ValueError("Isolated copied region SHA verification failed")
    return record

def verify(world,composite=False):
    world=validate_copy(world,composite=composite,original_level_required=not composite)
    backup=world/BACKUP
    receipt=world/RECEIPT
    if not receipt.is_file() or receipt.is_symlink() or not backup.is_dir():
        raise ValueError("No starter/discovery preview receipt and backup")
    result=json.loads(receipt.read_text(encoding="utf-8"))
    if (result.get("schema")!=1 or result.get("mode")!="OFFLINE_STARTER_AND_DISCOVERY_EXPERIMENT"
            or result.get("target_count")!=24 or result.get("starter_item_commands")!=21
            or result.get("advancement_grant_commands")!=3
            or result.get("region_count")!=REGION_COUNT
            or result.get("source_level_sha256")!=LEVEL_SHA
            or result.get("source_direct_report_sha256")!=DIRECT_REPORT_SHA):
        raise ValueError("Unrecognized starter/discovery preview manifest")
    for rel,hashes in result["regions"].items():
        p=Path(rel)
        if p.is_absolute() or ".." in p.parts or len(p.parts)!=2 or p.parts[0]!="region" or not p.name.endswith(".mca"):
            raise ValueError("Unsafe preview region path")
        if regions.file_sha(world/p)!=hashes["after"]:
            raise ValueError("Source was edited after isolated copy preview: "+rel)
        if regions.file_sha(backup/p)!=hashes["before"]:
            raise ValueError("Corrupt exact original region backup: "+rel)
    if len(result["regions"])!=REGION_COUNT:
        raise ValueError("Preview record regions missing")
    return result

def restore(world,composite=False):
    world=validate_copy(world,composite=composite)
    record=verify(world,composite=composite)
    for rel,hashes in record["regions"].items():
        original=(world/BACKUP/rel).read_bytes()
        regions.write_atomic(world/rel,original)
        if regions.file_sha(world/rel)!=hashes["before"]:
            raise ValueError("Exact original region restore failed")
    (world/RECEIPT).unlink()
    shutil.rmtree(world/BACKUP)
    return {
        "status":"ORIGINAL_STARTER_DISCOVERY_REGIONS_RESTORED",
        "target_count":record["target_count"],
        "region_count":record["region_count"],
        "original_world_unchanged":True,
        "playtested":False
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("action",choices=("inspect","apply","verify","restore"))
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--direct-report",type=Path)
    p.add_argument("--output",required=True,type=Path)
    args=p.parse_args()
    world=args.world_copy.resolve(strict=True)
    out=args.output.resolve()
    if out==world or world in out.parents:
        p.error("Report must not be written inside world copy")
    if args.action in ("inspect","apply"):
        if not args.direct_report:
            p.error("--direct-report required")
        targets=load_candidates(args.direct_report)
        if args.action=="inspect":
            regions_plan=preflight(world,targets)
            r={"status":"STARTER_DISCOVERY_PREFLIGHT","target_count":24,
               "region_count":len(regions_plan),"world_changed":False,
               "gameplay_tested":False}
        else:
            r=apply(world,targets)
    elif args.action=="verify":
        r=verify(world)
    else:
        r=restore(world)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(r,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":r.get("status",r.get("mode")),
                      "target_count":r["target_count"],"playtested":False}))

if __name__=="__main__":
    try:main()
    except (ValueError,OSError,KeyError,TypeError) as error:
        print("Original starter/discovery copied-world preview REFUSED: "+str(error),file=sys.stderr)
        sys.exit(2)
