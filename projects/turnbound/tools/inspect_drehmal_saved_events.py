#!/usr/bin/env python3
"""Read-only, version-pinned audit of Drehmal 2.2.2f saved scheduled events.

Minecraft Java 1.20.1 stores pending function callbacks inside the compressed
level.dat Data.ScheduledEvents list; this is distinct from literal /schedule
commands in datapack functions. Never publishes raw NBT, inventories, score
owners, player UUIDs, or the original world file.
"""
from __future__ import annotations
import argparse
import collections
import gzip
import hashlib
import json
import sys
from pathlib import Path
from scan_drehmal_command_blocks import NBTReader, NBTError

LEVEL_SHA="44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
AUX_SHA="9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef"
MAX_NBT=64*1024*1024
RECOGNIZED_TYPES={"minecraft:function","minecraft:function_tag"}

def file_sha(path):
    sha=hashlib.sha256()
    with path.open("rb") as f:
        while chunk:=f.read(4*1024*1024):
            sha.update(chunk)
    return sha.hexdigest()

def read_gzip_nbt(path):
    if not path.is_file() or path.is_symlink():
        raise ValueError("Missing or symlinked NBT save file")
    with gzip.open(path,"rb") as f:
        raw=f.read(MAX_NBT+1)
    if len(raw)>MAX_NBT:
        raise ValueError("NBT document exceeds read-only audit limit")
    reader=NBTReader(raw)
    root=reader.root()
    if reader.pos!=len(raw) or not isinstance(root,dict):
        raise ValueError("Unexpected NBT root structure/trailing bytes")
    return root

def saved_schedule(level_root):
    data=level_root.get("Data")
    if not isinstance(data,dict):
        raise ValueError("Expected 1.20.1 Data compound")
    present="ScheduledEvents" in data
    entries=data.get("ScheduledEvents",[])
    if not isinstance(entries,list):
        raise ValueError("Unexpected saved ScheduledEvents format")
    if len(entries)>100_000:
        raise ValueError("Excessive scheduled event count")
    events=[]
    for index,event in enumerate(entries):
        if not isinstance(event,dict):
            raise ValueError("Unexpected event list entry")
        callback=event.get("Callback")
        if not isinstance(callback,dict):
            raise ValueError("Missing scheduled callback compound")
        identity=event.get("Name")
        tick=event.get("TriggerTime")
        callback_id=callback.get("Name")
        callback_type=callback.get("Type")
        if (not isinstance(identity,str) or not isinstance(callback_id,str)
                or not isinstance(callback_type,str) or type(tick) is not int):
            raise ValueError("Unexpected 1.20.1 scheduled event fields")
        events.append({
            "event_id":identity,
            "callback_id":callback_id,
            "callback_type":callback_type,
            "trigger_tick":tick,
            "type_review":"KNOWN_FUNCTION_CALLBACK" if callback_type in RECOGNIZED_TYPES else "UNRECOGNIZED_REVIEW",
            "action":"READ_ONLY_NOT_APPROVED_FOR_DELETION"
        })
    return {
        "tag_state":"HAS_SAVED_ENTRIES" if entries else ("EMPTY" if present else "ABSENT"),
        "count":len(events),
        "entries":events,
        "save_game_time":data.get("Time") if type(data.get("Time")) is int else None,
        "cannot_prove":"A saved snapshot does not prove what a live server will later schedule or run."
    }

def scoreboard_summary(world):
    path=world/"data"/"scoreboard.dat"
    if not path.exists():
        return {"state":"ABSENT","objective_count":0,"objectives":[]}
    root=read_gzip_nbt(path)
    data=root.get("data")
    if not isinstance(data,dict):
        raise ValueError("Unexpected scoreboard.dat structure")
    objectives=data.get("Objectives",[])
    if not isinstance(objectives,list):
        raise ValueError("Unexpected scoreboard objective list")
    details=[]
    for row in objectives:
        if not isinstance(row,dict):
            raise ValueError("Unexpected objective record")
        name=row.get("Name")
        criteria=row.get("CriteriaName")
        if not isinstance(name,str) or not isinstance(criteria,str):
            raise ValueError("Unexpected objective fields")
        details.append({"objective_id":name,"criteria":criteria})
    return {
        "state":"PRESENT","file_sha256":file_sha(path),
        "objective_count":len(details),"objectives":sorted(details,key=lambda x:x["objective_id"]),
        "player_scores":"NOT_READ_OR_EXPORTED"
    }

