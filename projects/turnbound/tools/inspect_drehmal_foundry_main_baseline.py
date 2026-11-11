#!/usr/bin/env python3
"""Drehmal 2.2.2f original Foundry three-lever gate forensic probe, READ ONLY.

Hash-checks official release. Compares entire clone source/destination palette
states, and summarizes the exact branching shape of original levers/main.
Never emits original mcfunction lines, NBT, map bytes or original commands.
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

import inspect_drehmal_teleport_footprints as official
from audit_drehmal_starter_mechanisms import World, palette_index

GATE_SRC=(-3274,134,1557)
GATE_DST=(-3278,156,1576)
GATE_DIMS=(5,9,2)
FUNCTIONS=("foundry:levers/main","foundry:levers/door",
           "foundry:levers/door1","foundry:levers/1_check",
           "foundry:levers/2_check","foundry:levers/3_check",
           "foundry:levers/1","foundry:levers/2","foundry:levers/3")
REFERENCE_ID=re.compile(r"(?<![a-z_])function\s+([a-z0-9_.-]+:[a-z0-9_./-]+)")
NUMERIC_MATCH=re.compile(r"\bmatches\s+(-?\d+)(?:\.\.(-?\d+)?)?")
INT_ONLY=re.compile(r"^-?\d+$")
ALLOWED_OPERATORS={"=","<",">","<=",">="}
AIR={"minecraft:air","minecraft:cave_air","minecraft:void_air"}
GATE_IDS={"minecraft:bedrock","minecraft:basalt","minecraft:polished_basalt","minecraft:smooth_basalt"}


def state(view, at):
    x,y,z=at
    root=view.chunk(x,z)
    sec=next((v for v in root.get("sections",[])
              if isinstance(v,dict) and v.get("Y")==y//16),None)
    if sec is None or not sec.get("block_states"):
        return ("minecraft:air",())
    blocks=sec["block_states"]
    idx=((y&15)<<8)|((z&15)<<4)|(x&15)
    palette=blocks["palette"][palette_index(blocks["palette"],blocks.get("data"),idx)]
    props=palette.get("Properties",{})
    return (palette["Name"],tuple(sorted(props.items())))


def source_destination_comparison(world):
    view=World(world)
    same_id=0;same_full=0;changes=0
    src_types=collections.Counter();dst_types=collections.Counter()
    mismatched_id=collections.Counter()
    source_air=dest_air=0
    for dx in range(GATE_DIMS[0]):
        for dy in range(GATE_DIMS[1]):
            for dz in range(GATE_DIMS[2]):
                a=state(view,(GATE_SRC[0]+dx,GATE_SRC[1]+dy,GATE_SRC[2]+dz))
                b=state(view,(GATE_DST[0]+dx,GATE_DST[1]+dy,GATE_DST[2]+dz))
                src_types[a[0]]+=1;dst_types[b[0]]+=1
                source_air+=a[0] in AIR;dest_air+=b[0] in AIR
                same_id+=a[0]==b[0]
                same_full+=a==b
                changes+=a!=b
                if a[0]!=b[0]:mismatched_id[(a[0],b[0])]+=1
    count=GATE_DIMS[0]*GATE_DIMS[1]*GATE_DIMS[2]
    if count!=90 or sum(src_types.values())!=90 or sum(dst_types.values())!=90:
        raise ValueError("Original clone extent drifted")
    return {
        "source_min":[*GATE_SRC],
        "destination_min":[*GATE_DST],
        "dimensions":[*GATE_DIMS],
        "count":count,
        "same_block_id_before_clone":same_id,
        "same_full_blockstate_before_clone":same_full,
        "source_destination_fullstate_differences":changes,
        "source_air_cells":source_air,
        "destination_air_cells":dest_air,
        "source_basalt_bedrock_cells":sum(v for k,v in src_types.items() if k in GATE_IDS),
        "destination_basalt_bedrock_cells":sum(v for k,v in dst_types.items() if k in GATE_IDS),
        "source_block_id_histogram":dict(sorted(src_types.items())),
        "destination_block_id_histogram":dict(sorted(dst_types.items())),
        "different_id_transition_counts":[
            {"from":a,"to":b,"cells":n} for (a,b),n in sorted(mismatched_id.items())
        ],
        "runtime_clone_executed":False,
        "actual_door_traversal_verified":False,
    }


def classify_conditional(line):
    """One redacted, inspectable condition shape per original code line."""
    text=line.lstrip("/")
    parts=text.split()
    functions=sorted(set(REFERENCE_ID.findall(text)))
    shape=[]
    for i,token in enumerate(parts):
        if token in ("if","unless") and i+1<len(parts):
            next_type=parts[i+1]
            typ=next_type if next_type in ("score","block","entity","data","predicate","biome",
                                           "dimension","blocks","loaded","function") else "other"
            row={"polarity":token,"condition_type":typ}
            if typ=="score":
                # Parse only operator type and integer match range, not any
                # original scoreboard owner or objective names.
                trailing=parts[i+2:]
                if "matches" in trailing:
                    m=NUMERIC_MATCH.search(" ".join(trailing))
                    if m: row["matches_integer_range"]=[m.group(1),m.group(2)]
                for op in ALLOWED_OPERATORS:
                    if op in trailing:
                        row["compare_operator"]=op
                        break
            shape.append(row)
    return {"root_operation":parts[0] if parts else "",
            "conditional_clauses":shape,
            "function_reference_ids":functions,
            "has_execute_run":" run " in " "+text+" ",
            "has_clone":bool(re.search(r"\bclone\b",text)),
            "has_schedule":bool(re.search(r"\bschedule\b",text)),
            }


def function_inventory(pack):
    with zipfile.ZipFile(pack) as z:
        contents=z.namelist()
        if len(contents)!=len(set(contents)):
            raise ValueError("Duplicate source pack members")
        result={}
        for fid in FUNCTIONS:
            namespace,path=fid.split(":",1)
            member=f"data/{namespace}/functions/{path}.mcfunction"
            if contents.count(member)!=1:
                raise ValueError("Missing original Foundry function "+fid)
            raw=z.read(member)
            if len(raw)>400_000:
                raise ValueError("Unbounded original function content")
            try: lines=raw.decode("utf-8").splitlines()
            except UnicodeDecodeError as e:raise ValueError("Original function not UTF-8") from e
            statements=[]
            for line in lines:
                t=line.strip()
                if not t or t.startswith("#"):continue
                statements.append(classify_conditional(t))
            result[fid]={
                "present":True,
                "statement_count":len(statements),
                "direct_command_category_counts":dict(sorted(collections.Counter(
                   s["root_operation"] for s in statements).items())),
                "literal_callees":sorted({v for s in statements for v in s["function_reference_ids"]}),
                "condition_shapes":[
                  {"clauses":s["conditional_clauses"],"callees":s["function_reference_ids"]}
                  for s in statements if s["conditional_clauses"]
                ],
                "has_clone":any(s["has_clone"] for s in statements),
                "has_schedule":any(s["has_schedule"] for s in statements),
                # Do not publish originals, even if they look short.
                "original_command_contents_exported":False,
            }
    return result



def trace_foundry_door_callers(pack):
    """Metadata only. Do not export original command texts, map data or score names."""
    watch=("foundry:levers/door","foundry:levers/door1")
    incoming={key:set() for key in watch}
    advancement_actions={}
    score_actions={}
    branches=[]
    original_count=0
    with zipfile.ZipFile(pack) as source:
        for name in source.namelist():
            if not name.startswith("data/") or not name.endswith(".mcfunction") or "/functions/" not in name:
                continue
            namespace,path=name[5:].split("/functions/",1)
            func=namespace+":"+path.removesuffix(".mcfunction")
            original_count+=1
            lines=source.read(name).decode("utf-8").splitlines()
            for raw in lines:
                line=raw.strip().lstrip("/")
                if not line or line.startswith("#"):
                    continue
                for target in watch:
                    if re.search(r"\bfunction\s+"+re.escape(target)+r"(?![a-z0-9_./-])",line):
                        incoming[target].add(func)
                if func=="foundry:levers/main":
                    match=re.search(r"\b(if|unless)\s+score\s+(\S+)\s+(\S+)\s+matches\s+(-?\d+)(?:\.\.(-?\d+)?)?",line)
                    if match:
                        post=line.split(" run ",1)[1].split() if " run " in line else []
                        branches.append({"match":int(match.group(4)),
                                         "polarity":match.group(1),
                                         "redacted_target_key":(match.group(2),match.group(3)),
                                         "run_root":post[0] if post else None,
                                         "run_operation":post[2] if len(post)>2 and post[:2]==["scoreboard","players"] else None})
                if func in ("foundry:levers/1_check","foundry:levers/2_check","foundry:levers/3_check") and line.startswith("advancement "):
                    advancement_actions.setdefault(func,[]).append(line.split()[1])
                if func in ("foundry:levers/1","foundry:levers/2","foundry:levers/3") and line.startswith("scoreboard players "):
                    score_actions.setdefault(func,[]).append(line.split()[2])
    if len(branches)!=3 or {x["match"] for x in branches}!={1,2,3}:
        raise ValueError("Pinned main matches 1/2/3 drifted")
    unique=len(set(x["redacted_target_key"] for x in branches))
    for x in branches: x.pop("redacted_target_key")
    return {"original_function_files_scanned":original_count,
            "door_literal_callers":{k:sorted(v) for k,v in incoming.items()},
            "main_condition_operations":branches,
            "main_condition_distinct_score_keys":unique,
            "check_advancement_actions":advancement_actions,
            "lever_score_actions":score_actions,
            "nonliteral_or_command_block_callers_excluded":True,
            "original_commands_exported":False,
            "runtime_callback_execution_proven":False}


def inspect(world):
    world=Path(world).resolve(strict=True)
    if (world/"turnbound-world.marker").exists():
        raise ValueError("Activated game world forbidden")
    official.check_official_source(world)
    comparison=source_destination_comparison(world)
    funcs=function_inventory(world/"datapacks"/"hi_drehmal.zip")
    official.check_official_source(world)
    return {
        "status":"OFFICIAL_FOUNDRY_LEVER_GATE_BASELINE_AND_MAIN_CONDITIONS_READ_ONLY",
        "source_release":"Drehmal APOTHEOSIS 2.2.2f Minecraft 1.20.1",
        "clone_original_source_destination":comparison,
        "foundry_lever_function_condition_shapes":funcs,
        "foundry_door_caller_trace":trace_foundry_door_callers(world/"datapacks"/"hi_drehmal.zip"),
        "original_map_modified":False,
        "original_commands_or_nbt_exported":False,
        "dynamic_score_conditions_verified":False,
        "physical_gate_opened_verified":False,
        "original_wave_system_disabled":False,
        "map_separation_complete":False,
    }


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--original",required=True,type=Path)
    parser.add_argument("--output",required=True,type=Path)
    args=parser.parse_args()
    source=args.original.resolve(strict=True)
    out=args.output.resolve()
    if out==source or source in out.parents:parser.error("No reports inside original source map")
    data=inspect(source)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(data,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({"status":data["status"],
        "clone_source_dest_different_full_blocks":
           data["clone_original_source_destination"]["source_destination_fullstate_differences"],
        "main_conditional_statements":len(data["foundry_lever_function_condition_shapes"]
            ["foundry:levers/main"]["condition_shapes"]),
        "map_separation_complete":False}))


if __name__=="__main__":
    try:main()
    except (OSError,ValueError,TypeError,KeyError,zipfile.BadZipFile) as e:
        print("Foundry source-only baseline REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)
