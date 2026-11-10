#!/usr/bin/env python3
"""Experimental seventh offline Drehmal boundary: clear 33 residual START chains.

Requires an already VERIFIED six-stage non-activated COPY. Original command strings
are only used for source SHA checks; never reused in TURNBOUND or output reports.
This does not prove the original scene mechanisms remain functional.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import shutil
import sys
from pathlib import Path

import patch_drehmal_terminus_commands as regions
import prepare_drehmal_isolated_copy as six
import preview_drehmal_starter_discovery_commands as starter
import suppress_drehmal_gamemode_commands as modes
from scan_drehmal_command_blocks import scan_region

REGION="region/r.51.-1.mca"
AUX_SHA=modes.AUX_SHA
RECEIPT="turnbound-start-chain-residual-preview.json"
BACKUP="turnbound-start-chain-residual-original-region"
EXPECTED_COUNT=33
CHAIN_COUNT=57
TOP=(26458,26461,26464)
BOTTOM=(26457,26461,26465)
EXPECTED={
    "tp":3, "effect":3, "setblock":6, "spawnpoint":3,
    "scoreboard":6, "advancement":6, "spreadplayers":3, "gamerule":3
}

def sha(raw): return hashlib.sha256(raw).hexdigest()

def coordinate_roles():
    positions={}
    top={205:("tp","tp"),206:("effect","effect"),
         207:("setblock","setblock"),208:("setblock","setblock"),
         209:("execute","scoreboard"),210:("execute","advancement"),
         211:("spawnpoint","spawnpoint")}
    low={206:("spreadplayers","spreadplayers"),208:("gamerule","gamerule"),
         210:("execute","scoreboard"),211:("execute","advancement")}
    for x in TOP:
        for y,role in top.items(): positions[(x,y,-288)]=role
    for x in BOTTOM:
        for y,role in low.items(): positions[(x,y,-260)]=role
    assert len(positions)==EXPECTED_COUNT
    return positions

def read_targets(aux):
    raw=Path(aux).read_bytes()
    if sha(raw)!=AUX_SHA: raise ValueError("Official command block audit SHA drift")
    doc=json.loads(raw)
    if (doc.get("level_sha256")!=six.LEVEL_SHA or
            doc.get("world_bytes_modified") is not False or
            doc.get("command_blocks",{}).get("scanned_command_blocks")!=2232):
        raise ValueError("Original map source metadata mismatch")
    roles=coordinate_roles()
    seen={}
    counts=collections.Counter()
    for entry in doc["command_blocks"]["all_block_metadata"]:
        coords=entry.get("coordinates")
        if not isinstance(coords,list) or len(coords)!=3: continue
        pos=tuple(coords)
        if pos not in roles: continue
        if pos in seen: raise ValueError("Duplicate original chain position")
        root,op=roles[pos]
        if (entry.get("dimension")!="minecraft:overworld" or
                entry.get("region_file")!=REGION or
                entry.get("command_actor")!="minecraft:command_block" or
                entry.get("root_operation")!=root or
                entry.get("effective_operation")!=op or
                entry.get("classification")!="MANUAL_REVIEW_NO_REMOVAL" or
                type(entry.get("chunk_slot")) is not int or
                entry["chunk_slot"] != ((pos[0]//16)&31)+(((pos[2]//16)&31)<<5) or
                not isinstance(entry.get("command_sha256"),str) or
                len(entry["command_sha256"])!=64):
            raise ValueError("Source chain command identity/role drift")
        try: bytes.fromhex(entry["command_sha256"])
        except ValueError as exc: raise ValueError("Invalid command hash") from exc
        seen[pos]=(REGION,entry["chunk_slot"],pos,
                   entry["command_sha256"],entry["command_actor"])
        counts[op]+=1
    if set(seen)!=set(roles) or dict(counts)!=EXPECTED:
        raise ValueError("Expected exactly 33 known residual start-chain commands")
    return [seen[pos] for pos in sorted(seen)]

def validate_copy(world,overlay=False):
    world=Path(world).resolve(strict=True)
    starter.validate_copy(world,composite=True,original_level_required=False)
    if not (world/starter.RECEIPT).is_file() or not (world/six.SAVED_RECEIPT).is_file():
        raise ValueError("Seven-stage experiment requires completed six-stage copy")
    for name in (RECEIPT,BACKUP):
        if not overlay and (world/name).exists():
            raise ValueError("Residual preview already active or interrupted")
    return world

def preflight(world,targets):
    world=validate_copy(world)
    six.verify(world)  # Existing five + 24 stages must be independently valid.
    slots=collections.defaultdict(list)
    for rel,slot,pos,digest,actor in targets:
        if rel!=REGION: raise ValueError("Unexpected region in start-chain scan")
        slots[slot].append((pos,digest,actor))
    file=world/REGION
    if not file.is_file() or file.is_symlink():
        raise ValueError("Missing/symlinked source start-chain region")
    original=file.read_bytes()
    updated,count=regions.patch_region(original,slots)
    if count!=EXPECTED_COUNT: raise ValueError("Partial seventh-stage patch")
    return original,updated

def verify_empty_chain(world):
    records,warnings=scan_region(Path(world)/REGION)
    expected=set(coordinate_roles())
    expected.update((x,y,-288) for x in TOP for y in (212,213,214))
    expected.update((x,y,-260) for x in BOTTOM for y in (207,209,212,213,214))
    if len(expected)!=CHAIN_COUNT: raise ValueError("Chain geometry count drift")
    seen=collections.defaultdict(list)
    for item in records:
        pos=(item.get("x"),item.get("y"),item.get("z"))
        if pos in expected: seen[pos].append(item["command"])
    if set(seen)!=expected or any(commands!=[""] for commands in seen.values()):
        raise ValueError("One or more of 57 original START commands remain active")
    return {"empty_start_chain_commands":len(seen),
            "region_scan_warnings":len(warnings)}

def apply(world,targets):
    world=validate_copy(world)
    before,after=preflight(world,targets)
    backup=world/BACKUP
    receipt=world/RECEIPT
    backup.mkdir()
    backupfile=backup/"r.51.-1.mca"
    backupfile.write_bytes(before)
    if regions.file_sha(backupfile)!=sha(before):
        raise ValueError("Residual original region backup failed hash check")
    record={"schema":1,"status":"OFFLINE_START_CHAIN_RESIDUAL_EXPERIMENT",
            "source_level_sha256":six.LEVEL_SHA,"source_aux_sha256":AUX_SHA,
            "command_count":EXPECTED_COUNT,"start_chain_total":CHAIN_COUNT,
            "region_file":REGION,"before":sha(before),"after":sha(after),
            "original_code_reused":False,"world_activated":False,
            "structure_functionality_tested":False,"playtested":False}
    receipt.write_text(json.dumps(record,indent=2)+"\n",encoding="utf-8")
    regions.write_atomic(world/REGION,after)
    checked=verify(world)
    return checked

def verify(world):
    world=validate_copy(world,overlay=True)
    record_path=world/RECEIPT
    backupfile=world/BACKUP/"r.51.-1.mca"
    if (not record_path.is_file() or record_path.is_symlink()
            or not backupfile.is_file() or backupfile.is_symlink()):
        raise ValueError("Residual copy preview receipt/backup missing")
    doc=json.loads(record_path.read_text(encoding="utf-8"))
    if (doc.get("schema")!=1 or doc.get("status")!="OFFLINE_START_CHAIN_RESIDUAL_EXPERIMENT" or
            doc.get("source_level_sha256")!=six.LEVEL_SHA or
            doc.get("source_aux_sha256")!=AUX_SHA or
            doc.get("command_count")!=EXPECTED_COUNT or
            doc.get("start_chain_total")!=CHAIN_COUNT or
            doc.get("region_file")!=REGION or
            doc.get("original_code_reused") is not False or
            doc.get("world_activated") is not False):
        raise ValueError("Residual preview receipt incompatible")
    if regions.file_sha(backupfile)!=doc["before"] or regions.file_sha(world/REGION)!=doc["after"]:
        raise ValueError("Residual region or its backup has changed")
    rstarter=six.receipt(world,six.STARTER_RECEIPT)
    expected_previous=rstarter["regions"][REGION]["after"]
    if doc["before"]!=expected_previous:
        raise ValueError("Seven-stage overlay does not follow the six-stage region hash")
    return {**doc, **verify_empty_chain(world),
            "seven_stage_offline_experiment_verified":True,
            "map_separation_complete":False}

def restore(world):
    record=verify(world)
    original=(Path(world)/BACKUP/"r.51.-1.mca").read_bytes()
    regions.write_atomic(Path(world)/REGION,original)
    if regions.file_sha(Path(world)/REGION)!=record["before"]:
        raise ValueError("Seventh-stage region exact restoration failed")
    (Path(world)/RECEIPT).unlink()
    shutil.rmtree(Path(world)/BACKUP)
    base=six.verify(world)
    if base["status"]!="SIX_BOUNDARIES_PRESENT_VERIFIED_OFFLINE_EXPERIMENT":
        raise ValueError("Original six-stage copy no longer matches receipts")
    return {"status":"SEVENTH_STAGE_RESTORED_TO_VERIFIED_SIX_STAGE",
            "region_file":REGION,"restored_sha256":record["before"],
            "world_activated":False,"map_separation_complete":False}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("action",choices=("inspect","apply","verify","restore"))
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--source-aux",type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    world=a.world_copy.resolve(strict=True)
    out=a.output.resolve()
    if world==out or world in out.parents:
        p.error("Report must be outside world")
    if a.action in ("inspect","apply"):
        if not a.source_aux: p.error("Inspect/apply require --source-aux")
        targets=read_targets(a.source_aux)
        if a.action=="inspect":
            before,after=preflight(world,targets)
            result={"status":"SEVENTH_STAGE_PREFLIGHT_OFFLINE_ONLY",
                    "command_count":len(targets),"source_sha256":sha(before),
                    "proposed_sha256":sha(after),"world_modified":False,
                    "structure_functionality_tested":False}
        else: result=apply(world,targets)
    elif a.action=="verify": result=verify(world)
    else: result=restore(world)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({"status":result["status"],
                      "map_separation_complete":False,
                      "playtested":False}))

if __name__=="__main__":
    try: main()
    except (ValueError,OSError,KeyError,TypeError) as e:
        print("Seven-stage original start-chain experiment REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)
