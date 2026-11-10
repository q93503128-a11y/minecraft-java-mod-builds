#!/usr/bin/env python3
"""Read-only, SHA-pinned original Drehmal function effect audit for 12 callbacks.

Parses ORIGINAL mcfunction command text in RAM only, and outputs operation/role
counts and source IDs, never original source commands, NBT, selectors, map files,
or item/reward bodies. Static evidence is NOT permission to disable machinery.
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

from audit_drehmal_auxiliary_sources import PACKS, FUNCTION_PATH, command_shape
from audit_drehmal_objective_storage_dependencies import parse_command
from review_drehmal_callback_activation_boundaries import traverse, merge_schedule_edges

GRAPH_SHA = "e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826"
AUX_SHA = "9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef"
CROSS_SHA = "221843d2a2c7ea6469a29ab26aab556c5bfe415db968560e2e56c53f04eaeef3"
LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
OBJECTIVE = "bowbladEffect"
EXTRA = frozenset((
    "data/foundry/advancements/2.json",
    "data/core/advancements/godmachine/godbutton.json",
    "data/players/advancements/terms/avsal.json",
))
FAMILIES = {
    "WORLD_BLOCK_OR_STORAGE": {"setblock","fill","clone","place","fillbiome","data","forceload"},
    "TRAVEL_OR_SPAWN": {"tp","teleport","spreadplayers","spawnpoint","setworldspawn"},
    "PLAYER_REWARD_OR_STATE": {"give","clear","loot","item","advancement","effect",
                               "attribute","experience","xp","recipe","gamemode"},
    "ENTITY_OR_COMBAT": {"summon","kill","damage","ride","tag","team"},
    "SCORE_OR_GLOBAL_STATE": {"scoreboard","trigger","bossbar","gamerule","time","weather"},
    "VISUAL_OR_FEEDBACK": {"title","tellraw","playsound","stopsound","particle","say"},
    "FUNCTION_OR_SCHEDULE_DISPATCH": {"function","schedule"},
}
ALIASES = {"minecraft:tp":"tp", "minecraft:teleport":"teleport"}
SUPPORTED = set().union(*FAMILIES.values())
MAX_IDS = 35

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def read_pinned(path, expected):
    raw = Path(path).read_bytes()
    if sha(raw) != expected:
        raise ValueError("Original evidence SHA differs")
    return json.loads(raw)

def operation(raw):
    s = raw.strip()
    if not s or s.startswith("#"):
        return None
    if s.startswith("$"):
        return "UNRESOLVED_DYNAMIC_COMMAND"
    shaped = command_shape(s)
    effective = shaped["effective"]
    if effective == "EXECUTE_WITHOUT_LITERAL_RUN":
        return "EXECUTE_WITHOUT_LITERAL_RUN"
    effective = ALIASES.get(effective, effective)
    return effective if effective in SUPPORTED else "UNRECOGNIZED_OR_UNSUPPORTED_COMMAND"

def family(op):
    for group, ops in FAMILIES.items():
        if op in ops: return group
    return "UNKNOWN_OR_EXECUTE_ONLY"

def summarize_function(raw):
    roles = collections.Counter()
    ops = collections.Counter()
    families = collections.Counter()
    checked_lines = 0
    for line in raw.decode("utf-8",errors="replace").splitlines():
        op = operation(line)
        if op is None:
            continue
        checked_lines += 1
        ops[op] += 1
        families[family(op)] += 1
        references, _ = parse_command(line)
        for objective, role in references:
            if objective == OBJECTIVE:
                roles[role] += 1
    return {
        "literal_command_line_count": checked_lines,
        "effective_operation_counts": dict(sorted(ops.items())),
        "effect_family_counts": dict(sorted(families.items())),
        "bowblade_objective_role_counts": dict(sorted(roles.items())),
        "unresolved_or_unsupported_lines": (
            ops.get("UNRESOLVED_DYNAMIC_COMMAND",0)
            + ops.get("UNRECOGNIZED_OR_UNSUPPORTED_COMMAND",0)
            + ops.get("EXECUTE_WITHOUT_LITERAL_RUN",0)),
    }

def closure(roots, graph, schedule):
    call = {name:set(node["calls"]) for name,node in graph.items()}
    merged = merge_schedule_edges(call,schedule)
    return traverse(roots,merged,set(graph))

def sources(world,graph):
    """Hash every source function in all four official packs; do not emit bodies."""
    seen=set()
    summaries={}
    for packname in PACKS:
        file=world/"datapacks"/packname
        if not file.is_file() or file.is_symlink():
            raise ValueError("Missing official source datapack ZIP")
        with zipfile.ZipFile(file) as archive:
            names=archive.namelist()
            if len(names)!=len(set(names)):
                raise ValueError("Duplicate source archive paths")
            for entry in names:
                match=FUNCTION_PATH.fullmatch(entry)
                if not match:
                    continue
                fid=match.group(1)+":"+match.group(2)
                meta=graph.get(fid)
                content=archive.read(entry)
                if (fid in seen or meta is None or meta["pack"]!=packname
                        or sha(content)!=meta["source_sha256"]):
                    raise ValueError("Unpinned, missing or duplicated original function body")
                seen.add(fid)
                summaries[fid]=summarize_function(content)
    if seen!=set(graph) or len(seen)!=18771:
        raise ValueError("Full original source function inventory mismatch")
    return summaries

def inspect(world,graph_payload,aux,cross):
    level=world/"level.dat"
    if not level.is_file() or level.is_symlink() or sha(level.read_bytes())!=LEVEL_SHA:
        raise ValueError("Original official world level.dat SHA mismatch")
    if (graph_payload.get("world_level_dat_sha256")!=LEVEL_SHA
        or graph_payload.get("function_count")!=18771
        or aux.get("level_sha256")!=LEVEL_SHA
        or aux.get("world_bytes_modified") is not False
        or cross.get("status")!="READ_ONLY_CALLBACK_DEFERRED_AND_MECHANISM_CROSS_REVIEW"
        or cross.get("total_original_nonweapon_callback_links")!=181
        or cross.get("review_priorities")!={"ALREADY_EIGHTH_OFFLINE_PREVIEW_NOT_PLAYABLE":8,
                                            "PRESERVE_UNTIL_MECHANISM_REVIEW":173}):
        raise ValueError("Original source metadata identity or callback coverage changed")
    index={r["id"]:r for r in graph_payload["functions"]}
    if len(index)!=18771:
        raise ValueError("Original function graph has duplicate IDs")
    schedule=collections.defaultdict(set)
    events=aux["datapack_json_and_schedule"]["function_schedule_literal_sites"]
    if len(events)!=672 or sum(row["literal_schedule_count"] for row in events)!=854:
        raise ValueError("Schedule metadata cohort changed")
    for item in events:
        fid=item["function_id"]
        if fid not in index or item["function_source_sha256"]!=index[fid]["source_sha256"]:
            raise ValueError("Original schedule origin hash mismatch")
        schedule[fid].update(item["scheduled_target_ids"])
    if len(schedule)!=672:
        raise ValueError("Schedule source count unexpectedly changed")
    existing={r["advancement_file"]:r for r in cross["callbacks"]}
    if len(existing)!=181:
        raise ValueError("Duplicate or missing 181 callback rows")
    from preview_drehmal_nonweapon_callbacks import TARGET_NAMES
    targets=TARGET_NAMES | EXTRA
    if len(targets)!=12 or not targets.issubset(existing):
        raise ValueError("Targeted 12 callback advancements not found")
    summaries=sources(world,index)
    bow_refs={fid:row["bowblade_objective_role_counts"] for fid,row in summaries.items()
              if row["bowblade_objective_role_counts"]}
    roles=collections.Counter()
    for r in bow_refs.values():
        roles.update(r)
    output=[]
    for path in sorted(targets):
        row=existing[path]
        fid=row["callback_id"]
        reached,unresolved=closure([fid],index,schedule)
        direct=summaries.get(fid)
        joined=collections.Counter()
        family_counts=collections.Counter()
        unresolved_lines=0
        role_counts=collections.Counter()
        flagged=[]
        for node in sorted(reached):
            value=summaries[node]
            joined.update(value["effective_operation_counts"])
            family_counts.update(value["effect_family_counts"])
            role_counts.update(value["bowblade_objective_role_counts"])
            unresolved_lines+=value["unresolved_or_unsupported_lines"]
            if (value["bowblade_objective_role_counts"]
                    or any(value["effect_family_counts"].get(t,0) for t in (
                        "WORLD_BLOCK_OR_STORAGE","TRAVEL_OR_SPAWN",
                        "SCORE_OR_GLOBAL_STATE","ENTITY_OR_COMBAT"))):
                flagged.append({
                    "function_id":node,
                    "effect_family_counts":value["effect_family_counts"],
                    "bowblade_objective_roles":value["bowblade_objective_role_counts"],
                })
        if direct is None and row["callback_id"] not in {"players:null","players:items/frenzy/kill"}:
            raise ValueError("Reviewed target callback source missing unexpectedly")
        output.append({
            "advancement_file":path,"callback_id":fid,
            "was_in_eighth_offline_experiment":path in TARGET_NAMES,
            "original_cross_review_risks":row["risk_reasons"],
            "source_function_sha256":index[fid]["source_sha256"] if fid in index else None,
            "direct_function_effects":direct,
            "combined_static_and_scheduled_reachable_functions":len(reached),
            "unresolved_function_refs":len(unresolved),
            "direct_and_indirect_operation_counts":dict(sorted(joined.items())),
            "direct_and_indirect_family_counts":dict(sorted(family_counts.items())),
            "direct_and_indirect_unknown_lines":unresolved_lines,
            "bowblade_objective_roles_in_reachable_functions":dict(sorted(role_counts.items())),
            "state_relevant_functions":flagged[:MAX_IDS],
            "state_relevant_function_count":len(flagged),
            "source_code_reused":False,"approved_for_removal":False,
            "actual_structure_function_tested":False,
        })
    bow=next(x for x in output if x["advancement_file"].endswith("/kill_with_bowblade.json"))
    if not bow["direct_function_effects"]["bowblade_objective_role_counts"]:
        raise ValueError("Pinned bowblade callback must explicitly reference bowbladEffect")
    if (bow["was_in_eighth_offline_experiment"] is not True
        or len(bow_refs)==0):
        raise ValueError("Bowblade risk scope unexpectedly absent")
    return {
        "status":"READ_ONLY_PINNED_ORIGINAL_CALLBACK_COMMAND_EFFECTS",
        "source_sha256":{"graph":GRAPH_SHA,"aux":AUX_SHA,"cross_review":CROSS_SHA},
        "original_function_bodies_sha_checked":len(summaries),
        "selected_advancement_callbacks":len(output),
        "selected_callback_effects":output,
        "bowblade_objective":OBJECTIVE,
        "bowblade_objective_all_verified_functions_count":len(bow_refs),
        "bowblade_objective_role_totals":dict(sorted(roles.items())),
        "bowblade_objective_function_ids_sample":sorted(bow_refs)[:MAX_IDS],
        "world_modified":False,
        "original_source_code_published":False,
        "original_code_reused":False,
        "automatic_removals_approved":0,
        "runtime_triggered":False,"structures_tested":False,
        "playtested":False,"multiplayer_tested":False,
        "limitations":[
            "Literal 1.20.1 mcfunction lines only: parser does not fully model execute nesting, NBT or macros.",
            "Effective operation counts are syntax evidence, not observed execution.",
            "Some score matches are conditional, read/write roles may be ambiguous.",
            "Source function SHA coverage does not validate all command blocks, JSON predicates or live facility mechanics.",
            "No playable map or original code redistribution authorization.",
        ]
    }

def main():
    p=argparse.ArgumentParser()
    for flag in ("world","graph","aux","cross","output"):
        p.add_argument("--"+flag,type=Path,required=True)
    a=p.parse_args()
    world=a.world.resolve(strict=True)
    out=a.output.resolve()
    if out==world or world in out.parents:
        p.error("No reports may be placed in the original world")
    result=inspect(world,
                   read_pinned(a.graph,GRAPH_SHA),
                   read_pinned(a.aux,AUX_SHA),
                   read_pinned(a.cross,CROSS_SHA))
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({"status":result["status"],
                      "sha_checked_functions":result["original_function_bodies_sha_checked"],
                      "callback_count":result["selected_advancement_callbacks"],
                      "bowblade_referencing_functions":result["bowblade_objective_all_verified_functions_count"],
                      "world_modified":False}))

if __name__=="__main__":
    try:main()
    except (ValueError,OSError,KeyError,TypeError,zipfile.BadZipFile) as e:
        print("Original Drehmal callback effect inspection REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)
