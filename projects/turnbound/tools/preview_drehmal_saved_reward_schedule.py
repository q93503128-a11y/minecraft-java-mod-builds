#!/usr/bin/env python3
"""Reversible OFFLINE removal of one verified saved Drehmal reward scheduler.

Only a staged inactive world COPY with exact official level.dat SHA is allowed.
This is a separation preview, not TURNBOUND gameplay or use of original code.
"""
from __future__ import annotations
import argparse
import gzip
import hashlib
import io
import json
import os
import struct
import sys
import tempfile
from pathlib import Path
from scan_drehmal_command_blocks import NBTReader, NBTError
from inspect_drehmal_saved_events import file_sha, read_gzip_nbt, saved_schedule

LEVEL_SHA="44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
EVENT_ID="terminus:gifts/respawn_gift"
EVENT_TICK=1135083594
ORIGINAL_TIME=1135083463
RECEIPT="turnbound-saved-reward-schedule-preview.json"
BACKUP="turnbound-saved-reward-schedule-backup.dat"
MAX_NBT=64*1024*1024

def digest(raw):
    return hashlib.sha256(raw).hexdigest()

def load_nbt(blob):
    with gzip.GzipFile(fileobj=io.BytesIO(blob),mode="rb") as f:
        raw=f.read(MAX_NBT+1)
    if len(raw)>MAX_NBT:
        raise ValueError("Original level.dat NBT too large")
    reader=NBTReader(raw)
    root=reader.root()
    if reader.pos!=len(raw):
        raise ValueError("Trailing source NBT bytes")
    return raw,root

def check_copy(world,original_level_sha=LEVEL_SHA):
    world=Path(world).resolve(strict=True)
    marker=world/"turnbound-stage.json"
    level=world/"level.dat"
    if not marker.is_file() or marker.is_symlink() or not level.is_file() or level.is_symlink():
        raise ValueError("Only staged world copies are allowed")
    data=json.loads(marker.read_text(encoding="utf-8"))
    if data.get("schema")!=1 or data.get("source_level_sha256")!=original_level_sha:
        raise ValueError("Staging receipt does not match original world")
    if (world/"turnbound-world.marker").exists():
        raise ValueError("Cannot edit activated TURNBOUND world")
    return world,level

def preview_bytes(original,source_sha=LEVEL_SHA,expected_name=EVENT_ID,
                  expected_tick=EVENT_TICK,expected_game_time=ORIGINAL_TIME):
    if digest(original)!=source_sha:
        raise ValueError("Official original level.dat hash mismatch")
    raw,root=load_nbt(original)
    state=saved_schedule(root)
    if state["count"]!=1 or state["save_game_time"]!=expected_game_time:
        raise ValueError("Unexpected saved scheduled event count or world time")
    event=state["entries"][0]
    if (event["event_id"]!=expected_name or event["callback_id"]!=expected_name
            or event["callback_type"]!="minecraft:function" or
            event["trigger_tick"]!=expected_tick):
        raise ValueError("Saved reward event differs from reviewed official version")
    # Bind the binary list entry to the semantically verified NBT tree, not
    # a naked regex over source bytes. The pinned full level SHA plus unique
    # serialized list header prevents accidental edits to unrelated sections.
    name=expected_name
    field="ScheduledEvents".encode("utf-8")
    marker=b"\x09"+struct.pack(">H",len(field))+field+b"\x0a"+struct.pack(">i",1)
    first=raw.find(marker)
    if first<0 or raw.find(marker,first+1)!=-1:
        raise ValueError("Ambiguous original ScheduledEvents binary field")
    count_offset=first+1+2+len(field)+1
    entry_start=count_offset+4
    reader=NBTReader(raw)
    reader.pos=entry_start
    binary_event=reader.value(10)
    if (binary_event!=root["Data"]["ScheduledEvents"][0] or
            binary_event.get("Name")!=name):
        raise ValueError("Binary event does not match validated original tree")
    end=reader.pos
    if not end>entry_start or end>len(raw):
        raise ValueError("Invalid event NBT span")
    changed=raw[:count_offset]+struct.pack(">i",0)+raw[end:]
    verify=NBTReader(changed)
    replaced_root=verify.root()
    if verify.pos!=len(changed):
        raise ValueError("New NBT has trailing bytes")
    new_state=saved_schedule(replaced_root)
    if new_state["tag_state"]!="EMPTY" or new_state["count"]!=0:
        raise ValueError("Reward schedule not isolated")
    # Exact semantic preservation of all other level.dat data.
    import copy
    expected=copy.deepcopy(root)
    expected["Data"]["ScheduledEvents"]=[]
    if replaced_root!=expected:
        raise ValueError("Unrelated NBT semantics modified")
    return gzip.compress(changed,mtime=0)

