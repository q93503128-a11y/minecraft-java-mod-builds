#!/usr/bin/env python3
"""Read-only original Drehmal bowbladEffect entrypoint and facility root review.

Pinned source reports only; static literal call/schedule roots are not proof of
live execution. Never edits original map, functions, datapacks or game code.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import sys
from pathlib import Path

GRAPH_SHA="e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826"
AUX_SHA="9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef"
EFFECT_SHA="5b90a636c23b9053e812facd1396c97327c8be4ccefa3e91d612e1845f448153"
LEVEL_SHA="44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
LOAD_TICK=frozenset(("core:init","core:main","entities:main"))
BOW_FUNCTIONS=frozenset((
    "core:init","entities:misc/arrow_data",
    "players:items/ultva_bowblade/has_effect",
    "players:items/ultva_bowblade/kill_entity","players:main"
))
BOW_REWARD="data/players/advancements/kill_with_bowblade.json"
FACILITY_ADVANCEMENTS={
    "core:scene/terminals/avsal_check":"data/players/advancements/terms/avsal.json",
    "foundry:levers/2_check":"data/foundry/advancements/2.json",
    "core:scene/godmachine/enable":"data/core/advancements/godmachine/godbutton.json",
}

def digest(raw):
    return hashlib.sha256(raw).hexdigest()

def pinned(path,checksum):
    raw=Path(path).read_bytes()
    if digest(raw)!=checksum:
        raise ValueError("Pinned metadata report hash drift: "+Path(path).name)
    return json.loads(raw)

def incoming_graph(functions,schedules):
    """Map each called/scheduled function ID to its literal known caller IDs."""
    back=collections.defaultdict(set)
    for source,meta in functions.items():
        for target in meta["calls"]:
            if target in functions:
                back[target].add((source,"function_call"))
    for item in schedules:
        for target in item["scheduled_target_ids"]:
            if target in functions:
                back[target].add((item["function_id"],"schedule_function"))
    return back

def evidence_paths(target,incoming,roots,max_nodes=18771):
    if target not in roots["function_ids"]:
        raise ValueError("Target absent from official source graph")
    queue=collections.deque([(target,(target,),())])
    visited={target}
    matched={"automatic":{},"advancement":{},"command_block":{}}
    while queue:
        here,reverse_path,reverse_kinds=queue.popleft()
        for category in matched:
            labels=roots[category].get(here)
            if labels is None:
                continue
            if here not in matched[category]:
                matched[category][here]={
                    "root_function_id":here,
                    "static_function_path":list(reversed(reverse_path)),
                    "edge_kinds":list(reversed(reverse_kinds)),
                    "source_count":len(labels),
                    "source_files":labels[:12] if category=="advancement" else [],
                }
        for parent,kind in sorted(incoming.get(here,())):
            if parent not in visited:
                visited.add(parent)
                queue.append((parent,reverse_path+(parent,),reverse_kinds+(kind,)))
        if len(visited)>max_nodes:
            raise ValueError("Reverse graph exceeds verified function count")
    return {
        "reverse_reachable_function_count":len(visited),
        "literal_known_roots":{
            key:[matched[key][name] for name in sorted(matched[key])] for key in matched
        },
        "possible_root_counts":{key:len(matched[key]) for key in matched},
        "unknown_dynamic_execution_excluded":True,
        "original_runtime_execution_proven":False,
    }

def review(graph,aux,effects):
    if (graph.get("world_level_dat_sha256")!=LEVEL_SHA or
            graph.get("function_count")!=18771 or
            aux.get("level_sha256")!=LEVEL_SHA or
            aux.get("world_bytes_modified") is not False or
            effects.get("status")!="READ_ONLY_PINNED_ORIGINAL_CALLBACK_COMMAND_EFFECTS" or
            effects.get("source_sha256",{}).get("graph")!=GRAPH_SHA or
            effects.get("source_sha256",{}).get("aux")!=AUX_SHA or
            effects.get("original_function_bodies_sha_checked")!=18771 or
            effects.get("bowblade_objective_all_verified_functions_count")!=5 or
            effects.get("bowblade_objective")!="bowbladEffect" or
            effects.get("automatic_removals_approved")!=0 or
            effects.get("world_modified") is not False):
        raise ValueError("Pinned original Drehmal evidence identity or provenance differs")
    functions={row["id"]:row for row in graph["functions"]}
    if len(functions)!=18771:
        raise ValueError("Duplicate source function IDs")
    if set(effects["bowblade_objective_function_ids_sample"])!=BOW_FUNCTIONS:
        raise ValueError("Verified five bowbladEffect function IDs drifted")
    schedules=aux["datapack_json_and_schedule"]["function_schedule_literal_sites"]
    blocks=aux["command_blocks"]["all_block_metadata"]
    advancements=aux["datapack_json_and_schedule"]["advancement_reward_records"]
    if (len(schedules)!=672 or
            sum(row["literal_schedule_count"] for row in schedules)!=854 or
            len(blocks)!=2232 or len(advancements)!=292):
        raise ValueError("Original entrypoint scope differs")
    for row in schedules:
        source=functions.get(row["function_id"])
        if source is None or source["source_sha256"]!=row["function_source_sha256"]:
            raise ValueError("Scheduled call source hash mismatch")
    reward_roots=collections.defaultdict(list)
    for row in advancements:
        if row["reward_function_id"]:
            reward_roots[row["reward_function_id"]].append(row["file"])
    block_roots=collections.defaultdict(list)
    for block in blocks:
        for fid in block["literal_function_ids"]:
            block_roots[fid].append((block["region_file"],block["command_sha256"]))
    roots={
        "function_ids":set(functions),
        "automatic":{fid:["original_load_or_tick_function_tag"] for fid in LOAD_TICK},
        "advancement":{fid:sorted(names) for fid,names in reward_roots.items()},
        "command_block":{fid:["original_literal_block"]*len(refs) for fid,refs in block_roots.items()},
    }
    back=incoming_graph(functions,schedules)
    bow={fid:evidence_paths(fid,back,roots) for fid in sorted(BOW_FUNCTIONS)}
    facilities={fid:evidence_paths(fid,back,roots) for fid in sorted(FACILITY_ADVANCEMENTS)}
    for fid,row in bow.items():
        counts=row["possible_root_counts"]
        if fid=="players:items/ultva_bowblade/kill_entity":
            refs=row["literal_known_roots"]["advancement"]
            if (counts!={"automatic":0,"advancement":1,"command_block":0} or
                    len(refs)!=1 or refs[0]["source_files"]!=[BOW_REWARD]):
                raise ValueError("Bowblade reward has unexpected original literal caller")
            row["known_entrypoint_after_eighth_offline_preview"]="ADVANCEMENT_REWARD_LINK_REMOVED_OFFLINE_ONLY"
        else:
            expected="core:init" if fid=="core:init" else "entities:main"
            refs=row["literal_known_roots"]["automatic"]
            if (counts!={"automatic":1,"advancement":0,"command_block":0} or
                    len(refs)!=1 or refs[0]["root_function_id"]!=expected):
                raise ValueError("Bowblade automatic chain has unexpected original literal root")
            row["known_entrypoint_after_eighth_offline_preview"]="LOAD_TICK_TAG_REMOVED_OFFLINE_ONLY"
    for fid,row in facilities.items():
        refs=row["literal_known_roots"]["advancement"]
        if (row["possible_root_counts"]!={"automatic":0,"advancement":1,"command_block":0}
                or len(refs)!=1 or refs[0]["source_files"]!=[FACILITY_ADVANCEMENTS[fid]]):
            raise ValueError("Original facility callback root differs")
        row["known_entrypoint_after_eighth_offline_preview"]="ORIGINAL_ADVANCEMENT_LINK_RETAINED"
    return {
        "status":"READ_ONLY_BOWBLADE_ORIGINAL_ENTRYPOINT_BOUNDARY_REVIEW",
        "source_sha256":{"graph":GRAPH_SHA,"aux":AUX_SHA,"effects":EFFECT_SHA},
        "total_reviewed_source_functions":18771,
        "source_original_function_tag_roots":sorted(LOAD_TICK),
        "original_advancement_reward_records":sum(bool(x["reward_function_id"]) for x in advancements),
        "original_literal_block_function_roots":sum(map(len,block_roots.values())),
        "schedule_source_functions":len({r["function_id"] for r in schedules}),
        "bowblade_score_objective":"bowbladEffect",
        "bowblade_function_entrypoints":bow,
        "retained_facility_callback_entrypoints":facilities,
        "bowblade_known_literal_roots_after_eighth_offline_isolation":0,
        "static_nonroot_execution_ruled_out":False,
        "dynamic_or_saved_execution_untested":True,
        "automatic_removals_approved":0,
        "world_modified":False,"original_code_reused":False,
        "playtested":False,"multiplayer_tested":False,
        "limitations":[
            "Original static call/schedule and literal command-block reference graph only; dynamic dispatch remains unknown.",
            "Removing the load/tick function tag does not erase function bodies or prevent other runtime calls.",
            "A reward link removed on a copy is not proof that all real gameplay activation paths are disabled.",
            "Original block root inventory contains potentially suppressed blocks; it is not a live-block inventory.",
            "Player join, advancement completion, facility doors/portals, redstone and multiplayer have not been tested.",
            "No original map/function body/third-party asset republished."
        ],
    }

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("graph","aux","effects","out"):
        p.add_argument("--"+name,type=Path,required=True)
    args=p.parse_args()
    inputs=(args.graph,args.aux,args.effects)
    out=args.out.resolve()
    if out in {item.resolve() for item in inputs}:
        p.error("Output may not overwrite pinned evidence")
    result=review(pinned(args.graph,GRAPH_SHA),pinned(args.aux,AUX_SHA),
                  pinned(args.effects,EFFECT_SHA))
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({
        "status":result["status"],
        "bowblade_function_count":len(result["bowblade_function_entrypoints"]),
        "retained_facility_callbacks":len(result["retained_facility_callback_entrypoints"]),
        "known_roots_in_eighth_copy":0,"playtested":False,
    }))

if __name__=="__main__":
    try:main()
    except (ValueError,OSError,TypeError,KeyError) as exc:
        print("Original bowblade entrypoint review REFUSED: "+str(exc),file=sys.stderr)
        sys.exit(2)
