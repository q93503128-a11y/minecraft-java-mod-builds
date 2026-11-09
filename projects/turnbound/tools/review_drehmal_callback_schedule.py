#!/usr/bin/env python3
"""Verified metadata-only Drehmal advancement and schedule review.

NOT a safe-removal list, source-code license, runtime test or Minecraft world edit.
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path

AUX_SHA = "9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef"
GRAPH_SHA = "e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826"
LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
RELEASE = "Drehmal: APOTHEOSIS v2.2.2f"

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def reach(graph, roots):
    visited = set()
    stack = list(roots)
    while stack:
        v = stack.pop()
        if v in visited or v not in graph:
            continue
        visited.add(v)
        stack.extend(graph[v].get("calls", []))
    return visited

def cycle_components(edges):
    """Tarjan SCC of explicit schedule-function relations only."""
    graph = {k: sorted(set(v)) for k,v in edges.items()}
    rank = 0
    indices, lowlinks, stack, active, components = {}, {}, [], set(), []
    def visit(node):
        nonlocal rank
        indices[node] = lowlinks[node] = rank
        rank += 1
        stack.append(node)
        active.add(node)
        for child in graph.get(node, ()):
            if child not in indices:
                visit(child)
                lowlinks[node] = min(lowlinks[node], lowlinks[child])
            elif child in active:
                lowlinks[node] = min(lowlinks[node], indices[child])
        if lowlinks[node] == indices[node]:
            members = []
            while True:
                member = stack.pop()
                active.remove(member)
                members.append(member)
                if member == node:
                    break
            if len(members) > 1 or node in graph.get(node, ()):
                components.append(sorted(members))
    for node in sorted(graph):
        if node not in indices:
            visit(node)
    return sorted(components, key=lambda x:(-len(x),x))

def group(record, meta):
    name = record["reward_function_id"]
    namespace = record["file"].split("/")[1]
    if meta is None:
        return "MISSING_FUNCTION_REVIEW"
    flags = set(meta.get("categories", []))
    if (namespace == "foundry" or
            name.startswith(("core:scene/","core:heatmap/","terminus:"))):
        return "STRUCTURE_OR_PUZZLE_PRESERVE_REVIEW"
    if "world_edit" in flags or "teleport" in flags:
        return "WORLD_STATE_OR_TRAVEL_REVIEW"
    if namespace == "entities" or "entities" in flags:
        return "ENTITY_COMBAT_REVIEW"
    if namespace in ("dialogue","advancements"):
        return "STORY_OR_ADVANCEMENT_REVIEW"
    return "PLAYER_REWARD_OR_STATE_REVIEW"

def analyze(aux, graph_payload):
    if (aux.get("map_release") != RELEASE or
            graph_payload.get("map_release") != RELEASE or
            aux.get("level_sha256") != LEVEL_SHA or
            graph_payload.get("world_level_dat_sha256") != LEVEL_SHA or
            aux.get("world_bytes_modified") is not False or
            aux.get("original_code_copied_into_turnbound") is not False):
        raise ValueError("Unverified original Drehmal identity")
    source = graph_payload["functions"]
    graph = {r["id"]:r for r in source}
    if (len(source) != len(graph) or
            len(graph) != graph_payload.get("function_count") or
            graph_payload.get("duplicate_function_ids")):
        raise ValueError("Incomplete or duplicated function index")
    rewards = aux["datapack_json_and_schedule"]["advancement_reward_records"]
    callbacks = [r for r in rewards if r.get("reward_function_id")]
    weapons = [r for r in callbacks if r["file"].startswith("data/weapons/advancements/")]
    other = [r for r in callbacks if r not in weapons]
    if (len(rewards),len(callbacks),len(weapons),len(other)) != (292,220,39,181):
        raise ValueError("The previously verified callback cohort changed")
    if len({r["file"] for r in other}) != len(other):
        raise ValueError("Duplicate advancement file")
    entries = aux["datapack_json_and_schedule"]["function_schedule_literal_sites"]
    if len(entries)!=672 or sum(s["literal_schedule_count"] for s in entries)!=854:
        raise ValueError("The previously verified scheduler cohort changed")
    schedule = collections.defaultdict(set)
    for entry in entries:
        fid = entry["function_id"]
        if fid not in graph or entry["function_source_sha256"]!=graph[fid]["source_sha256"]:
            raise ValueError("Schedule source hash mismatch")
        if entry["literal_schedule_count"] < len(entry["scheduled_target_ids"]):
            raise ValueError("Inconsistent schedule call count")
        schedule[fid].update(entry["scheduled_target_ids"])
    auto = reach(graph, ("core:init","core:main","entities:main"))
    root_callbacks = [r["reward_function_id"] for r in other]
    callback_graph = reach(graph,root_callbacks)
    cycles = cycle_components(schedule)
    tagged = []
    for row in other:
        fid=row["reward_function_id"]
        meta=graph.get(fid)
        transitive=reach(graph,(fid,))
        tagged.append({
            "advancement_file":row["file"],
            "advancement_source_sha256":row["file_sha256"],
            "callback_id":fid,
            "callback_exists":meta is not None,
            "trigger_types":row["criteria_triggers"],
            "reward_fields":row["reward_field_names"],
            "review_group":group(row,meta),
            "direct_function_callers":meta["called_by"] if meta else [],
            "reachable_from_original_auto_roots":fid in auto,
            "transitive_function_count":len(transitive),
            "possible_world_edit":any("world_edit" in graph[x]["categories"] for x in transitive),
            "has_schedule_in_static_callgraph":bool(set(schedule)&transitive),
            "decision":"NOT_APPROVED_FOR_REMOVAL_OR_REUSE"
        })
    result={
        "schema":1,"state":"READ_ONLY_NOT_ACTIVATION_PROOF",
        "source_aux_sha256":AUX_SHA,"source_graph_sha256":GRAPH_SHA,
        "nonweapon_callback_records":len(other),
        "nonweapon_distinct_callback_ids":len(set(root_callbacks)),
        "callback_missing_ids":sorted(set(root_callbacks)-set(graph)),
        "review_group_counts":dict(sorted(collections.Counter(t["review_group"] for t in tagged).items())),
        "advancement_namespace_counts":dict(sorted(collections.Counter(t["advancement_file"].split("/")[1] for t in tagged).items())),
        "callbacks_with_direct_function_callers":sum(bool(t["direct_function_callers"]) for t in tagged),
        "callbacks_with_possible_world_edit":sum(t["possible_world_edit"] for t in tagged),
        "callbacks_with_schedule_path":sum(t["has_schedule_in_static_callgraph"] for t in tagged),
        "callbacks_reachable_from_original_auto_roots":sum(t["reachable_from_original_auto_roots"] for t in tagged),
        "schedule_source_functions":len(schedule),
        "schedule_call_sites":sum(s["literal_schedule_count"] for s in entries),
        "schedule_unique_target_ids":len({v for targets in schedule.values() for v in targets}),
        "schedule_sources_outside_auto_graph":len(set(schedule)-auto),
        "schedule_sources_reachable_from_callback_graph":len(set(schedule)&callback_graph),
        "schedule_self_edges":sum(k in v for k,v in schedule.items()),
        "schedule_cycle_components":len(cycles),
        "schedule_cycle_sizes":dict(sorted(collections.Counter(map(len,cycles)).items())),
        "schedule_cycle_examples":cycles[:10],
        "callbacks":tagged,
        "world_modified":False,"original_code_reused":False,
        "not_verified":["No direct proof of callback firing or scheduled execution",
                        "Saved schedule state is not analyzed",
                        "Advancement progress may gate doors and devices even with no function callers",
                        "World structure preservation not verified in Minecraft client"]
    }
    if (result["nonweapon_distinct_callback_ids"] != 171 or
            result["schedule_cycle_components"] != 29):
        raise ValueError("Known verified callback/schedule counts drifted")
    return result

def main():
    p=argparse.ArgumentParser()
    for a in ("aux","graph","out"):
        p.add_argument("--"+a,type=Path,required=True)
    args=p.parse_args()
    raw_a,raw_g=args.aux.read_bytes(),args.graph.read_bytes()
    if sha(raw_a)!=AUX_SHA or sha(raw_g)!=GRAPH_SHA:
        raise ValueError("Source audit artifacts must match original pinned SHA256")
    if args.out.resolve() in (args.aux.resolve(),args.graph.resolve()):
        p.error("Cannot overwrite original metadata input")
    report=analyze(json.loads(raw_a),json.loads(raw_g))
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:report[k] for k in
        ("nonweapon_callback_records","schedule_source_functions","schedule_cycle_components","world_modified")}))

if __name__=="__main__":
    main()
