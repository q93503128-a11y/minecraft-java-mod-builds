#!/usr/bin/env python3
"""NINTH reversible COPY-ONLY isolation: primordial root reward -> players:sethost.

Based on confirmed official Fabric 1.20.1 two-player test #42817029748.
Changes only one advancement reward.function link in an offline seven-stage
Drehmal copy. No original author source or world pack is redistributed.
Not a general game-save activation, content reuse or facilities-playtest pass.
"""
from __future__ import annotations
import argparse
import json
import os
import sys
import zipfile
from pathlib import Path

import prepare_drehmal_isolated_copy as six
import verify_drehmal_start_chain_isolation as seven
import probe_drehmal_primordial_reward_runtime as selective

PACK=Path("datapacks/hi_drehmal.zip")
BACKUP="turnbound-primordial-root-ninth-backup.zip"
RECEIPT="turnbound-primordial-root-ninth-receipt.json"
EIGHTH_RECEIPT="turnbound-nonweapon-callback-eight-preview.json"
STATUS="NINTH_PRIMORDIAL_ROOT_REWARD_LINK_OFFLINE_PREVIEW"

def hash_bytes(b):return selective.digest(b)

def atomic_write(path,data):
    path=Path(path)
    if not path.is_file() or path.is_symlink():
        raise ValueError("Unsafe original pack/backup destination")
    replacement=path.with_name(path.name+".turnbound-ci-replace")
    if replacement.exists():
        raise ValueError("An interrupted atomic replacement already exists")
    replacement.write_bytes(data)
    if hash_bytes(replacement.read_bytes())!=hash_bytes(data):
        raise ValueError("Replacement bytes changed before atomic move")
    os.replace(replacement,path)
    if hash_bytes(path.read_bytes())!=hash_bytes(data):
        raise ValueError("Replacement bytes changed after atomic move")

def stage_gate(world):
    world=Path(world).resolve(strict=True)
    if (world/"turnbound-world.marker").exists():
        raise ValueError("Activated game world cannot be subject to offline preview")
    if (world/EIGHTH_RECEIPT).exists():
        raise ValueError("Ninth preview starts from original seven-stage, not eighth overlay")
    seven.verify(world)
    pack=world/PACK
    if pack.is_symlink() or not pack.is_file():
        raise ValueError("Missing/symlinked seventh-stage pack")
    return world,pack

def preflight(world):
    world,pack=stage_gate(world)
    if any((world/name).exists() for name in (BACKUP,RECEIPT)):
        raise ValueError("Existing ninth preview or interrupted recovery")
    before=pack.read_bytes()
    expected=six.receipt(world,six.ROOT_RECEIPT)["patched_pack_sha256"]
    if hash_bytes(before)!=expected:
        raise ValueError("Previous seventh-stage baseline pack SHA changed")
    after=selective.repack_without_primordial_reward(before)
    return before,after

def apply(world):
    world,pack=stage_gate(world)
    before,after=preflight(world)
    backup=world/BACKUP
    backup.write_bytes(before)
    if hash_bytes(backup.read_bytes())!=hash_bytes(before):
        raise ValueError("Reversible original source pack backup did not verify")
    record={
        "schema":1,"status":STATUS,
        "source_base_pack_sha256":hash_bytes(before),
        "result_pack_sha256":hash_bytes(after),
        "changed_original_advancement_id":selective.PRIMORDIAL_ID,
        "removed_reward_link_target_id":selective.PRIMORDIAL_REWARD,
        "modified_datapack_members":1,
        "original_function_code_unchanged":True,
        "all_other_original_pack_members_unchanged":True,
        "no_original_code_reuse_in_turnbound":True,
        "world_activated":False,
        "facility_mechanism_playtest_complete":False,
        "map_separation_complete":False
    }
    (world/RECEIPT).write_text(json.dumps(record,indent=2)+"\n",encoding="utf-8")
    atomic_write(pack,after)
    return verify(world)

def verify(world):
    world,pack=stage_gate(world)
    receipt,backup=world/RECEIPT,world/BACKUP
    if any(p.is_symlink() or not p.is_file() for p in (receipt,backup)):
        raise ValueError("Ninth preview receipt/backup unavailable or unsafe")
    doc=json.loads(receipt.read_text(encoding="utf-8"))
    if not (
        doc.get("schema")==1 and doc.get("status")==STATUS and
        doc.get("changed_original_advancement_id")==selective.PRIMORDIAL_ID and
        doc.get("removed_reward_link_target_id")==selective.PRIMORDIAL_REWARD and
        doc.get("modified_datapack_members")==1 and
        doc.get("original_function_code_unchanged") is True and
        doc.get("all_other_original_pack_members_unchanged") is True and
        doc.get("no_original_code_reuse_in_turnbound") is True and
        doc.get("world_activated") is False and
        doc.get("map_separation_complete") is False
    ):
        raise ValueError("Ninth preview receipt does not match intended isolation")
    before,after=backup.read_bytes(),pack.read_bytes()
    expected=six.receipt(world,six.ROOT_RECEIPT)["patched_pack_sha256"]
    if hash_bytes(before)!=expected or hash_bytes(before)!=doc.get("source_base_pack_sha256"):
        raise ValueError("Ninth preview baseline source bytes have drifted")
    if hash_bytes(after)!=doc.get("result_pack_sha256"):
        raise ValueError("Ninth preview modified pack bytes have drifted")
    if selective.repack_without_primordial_reward(before)!=after:
        raise ValueError("More than original primordial root reward link changed")
    return {**doc,"verified":True}

def restore(world):
    doc=verify(world)
    world,pack=stage_gate(world)
    atomic_write(pack,(world/BACKUP).read_bytes())
    if hash_bytes(pack.read_bytes())!=doc["source_base_pack_sha256"]:
        raise ValueError("Restored seventh-stage pack SHA differs")
    (world/RECEIPT).unlink()
    (world/BACKUP).unlink()
    seven.verify(world)
    expected=six.receipt(world,six.ROOT_RECEIPT)["patched_pack_sha256"]
    if hash_bytes(pack.read_bytes())!=expected:
        raise ValueError("Restored world does not match original verified seventh-stage pack")
    return {
        "status":"NINTH_PRIMORDIAL_ROOT_PREVIEW_RESTORED_EXACTLY_TO_SEVENTH",
        "original_seventh_stage_pack_sha256":expected,
        "original_function_code_unchanged":True,
        "other_original_pack_members_unchanged":True,
        "world_activated":False,
        "map_separation_complete":False,
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("action",choices=("inspect","apply","verify","restore"))
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    world=a.world_copy.resolve(strict=True)
    out=a.output.resolve()
    if out==world or world in out.parents:
        p.error("Derived reports must not be written inside an official world copy")
    if a.action=="inspect":
        before,after=preflight(world)
        result={"status":"NINTH_PRIMORDIAL_ROOT_PREFLIGHT_ONLY",
                "base_pack_sha256":hash_bytes(before),
                "proposed_pack_sha256":hash_bytes(after),
                "modified_datapack_members":1,
                "world_modified":False,
                "map_separation_complete":False}
    elif a.action=="apply":result=apply(world)
    elif a.action=="verify":result=verify(world)
    else:result=restore(world)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":result["status"],
                      "verified":result.get("verified",False),
                      "map_separation_complete":False}))

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,TypeError,KeyError,zipfile.BadZipFile) as e:
        print("Selective ninth original Drehmal boundary REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)