def atomic_write(path,content):
    with tempfile.NamedTemporaryFile(dir=path.parent,prefix=".turnbound-saved-",delete=False) as f:
        temp=Path(f.name)
        f.write(content)
    try:
        os.replace(temp,path)
    finally:
        temp.unlink(missing_ok=True)

def apply(world,source_sha=LEVEL_SHA,event=EVENT_ID,tick=EVENT_TICK,game_time=ORIGINAL_TIME):
    world,level=check_copy(world,source_sha)
    receipt=world/RECEIPT
    backup=world/BACKUP
    if receipt.exists() or backup.exists():
        raise ValueError("Existing saved-event preview/backup must be restored")
    original=level.read_bytes()
    patched=preview_bytes(original,source_sha,event,tick,game_time)
    # Original backup and durable receipt exist before any modification.
    backup.write_bytes(original)
    if file_sha(backup)!=source_sha:
        raise ValueError("Original level.dat backup verification failed")
    info={"schema":1,"status":"OFFLINE_SAVED_REWARD_PREVIEW",
          "event_id":event,"event_count":1,"original_level_sha256":source_sha,
          "patched_level_sha256":digest(patched),
          "gameplay_verified":False,"original_drehmal_code_reused":False}
    receipt.write_text(json.dumps(info,indent=2)+"\n",encoding="utf-8")
    atomic_write(level,patched)
    if file_sha(level)!=info["patched_level_sha256"]:
        raise ValueError("Patched copied level.dat SHA mismatch")
    return info

def restore(world,original_sha=LEVEL_SHA):
    world,level=check_copy(world,original_sha)
    receipt,backup=world/RECEIPT,world/BACKUP
    if not receipt.is_file() or not backup.is_file():
        raise ValueError("No saved event preview backup")
    info=json.loads(receipt.read_text(encoding="utf-8"))
    if (info.get("schema")!=1 or info.get("status")!="OFFLINE_SAVED_REWARD_PREVIEW"
            or info.get("original_level_sha256")!=original_sha):
        raise ValueError("Unknown saved event preview receipt")
    if file_sha(level) not in (original_sha,info["patched_level_sha256"]):
        raise ValueError("Working world was modified after preview; refusing overwrite")
    if file_sha(backup)!=original_sha:
        raise ValueError("Original level.dat backup corrupted")
    atomic_write(level,backup.read_bytes())
    if file_sha(level)!=original_sha:
        raise ValueError("Exact level.dat restore verification failed")
    receipt.unlink()
    backup.unlink()
    return {"status":"ORIGINAL_LEVEL_EXACTLY_RESTORED",
            "event_count":info["event_count"],"level_sha256":file_sha(level)}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("action",choices=("inspect","apply","restore"))
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    args=p.parse_args()
    world=args.world_copy.resolve(strict=True)
    output=args.output.resolve()
    if output==world or world in output.parents:
        p.error("Output must be outside world copy")
    if args.action=="restore":
        result=restore(world)
    elif args.action=="apply":
        result=apply(world)
    else:
        _,level=check_copy(world)
        patched=preview_bytes(level.read_bytes())
        result={"status":"OFFLINE_PREFLIGHT_ONLY","event_count":1,
                "original_level_sha256":file_sha(level),
                "predicted_patch_sha256":digest(patched),
                "world_changed":False}
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":result["status"],"events":result["event_count"],
                      "original_drehmal_code_reused":False},ensure_ascii=False))

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,TypeError,KeyError,NBTError,EOFError) as error:
        print("Saved scheduler copy-only separation blocked: "+str(error),file=sys.stderr)
        sys.exit(2)
