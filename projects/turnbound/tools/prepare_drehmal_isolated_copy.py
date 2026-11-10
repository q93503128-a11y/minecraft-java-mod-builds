#!/usr/bin/env python3
"""Compose four or five already-verified Drehmal 2.2.2f isolation boundaries on a COPY.

Actions:
  inspect  - strict read-only source preflight, including all modified artifacts
  prepare  - apply four or five boundaries, verify patched copy, RETAIN it in non-playable stage
  verify   - read-only verification of retained isolated COPY
  restore  - reverse boundaries, verify the original hashes and remove backup receipts

NO TURNBOUND world activation, playable/released map claim, source-world edits,
original code reuse, or third-party asset publication is performed.
"""
from __future__ import annotations
import argparse
import json
import sys
from pathlib import Path

import drehmal_auto_root_preview as roots
import preview_drehmal_weapon_rewards as weapons
import patch_drehmal_terminus_commands as towers
import suppress_drehmal_gamemode_commands as modes
import preview_drehmal_saved_reward_schedule as saved
from inspect_drehmal_saved_events import saved_schedule, read_gzip_nbt

LEVEL_SHA=saved.LEVEL_SHA
ORIGINAL_PACK_SHA=weapons.ORIGINAL_PACK_SHA
ROOT_RECEIPT=roots.RECEIPT_FILE
WEAPON_RECEIPT=weapons.RECEIPT
TOWER_RECEIPT=towers.RECEIPT
GAMEMODE_RECEIPT=modes.RECEIPT
SAVED_RECEIPT=saved.RECEIPT
STAGES=("weapon","root","tower","gamemode","saved")

def receipt(world,name):
    path=world/name
    if not path.is_file() or path.is_symlink():
        raise ValueError("Missing or invalid offline receipt: "+name)
    return json.loads(path.read_text(encoding="utf-8"))

def no_other_stage(world):
    for name in (ROOT_RECEIPT,WEAPON_RECEIPT,TOWER_RECEIPT,GAMEMODE_RECEIPT,SAVED_RECEIPT,
                 roots.BACKUP_DIR,weapons.BACKUP,towers.BACKUP,modes.BACKUP,saved.BACKUP):
        if (world/name).exists():
            raise ValueError("Found pre-existing patch/backup; preserve copy and investigate: "+name)

def baseline(world):
    world=Path(world).resolve(strict=True)
    # Before any scheduled event edit, every original-pinned parser must agree.
    saved.check_copy(world)
    roots.check_staged_world(world,LEVEL_SHA)
    weapons.staged(world)
    towers.validate_world(world)
    if saved.file_sha(world/"level.dat")!=LEVEL_SHA:
        raise ValueError("Original save hash changed")
    if weapons.file_hash(world/"datapacks"/weapons.PACK)!=ORIGINAL_PACK_SHA:
        raise ValueError("Original datapack ZIP hash changed")
    return world

def load_evidence(aux,graph,positions):
    targets=weapons.load_evidence(aux,graph)
    triggers=towers.read_targets(positions)
    if len(targets)!=39 or len(triggers)!=62:
        raise ValueError("Official source analysis scope unexpected")
    return targets,triggers

def inspect(world,targets,triggers,game_modes=None):
    world=baseline(world)
    no_other_stage(world)
    roots.verify_source(world/"datapacks"/roots.PACK_NAME,roots.TAGS)
    weapons.repackage(world/"datapacks"/weapons.PACK,targets)
    changed=towers.prepare(world,triggers)
    saved.preview_bytes((world/"level.dat").read_bytes())
    if game_modes is not None:
        modes.preflight(world,game_modes)
    if len(changed)!=30:
        raise ValueError("Original tower region count drifted")
    return {
        "status":"COPY_PREFLIGHT_NO_WRITES",
        "original_level_sha256":LEVEL_SHA,
        "original_hi_drehmal_sha256":ORIGINAL_PACK_SHA,
        "weapon_advancements":len(targets),
        "automatic_function_roots":3,
        "terminus_command_blocks":len(triggers),
        "terminus_regions":len(changed),
        "saved_reward_schedules":1,
        "direct_gamemode_commands":len(game_modes) if game_modes is not None else 0,
        "world_changed":False,
        "gameplay_tested":False,
    }

