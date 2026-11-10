#!/usr/bin/env python3
"""Cross-index the 181 original Drehmal advancement callbacks against function
calls, deferred schedule edges, command-block roots and sampled scoreboard/storage
references. Source metadata only. Never a safe-remove or original-code reuse list.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import sys
from pathlib import Path

import preview_drehmal_nonweapon_callbacks as eight

AUX_SHA = "9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef"
GRAPH_SHA = "e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826"
CALLBACK_SHA = eight.SOURCE_SHA
SCORE_SHA = "37b8ba75e77e87cac6b62c7c7ef8665c85035143e36d08b1465c2e92b04ec95d"
LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
EXPECTED = {"callbacks":181,"functions":18771,"command_blocks":2232,
            "schedule_sources":672,"schedule_sites":854,
            "objectives":539,"storages":12}

def digest(raw):
    return hashlib.sha256(raw).hexdigest()

def read_pinned(path,expected):
    raw=Path(path).read_bytes()
    if digest(raw)!=expected:
        raise ValueError("Source report SHA changed: "+Path(path).name)
    return json.loads(raw)

def traverse(roots,edges,known):
    pending=list(roots)
    visited=set()
    unresolved=set()
    while pending:
        fid=pending.pop()
        if fid in visited:
            continue
        if fid not in known:
            unresolved.add(fid)
            continue
        visited.add(fid)
        pending.extend(edges.get(fid,()))
    return visited,unresolved

def tag(row,categories,link_count,sample_count):
    triggers=set(row["trigger_types"])
    group=row["review_group"]
    reasons=set()
    if group in ("STRUCTURE_OR_PUZZLE_PRESERVE_REVIEW","WORLD_STATE_OR_TRAVEL_REVIEW"):
        reasons.add("STRUCTURE_OR_TRAVEL_CLASSIFIED")
    if "world_edit" in categories: reasons.add("FUNCTION_WORLD_EDIT")
    if "teleport" in categories: reasons.add("FUNCTION_TELEPORT")
    if link_count: reasons.add("PHYSICAL_COMMAND_ROOT_CAN_REACH_CALLBACK")
    if triggers & {"minecraft:location","minecraft:changed_dimension"}:
        reasons.add("LOCATION_OR_DIMENSION_ACTIVATION")
    if triggers & {"minecraft:impossible"}:
        reasons.add("EXPLICIT_GRANT_OR_OTHER_UNOBSERVED_ACTIVATION")
    if row["direct_function_callers"]:
        reasons.add("OTHER_DIRECT_FUNCTION_CALLERS")
    if row["reachable_from_original_auto_roots"]:
        reasons.add("REACHABLE_FROM_ORIGINAL_AUTO_ROOTS")
    if row["has_schedule_in_static_callgraph"]:
        reasons.add("SCHEDULE_IN_CALL_GRAPH")
    if sample_count:
        reasons.add("SAMPLED_OBJECTIVE_OR_STORAGE_REFERENCE")
    if not row["callback_exists"]:
        reasons.add("UNRESOLVED_CALLBACK_TARGET")
    if reasons:
        priority="PRESERVE_UNTIL_MECHANISM_REVIEW"
    elif row["advancement_file"] in eight.TARGET_NAMES:
        priority="ALREADY_EIGHTH_OFFLINE_PREVIEW_NOT_PLAYABLE"
    else:
        priority="FURTHER_REVIEW_ONLY_NOT_REMOVAL_APPROVAL"
    return priority,sorted(reasons)

def analyze(aux,graph,callbacks,score):
    if (aux.get("level_sha256")!=LEVEL_SHA or
            graph.get("world_level_dat_sha256")!=LEVEL_SHA or
            aux.get("world_bytes_modified") is not False or
            graph.get("function_count")!=EXPECTED["functions"] or
            callbacks.get("nonweapon_callback_records")!=EXPECTED["callbacks"] or
            callbacks.get("source_aux_sha256")!=AUX_SHA or
            callbacks.get("source_graph_sha256")!=GRAPH_SHA or
            score.get("saved_objective_count")!=EXPECTED["objectives"] or
            score.get("command_storage_file_count")!=EXPECTED["storages"] or
            score.get("source_level_dat_sha256")!=LEVEL_SHA or
            score.get("pinned_function_graph_sha256")!=GRAPH_SHA or
            score.get("world_modified") is not False):
        raise ValueError("Drehmal v2.2.2f evidence identity or scope differs")
    index={row["id"]:row for row in graph["functions"]}
    if len(index)!=EXPECTED["functions"]:
        raise ValueError("Incomplete/duplicated original source function graph")
    rows=callbacks["callbacks"]
    if len(rows)!=EXPECTED["callbacks"] or len({r["advancement_file"] for r in rows})!=len(rows):
        raise ValueError("Advancement callback source rows duplicated or missing")
    if not eight.TARGET_NAMES.issubset({r["advancement_file"] for r in rows}):
        raise ValueError("Eighth boundary missing from this source evidence")
    blocks=aux["command_blocks"]["all_block_metadata"]
    entries=aux["datapack_json_and_schedule"]["function_schedule_literal_sites"]
    if (len(blocks)!=EXPECTED["command_blocks"] or
            len(entries)!=EXPECTED["schedule_sources"] or
            sum(e["literal_schedule_count"] for e in entries)!=EXPECTED["schedule_sites"]):
        raise ValueError("Original command/schedule scope differs")
    calls={name:set(row["calls"]) for name,row in index.items()}
    schedule=collections.defaultdict(set)
    for e in entries:
        fid=e["function_id"]
        if (fid not in index or
                e["function_source_sha256"]!=index[fid]["source_sha256"] or
                e["literal_schedule_count"]<len(set(e["scheduled_target_ids"]))):
            raise ValueError("Unverified original scheduled-function origin")
        schedule[fid].update(e["scheduled_target_ids"])
    combined={fid:calls[fid]|schedule[fid] for fid in index}
    known=set(index)
    direct=collections.defaultdict(list)
    root_ids=set()
    for block in blocks:
        for fid in block["literal_function_ids"]:
            root_ids.add(fid)
            direct[fid].append({
                "dimension":block["dimension"],
                "region_file":block["region_file"],
                "coordinates":block["coordinates"],
            })
    callback_ids={r["callback_id"] for r in rows}
    block_routes=collections.defaultdict(list)
    # Only original literal command-block references are used as roots. This
    # does not imply that they execute, only possible static call paths.
    for fid in sorted(root_ids):
        reachable,_=traverse((fid,),combined,known)
        for target in reachable & callback_ids:
            block_routes[target].append(fid)
    sampled_objectives=collections.defaultdict(set)
    for objective in score["objectives"]:
        for fid in objective["sample_function_ids"]:
            sampled_objectives[fid].add(objective["objective_id"])
    sampled_storage=collections.defaultdict(set)
    for item in score["command_storage_files"]:
        for fid in item["sample_function_ids"]:
            sampled_storage[fid].add(item["namespace"])
    output=[]
    original_ids=collections.Counter(r["callback_id"] for r in rows)
    for row in sorted(rows,key=lambda x:x["advancement_file"]):
        fid=row["callback_id"]
        plain,unresolved_calls=traverse((fid,),calls,known)
        expanded,unresolved_deferred=traverse((fid,),combined,known)
        categories=set()
        for reachable in expanded:
            categories.update(index[reachable]["categories"])
        scheduled_sources=sorted(expanded & set(schedule))
        objectives=sorted(set().union(*(sampled_objectives[f] for f in expanded)))
        storage=sorted(set().union(*(sampled_storage[f] for f in expanded)))
        physical_roots=sorted(block_routes[fid])
        priority,reasons=tag(row,categories,len(physical_roots),len(objectives)+len(storage))
        if scheduled_sources:
            reasons=sorted(set(reasons)|{"DEFERRED_SCHEDULE_EDGE"})
            priority="PRESERVE_UNTIL_MECHANISM_REVIEW"
        if unresolved_deferred:
            reasons=sorted(set(reasons)|{"UNRESOLVED_STATIC_FUNCTION_EDGE"})
            priority="PRESERVE_UNTIL_MECHANISM_REVIEW"
        if original_ids[fid]>1:
            reasons=sorted(set(reasons)|{"CALLBACK_SHARED_BY_MULTIPLE_ADVANCEMENTS"})
        result={
            "advancement_file":row["advancement_file"],
            "callback_id":fid,
            "source_advancement_sha256":row["advancement_source_sha256"],
            "trigger_types":row["trigger_types"],
            "original_review_group":row["review_group"],
            "already_eighth_copy_only_preview":row["advancement_file"] in eight.TARGET_NAMES,
            "static_calls_reachable":len(plain),
            "combined_call_schedule_reachable":len(expanded),
            "additional_deferred_reachable":len(expanded-plain),
            "deferred_schedule_source_count":len(scheduled_sources),
            "unresolved_static_ids_count":len(unresolved_deferred),
            "function_categories_in_combined_graph":sorted(categories),
            "command_block_literal_root_paths_reaching_callback":physical_roots[:12],
            "command_block_literal_root_path_count":len(physical_roots),
            "sampled_score_objective_ids":objectives[:12],
            "sampled_score_objective_match_count":len(objectives),
            "sampled_storage_namespaces":storage[:12],
            "sampled_storage_namespace_match_count":len(storage),
            "shared_advancement_callback_count":original_ids[fid],
            "risk_reasons":reasons,
            "review_priority":priority,
            "approved_for_removal":False,
            "actual_runtime_dependency_verified":False,
        }
        output.append(result)
    counts=collections.Counter(r["review_priority"] for r in output)
    reasons=collections.Counter(reason for row in output for reason in row["risk_reasons"])
    return {
        "schema":1,"status":"READ_ONLY_CALLBACK_DEFERRED_AND_MECHANISM_CROSS_REVIEW",
        "source_sha256":{"aux":AUX_SHA,"graph":GRAPH_SHA,
                          "callbacks":CALLBACK_SHA,"score":SCORE_SHA},
        "total_original_nonweapon_callback_links":181,
        "already_eighth_copy_only_experiment":9,
        "other_links_not_changed":172,
        "static_literal_command_function_roots":len(root_ids),
        "schedule_sites":854,
        "scoreboard_objective_catalog_count":539,
        "storage_namespace_file_count":12,
        "review_priorities":dict(sorted(counts.items())),
        "risk_reason_counts":dict(sorted(reasons.items())),
        "callbacks":output,
        "world_modified":False,"original_code_reused":False,
        "automatically_suppressed_original_functions":0,
        "fully_verified_original_map_separation":False,
        "playtested":False,"multiplayer_tested":False,
        "limitations":[
            "Static function calls and literal schedule-function edges only; dynamic macros/execution unknown.",
            "No proof that a command block actually fires or any advancement completes.",
            "Score/objective and storage matches rely on *sample_function_ids* in the earlier report, not exhaustive per-function joins.",
            "Zero sampled references is NOT proof that a function does not read or write a scoreboard/storage.",
            "The eighth boundary is only an offline nine-link experiment, not playable map approval.",
            "Map doors, portals, scene puzzles, player join/respawn and real multiplayer are NOT TESTED.",
            "Missing or truncated region files and original JSON criteria indirect effects remain unverified.",
            "No original Drehmal function body, command text, map file or third-party asset is reused or published."
        ],
    }

def main():
    p=argparse.ArgumentParser()
    for name in ("aux","graph","callbacks","score","output"):
        p.add_argument("--"+name,type=Path,required=True)
    a=p.parse_args()
    paths=(a.aux,a.graph,a.callbacks,a.score)
    out=a.output.resolve()
    if out in {p.resolve() for p in paths}:
        p.error("Output cannot overwrite source reports")
    docs=[
        read_pinned(a.aux,AUX_SHA),read_pinned(a.graph,GRAPH_SHA),
        read_pinned(a.callbacks,CALLBACK_SHA),read_pinned(a.score,SCORE_SHA)]
    result=analyze(*docs)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({
        "status":result["status"],
        "original_callbacks":result["total_original_nonweapon_callback_links"],
        "review_priorities":result["review_priorities"],
        "world_modified":False,"playtested":False}))

if __name__=="__main__":
    try:
        main()
    except (OSError,ValueError,TypeError,KeyError) as exc:
        print("Original deferred callback cross-review REFUSED: "+str(exc),file=sys.stderr)
        sys.exit(2)