def command_storage_index(world):
    folder=world/"data"
    if not folder.is_dir() or folder.is_symlink():
        return []
    found=[]
    for file in sorted(folder.glob("command_storage_*.dat")):
        if file.is_symlink() or not file.is_file():
            raise ValueError("Unexpected command storage link or nonfile")
        found.append({"file_name":file.name,"bytes":file.stat().st_size,"sha256":file_sha(file),
                      "contents":"NOT_READ_TO_AVOID_REUSING_OR_EXPOSING_ORIGINAL_DATA"})
    return found

def static_schedule_evidence(aux_path):
    raw=aux_path.read_bytes()
    if hashlib.sha256(raw).hexdigest()!=AUX_SHA:
        raise ValueError("Official auxiliary audit report hash mismatch")
    aux=json.loads(raw)
    if aux.get("level_sha256")!=LEVEL_SHA or aux.get("world_bytes_modified") is not False:
        raise ValueError("Auxiliary audit source world mismatch")
    edges=aux["datapack_json_and_schedule"]["function_schedule_literal_sites"]
    return {
        "function_sites":len(edges),
        "literal_schedule_calls":sum(e["literal_schedule_count"] for e in edges),
        "literal_target_ids":sorted({dest for entry in edges for dest in entry["scheduled_target_ids"]})
    }

def inspect(world,aux_path,expected_sha=LEVEL_SHA):
    world=Path(world).resolve(strict=True)
    level=world/"level.dat"
    if not level.is_file() or level.is_symlink() or file_sha(level)!=expected_sha:
        raise ValueError("World level.dat does not match official Drehmal 2.2.2f")
    snapshot=saved_schedule(read_gzip_nbt(level))
    static=static_schedule_evidence(Path(aux_path))
    static_set=set(static.pop("literal_target_ids"))
    for entry in snapshot["entries"]:
        entry["referenced_by_literal_schedule_command"]=entry["callback_id"] in static_set
    scoreboard=scoreboard_summary(world)
    storage=command_storage_index(world)
    if file_sha(level)!=expected_sha:
        raise ValueError("Source level.dat changed during read-only inspection")
    return {
        "status":"READ_ONLY_SAVED_STATE_CHECK",
        "source_release":"Drehmal: APOTHEOSIS v2.2.2f / Minecraft Java 1.20.1",
        "official_level_dat_sha256":expected_sha,
        "scheduled_events_at_original_save":snapshot,
        "datapack_schedule_literals":static,
        "scoreboard_objective_inventory":scoreboard,
        "command_storage_files":storage,
        "world_changed":False,
        "original_code_reused":False,
        "next_step":"Identify structure-critical scoreboard/scheduled callbacks and distinguish original gameplay before any further copy-only separation.",
        "limitations":[
            "Saved ScheduledEvents only describe queued events at time world was last saved.",
            "Static /schedule sites do not imply they are already queued.",
            "Current test does not prove gameplay, door/portal mechanisms, or safe removals.",
            "Unknown callback types or absent event list cannot be interpreted as map-system safety."
        ]
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--world",type=Path,required=True)
    p.add_argument("--aux",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    options=p.parse_args()
    world=options.world.resolve(strict=True)
    out=options.output.resolve()
    if out==world or world in out.parents:
        p.error("Report must be outside original world")
    r=inspect(world,options.aux)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(r,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({
        "scheduled_saved_events":r["scheduled_events_at_original_save"]["count"],
        "scheduled_source_calls":r["datapack_schedule_literals"]["literal_schedule_calls"],
        "scoreboard_objectives":r["scoreboard_objective_inventory"]["objective_count"],
        "storage_file_count":len(r["command_storage_files"]),
        "world_changed":False
    },ensure_ascii=False))

if __name__=="__main__":
    try: main()
    except (OSError,ValueError,KeyError,NBTError,EOFError,TypeError) as error:
        print("Official saved event inspection blocked: "+str(error),file=sys.stderr)
        sys.exit(2)
