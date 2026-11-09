#!/usr/bin/env python3
"""Read-only original Drehmal objective/storage reference dependency inventory.

The output is forensic metadata, NOT a safe-delete list, NOT original code
reused by TURNBOUND, and NOT gameplay or mechanism verification.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

from audit_drehmal_auxiliary_sources import PACKS, region_dimension
from scan_drehmal_command_blocks import scan_world

LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
SAVED_SHA = "c9d4f9666088b9b14644df0c3bd6be9f11559dd1b279f8f45ece3afa6cf46992"
GRAPH_SHA = "e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826"
FN_PATH = re.compile(r"^data/([a-z0-9_.-]+)/functions/([a-z0-9_./-]+)\.mcfunction$")
NAME = r"[A-Za-z0-9_.-]+"
ENTITY = r"(?:\S+)"
SCORE_PATTERNS = [
    ("score_write", re.compile(r"\bscoreboard\s+players\s+(?:set|add|remove|enable)\s+" + ENTITY + r"\s+("+NAME+r")(?=\s|$)", re.I)),
    ("score_read", re.compile(r"\bscoreboard\s+players\s+get\s+" + ENTITY + r"\s+("+NAME+r")(?=\s|$)", re.I)),
    ("score_write", re.compile(r"\bscoreboard\s+players\s+reset\s+" + ENTITY + r"\s+("+NAME+r")(?=\s|$)", re.I)),
    ("score_operation", re.compile(r"\bscoreboard\s+players\s+operation\s+"+ENTITY+r"\s+("+NAME+r")\s+\S+\s+"+ENTITY+r"\s+("+NAME+r")(?=\s|$)", re.I)),
    ("score_condition", re.compile(r"\b(?:if|unless)\s+score\s+"+ENTITY+r"\s+("+NAME+r")\s+matches(?:\s|$)", re.I)),
    ("score_condition", re.compile(r"\b(?:if|unless)\s+score\s+"+ENTITY+r"\s+("+NAME+r")\s+(?:<=|>=|<|>|=)\s+"+ENTITY+r"\s+("+NAME+r")(?=\s|$)", re.I)),
    ("score_write", re.compile(r"\bstore\s+(?:result|success)\s+score\s+"+ENTITY+r"\s+("+NAME+r")(?=\s|$)", re.I)),
    ("score_trigger", re.compile(r"\btrigger\s+("+NAME+r")(?=\s|$)", re.I)),
    ("objective_admin", re.compile(r"\bscoreboard\s+objectives\s+(?:add|remove|modify)\s+("+NAME+r")(?=\s|$)", re.I)),
    ("objective_display", re.compile(r"\bscoreboard\s+objectives\s+setdisplay\s+\S+\s+("+NAME+r")(?=\s|$)", re.I)),
    ("score_component", re.compile(r'"objective"\s*:\s*"('+NAME+r')"', re.I)),
]
SELECTOR_SCORES = re.compile(r"\bscores\s*=\s*\{([^{}]*)\}",re.I)
SELECTOR_KEY = re.compile(r"(?:(?<=^)|(?<=,))\s*("+NAME+r")\s*=")
STORAGE = re.compile(r"\bstorage\s+([a-z0-9_.-]+:[a-z0-9_./-]+)", re.I)
WORLD_MOD = re.compile(r"\b(?:setblock|fill|clone|place|tp|teleport|summon|kill|spreadplayers|fillbiome)\b",re.I)
VISUAL_MOD = re.compile(r"\b(?:playsound|stopsound|particle|title|tellraw|bossbar)\b",re.I)
PLAYER_MOD = re.compile(r"\b(?:give|clear|gamemode|effect|experience|xp|advancement|recipe)\b",re.I)

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def parse_command(raw):
    if not isinstance(raw,str) or not raw.strip() or raw.lstrip().startswith("#"):
        return [],[]
    refs=set()
    for role, pattern in SCORE_PATTERNS:
        for match in pattern.finditer(raw):
            for index in range(1,match.lastindex+1):
                objective=match.group(index)
                if objective:
                    refs.add((objective,role))
    # Score selector conditions may be present in commands that contain no literal
    # scoreboard verb (e.g. execute as @e[scores={door=1..}] ...).
    for selector in SELECTOR_SCORES.findall(raw):
        for name in SELECTOR_KEY.findall(selector):
            refs.add((name,"selector_condition"))
    stores=sorted(set(STORAGE.findall(raw)))
    return sorted(refs),stores

def load_pinned(saved_path,graph_path):
    saved_raw=saved_path.read_bytes()
    graph_raw=graph_path.read_bytes()
    if sha(saved_raw)!=SAVED_SHA or sha(graph_raw)!=GRAPH_SHA:
        raise ValueError("Official metadata hash drift")
    saved=json.loads(saved_raw)
    graph=json.loads(graph_raw)
    if (saved.get("official_level_dat_sha256")!=LEVEL_SHA or
            graph.get("world_level_dat_sha256")!=LEVEL_SHA or
            saved.get("world_changed") is not False):
        raise ValueError("Official map identity mismatch")
    objectives=saved["scoreboard_objective_inventory"]["objectives"]
    if (len(objectives)!=539 or saved["scoreboard_objective_inventory"]["objective_count"]!=539 or
            len({x["objective_id"] for x in objectives})!=539):
        raise ValueError("Unexpected source saved scoreboard objective catalog")
    storages=saved["command_storage_files"]
    if len(storages)!=12:
        raise ValueError("Unexpected saved command storage file set")
    functions={f["id"]:f for f in graph["functions"]}
    if len(functions)!=graph["function_count"] or len(functions)!=18771:
        raise ValueError("Unexpected original function index")
    return saved,graph,functions

def world_check(world):
    world=Path(world).resolve(strict=True)
    level=world/"level.dat"
    if not level.is_file() or level.is_symlink() or sha(level.read_bytes())!=LEVEL_SHA:
        raise ValueError("Not the official 2.2.2f original save")
    names=sorted(p.name for p in (world/"datapacks").glob("*.zip"))
    if names!=sorted(PACKS):
        raise ValueError("Original verified datapack set differs")
    return world

def scan_original(world,functions):
    """No original line text or NBT is emitted. All source functions must hash match."""
    usage=collections.defaultdict(lambda:{"roles":collections.Counter(),"functions":set(),"blocks":set(),
                                          "function_structure":set(),"function_visual":set(),"function_player":set()})
    stores=collections.defaultdict(lambda:{"functions":set(),"blocks":set(),"write_sites":0,"read_sites":0})
    unknown=collections.Counter()
    matched_functions=set()
    total_cmds=0
    for packname in PACKS:
        pack=world/"datapacks"/packname
        if not pack.is_file() or pack.is_symlink():
            raise ValueError("Missing or symlinked datapack ZIP")
        with zipfile.ZipFile(pack) as archive:
            if len(archive.namelist())!=len(set(archive.namelist())):
                raise ValueError("Duplicate function or JSON member")
            for file in archive.namelist():
                mat=FN_PATH.fullmatch(file)
                if not mat:
                    continue
                fid=mat[1]+":"+mat[2]
                raw=archive.read(file)
                meta=functions.get(fid)
                if meta is None or meta["pack"]!=packname or meta["source_sha256"]!=sha(raw):
                    raise ValueError("Pinned function graph does not match original source: "+fid)
                if fid in matched_functions:
                    raise ValueError("Duplicate function definition")
                matched_functions.add(fid)
                lines=raw.decode("utf-8",errors="replace").splitlines()
                has_structure=any(WORLD_MOD.search(line) for line in lines if not line.lstrip().startswith("#"))
                has_visual=any(VISUAL_MOD.search(line) for line in lines if not line.lstrip().startswith("#"))
                has_player=any(PLAYER_MOD.search(line) for line in lines if not line.lstrip().startswith("#"))
                for line in lines:
                    objectives,storages=parse_command(line)
                    if not objectives and not storages:
                        continue
                    total_cmds+=1
                    for name,role in objectives:
                        entry=usage[name]
                        entry["roles"][role]+=1
                        entry["functions"].add(fid)
                        if has_structure:entry["function_structure"].add(fid)
                        if has_visual:entry["function_visual"].add(fid)
                        if has_player:entry["function_player"].add(fid)
                    for storage in storages:
                        item=stores[storage]
                        item["functions"].add(fid)
                        if re.search(r"\b(?:modify|merge|remove|store)\b",line,re.I):
                            item["write_sites"]+=1
                        else:
                            item["read_sites"]+=1
    if len(matched_functions)!=len(functions):
        raise ValueError("Function source inventory incomplete")
    blocks=scan_world(world)
    for block in blocks["command_blocks"]:
        refs,store_ids=parse_command(block["command"])
        if not refs and not store_ids:
            continue
        path=Path(block["file"]).resolve()
        if world not in path.parents:
            raise ValueError("Region outside source world")
        relative=path.relative_to(world).as_posix()
        coord=(block.get("x"),block.get("y"),block.get("z"))
        if not all(type(v) is int for v in coord):
            coord=None
        identifier=(region_dimension(relative),relative,block["chunk_slot"],tuple(coord) if coord else None,sha(block["command"].encode("utf-8")))
        for name,role in refs:
            entry=usage[name]
            entry["roles"][role]+=1
            entry["blocks"].add(identifier)
        for storage in store_ids:
            item=stores[storage]
            item["blocks"].add(identifier)
            if re.search(r"\b(?:modify|merge|remove|store)\b",block["command"],re.I):
                item["write_sites"]+=1
            else:
                item["read_sites"]+=1
    return usage,stores,blocks,total_cmds

def summarize(saved,usage,storage,blocks,total):
    catalog={x["objective_id"]:x["criteria"] for x in saved["scoreboard_objective_inventory"]["objectives"]}
    objectives=[]
    for name,criteria in sorted(catalog.items()):
        e=usage.get(name)
        roles=e["roles"] if e else collections.Counter()
        fun=e["functions"] if e else set()
        block=e["blocks"] if e else set()
        objectives.append({
            "objective_id":name,"criteria":criteria,
            "observed_reference_count":sum(roles.values()),
            "reference_roles":dict(sorted(roles.items())),
            "referencing_function_count":len(fun),
            "sample_function_ids":sorted(fun)[:20],
            "referencing_command_block_count":len(block),
            "sample_command_block_locations":[{"dimension":x[0],"region":x[1],
                "slot":x[2],"coordinates":x[3],"source_command_sha256":x[4]}
                for x in sorted(block,key=str)[:6]],
            "functions_also_containing_world_commands":len(e["function_structure"]) if e else 0,
            "functions_also_containing_visual_commands":len(e["function_visual"]) if e else 0,
            "functions_also_containing_player_gameplay_commands":len(e["function_player"]) if e else 0,
            "review":"PRESERVE_UNTIL_MANUAL_MECHANISM_REVIEW" if e and (
                e["function_structure"] or e["function_visual"] or block) else "NO_SAFE_DELETE_INFERENCE"
        })
    storage_records=[]
    for record in saved["command_storage_files"]:
        namespace=record["file_name"].removeprefix("command_storage_").removesuffix(".dat")
        refs=[(sid,e) for sid,e in storage.items() if sid.split(":",1)[0]==namespace]
        all_fun=set().union(*(e["functions"] for _,e in refs)) if refs else set()
        all_block=set().union(*(e["blocks"] for _,e in refs)) if refs else set()
        storage_records.append({
            "saved_file":record["file_name"],"file_sha256":record["sha256"],
            "bytes":record["bytes"],"namespace":namespace,
            "referenced_storage_ids":sorted({sid for sid,_ in refs}),
            "functions_referencing_namespace":len(all_fun),
            "sample_function_ids":sorted(all_fun)[:20],
            "command_blocks_referencing_namespace":len(all_block),
            "read_sites":sum(e["read_sites"] for _,e in refs),
            "write_sites":sum(e["write_sites"] for _,e in refs),
            "contents":"NOT_READ_NOT_DELETED",
            "review":"PRESERVE_UNTIL_STRUCTURE_AND_GAMEPLAY_DEPENDENCIES_VERIFIED"
        })
    used=sum(bool(x["observed_reference_count"]) for x in objectives)
    missing=[name for name in usage if name not in catalog]
    return {
        "status":"OFFICIAL_SOURCE_OBJECTIVE_STORAGE_REFERENCE_AUDIT",
        "release":"Drehmal: APOTHEOSIS 2.2.2f",
        "pinned_saved_metadata_sha256":SAVED_SHA,"pinned_function_graph_sha256":GRAPH_SHA,
        "source_level_dat_sha256":LEVEL_SHA,
        "saved_objective_count":len(objectives),"objectives_with_observed_command_references":used,
        "objectives_without_observed_command_references":len(objectives)-used,
        "referenced_objective_ids_not_in_saved_catalog":sorted(missing),
        "command_storage_file_count":len(storage_records),
        "referenced_storage_ids_without_saved_namespace_file":sorted(
            sid for sid in storage if sid.split(":",1)[0] not in {x["namespace"] for x in storage_records}),
        "scanned_command_blocks":len(blocks["command_blocks"]),
        "region_scan_warnings":len(blocks["warnings"]),
        "source_function_commands_with_identifiable_references":total,
        "objectives":objectives,"command_storage_files":storage_records,
        "limitations":[
            "This is a conservative syntax scan, not a complete Minecraft command parser.",
            "Dynamic macros, function tags, predicates, advancement JSON and indirect entity data references may be missed.",
            "Same-function world/visual/player operation flags do not prove an objective controls that mechanism.",
            "Names with zero observed references are NOT safe to remove; unscanned chunks or JSON may reference them.",
            "Only storage file namespaces, not stored NBT content, were inspected.",
            "Original code was read solely for evidence. No source lines or original data/code are exported or reused."
        ],
        "world_modified":False,"original_drehmal_code_reused":False,
        "gameplay_or_multiplayer_tested":False
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--world",type=Path,required=True)
    p.add_argument("--saved",type=Path,required=True)
    p.add_argument("--graph",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    opts=p.parse_args()
    world=world_check(opts.world)
    output=opts.output.resolve()
    if output==world or world in output.parents:
        p.error("Report must be outside source world")
    saved,graph,index=load_pinned(opts.saved,opts.graph)
    source_usage,storage_usage,blocks,total=scan_original(world,index)
    report=summarize(saved,source_usage,storage_usage,blocks,total)
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({k:report[k] for k in ("saved_objective_count",
        "objectives_with_observed_command_references","command_storage_file_count",
        "scanned_command_blocks","region_scan_warnings","world_modified")},ensure_ascii=False))

if __name__=="__main__":
    try:main()
    except (ValueError,OSError,KeyError,zipfile.BadZipFile) as e:
        print("OFFICIAL DEPENDENCY REVIEW REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)