def verify(world,require_all=True):
    world=Path(world).resolve(strict=True)
    # Saved scheduler is the ONLY stage authorized to modify level.dat.
    saved.check_copy(world)
    rweapon=receipt(world,WEAPON_RECEIPT)
    rroot=receipt(world,ROOT_RECEIPT)
    rtower=receipt(world,TOWER_RECEIPT)
    rsaved=receipt(world,SAVED_RECEIPT)
    rmode=modes.verify(world) if (world/GAMEMODE_RECEIPT).exists() else None
    pack=world/"datapacks"/weapons.PACK
    # The root stage operates on a ZIP which already contains the weapon preview.
    if rweapon.get("advancement_count")!=39 or rroot.get("schema")!=1:
        raise ValueError("Unexpected package patch receipts")
    if rroot.get("original_pack_sha256")!=rweapon.get("pack_patched_sha256"):
        raise ValueError("Root preview was not applied after weapon preview")
    if weapons.file_hash(pack)!=rroot.get("patched_pack_sha256"):
        raise ValueError("Current datapack does not match composite isolation")
    with __import__("zipfile").ZipFile(pack) as archive:
        for path in roots.TAGS:
            if json.loads(archive.read(path))["values"]:
                raise ValueError("A load/tick root is still active")
    if (rtower.get("mode")!="OFFLINE_TERMINUS_ONLY"
            or rtower.get("target_count")!=62
            or len(rtower.get("regions",{}))!=30):
        raise ValueError("Tower preview receipt mismatch")
    for rel,hashes in rtower["regions"].items():
        active=hashes["after"]
        if rmode and rel in rmode["regions"]:
            if rmode["regions"][rel]["before"]!=active:
                raise ValueError("Tower and gamemode patch order/region SHA mismatch: "+rel)
            active=rmode["regions"][rel]["after"]
        if towers.file_sha(world/rel)!=active:
            raise ValueError("Tower region patch lost integrity: "+rel)
    if (rsaved.get("status")!="OFFLINE_SAVED_REWARD_PREVIEW"
            or rsaved.get("event_count")!=1
            or saved.file_sha(world/"level.dat")!=rsaved.get("patched_level_sha256")):
        raise ValueError("Saved reward callback patch mismatch")
    snapshot=saved_schedule(read_gzip_nbt(world/"level.dat"))
    if snapshot["count"]!=0 or snapshot["tag_state"]!="EMPTY":
        raise ValueError("Saved original gift callback is still queued")
    if (world/"turnbound-world.marker").exists():
        raise ValueError("An offline preview was activated as a playable world")
    return {
        "status":"FIVE_BOUNDARIES_PRESENT_VERIFIED" if rmode else "FOUR_BOUNDARIES_PRESENT_VERIFIED",
        "isolated_copy_retained":True,
        "world_activated":False,
        "map_separation_complete":False,
        "weapon_advancements":39,
        "automatic_function_roots":3,
        "terminus_command_blocks":62,
        "terminus_region_files":30,
        "saved_reward_schedules":1,
        "direct_gamemode_commands":rmode["target_count"] if rmode else 0,
        "gamemode_region_files":rmode["region_count"] if rmode else 0,
        "current_level_sha256":rsaved["patched_level_sha256"],
        "current_datapack_sha256":rroot["patched_pack_sha256"],
        "original_drehmal_code_reused":False,
        "playtested":False,
        "multi_player_tested":False,
        "known_remaining_original_entrypoints":True,
    }

def prepare(world,targets,triggers,game_modes=None):
    inspect(world,targets,triggers,game_modes)
    # PREPARED COPY is intentionally left on disk after success; never publish it.
    # On interruption, leave all patch receipts/backups for controlled recovery.
    weapons.apply(world,targets)
    roots.apply(world)
    towers.apply(world,triggers)
    if game_modes is not None:
        modes.apply(world,game_modes)
    saved.apply(world)
    return verify(world)

def restore(world):
    # Restore saved level FIRST: all subsequent restorers demand original level hash.
    # A failed restore leaves receipts and backups intact for manual recovery.
    verify(world)
    had_gamemode=(world/GAMEMODE_RECEIPT).exists()
    saved.restore(world)
    if had_gamemode:
        modes.restore(world)
    towers.restore(world)
    roots.restore(world)
    weapons.restore(world)
    world=baseline(world)
    no_other_stage(world)
    return {"status":"ALL_FIVE_BOUNDARIES_RESTORED_EXACTLY" if had_gamemode else "ALL_FOUR_BOUNDARIES_RESTORED_EXACTLY",
            "original_level_sha256":saved.file_sha(world/"level.dat"),
            "original_datapack_sha256":weapons.file_hash(world/"datapacks"/weapons.PACK),
            "original_world_preserved":True,"playtested":False}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("action",choices=("inspect","prepare","verify","restore"))
    p.add_argument("--world-copy",type=Path,required=True)
    p.add_argument("--aux",type=Path)
    p.add_argument("--graph",type=Path)
    p.add_argument("--positions",type=Path)
    p.add_argument("--include-gamemode",action="store_true")
    p.add_argument("--output",type=Path,required=True)
    a=p.parse_args()
    world=a.world_copy.resolve(strict=True)
    out=a.output.resolve()
    if out==world or world in out.parents:
        p.error("Output MUST be outside copied world")
    if a.action in ("inspect","prepare"):
        if not all((a.aux,a.graph,a.positions)):
            p.error("--aux, --graph, --positions are mandatory for inspect/prepare")
        targets,triggers=load_evidence(a.aux,a.graph,a.positions)
        game_modes=modes.read_targets(a.aux) if a.include_gamemode else None
        result=inspect(world,targets,triggers,game_modes) if a.action=="inspect" else prepare(world,targets,triggers,game_modes)
    elif a.action=="verify":
        result=verify(world)
    else:
        result=restore(world)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":result["status"],"map_separation_complete":False,
                      "playtested":False},ensure_ascii=False))

if __name__=="__main__":
    try:
        main()
    except (ValueError,OSError,KeyError,TypeError,__import__("zipfile").BadZipFile) as e:
        print("Combined original-world isolation REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)
