#!/usr/bin/env python3
"""Pure metadata review; NEVER executes/reuses original Drehmal code or edits worlds."""
import argparse
import collections
import hashlib
import json
from pathlib import Path

AUDIT_SHA = "e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826"
POSITIONS_SHA = "26df4bab53bb655ec6d0ca1d369162c5bc2dbf542baba9e876dccb284d71bb87"
LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
TAGS = {"#minecraft:load":["core:init"],"#minecraft:tick":["core:main","entities:main"]}
TOWER = {"TOWER_ARRIVAL_REPLACE","TOWER_DEPARTURE_REPLACE"}

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def closure(index, roots):
    found,unknown=set(),set()
    stack=list(roots)
    while stack:
        node=stack.pop()
        if node in found: continue
        if node not in index:
            unknown.add(node)
            continue
        found.add(node)
        stack.extend(index[node]["calls"])
    return found,unknown

def classify(audit,locations):
    if (audit.get("map_release")!="Drehmal: APOTHEOSIS v2.2.2f" or
            audit.get("world_level_dat_sha256")!=LEVEL_SHA or
            locations.get("level_dat_sha256")!=LEVEL_SHA or
            locations.get("world_changed") is not False):
        raise ValueError("Map/source mismatch")
    roots={x["id"]:x["values"] for x in audit.get("root_tags",[])}
    if roots!=TAGS:
        raise ValueError("Unexpected original load/tick roots")
    entries=audit["functions"]
    index={x["id"]:x for x in entries}
    if len(index)!=len(entries) or len(entries)!=audit["function_count"] or audit["duplicate_function_ids"]:
        raise ValueError("Ambiguous or incomplete callgraph")
    blocks=locations["triggers"]
    if len(blocks)!=locations["function_call_triggers"]:
        raise ValueError("Incomplete spatial trigger report")
    observed=collections.Counter(fid for block in blocks for fid in block["function_ids"])
    if observed!=collections.Counter(audit["region_command_block_scan"]["function_references"]):
        raise ValueError("Spatial and original function-call references disagree")
    suppressed=[b for b in blocks if b["review_categories"]==["TOWER_ARRIVAL_REPLACE"] or b["review_categories"]==["TOWER_DEPARTURE_REPLACE"]]
    remaining=[b for b in blocks if b not in suppressed]
    if (len(suppressed)!=62 or len(remaining)!=17 or
            sum(b["review_categories"]==["TOWER_ARRIVAL_REPLACE"] for b in suppressed)!=29 or
            sum(b["review_categories"]==["TOWER_DEPARTURE_REPLACE"] for b in suppressed)!=33):
        raise ValueError("Verified offline patch scope changed")
    auto,_=closure(index,roots["#minecraft:load"]+roots["#minecraft:tick"])
    blockall,_=closure(index,observed)
    direct=[fid for block in remaining for fid in block["function_ids"]]
    residual,unknown=closure(index,direct)
    if (len(auto),len(blockall),len(residual),len(direct))!=(16517,314,225,21):
        raise ValueError("Official callgraph changed; do not silently reclassify")
    individual=[]
    for block in remaining:
        ids=block["function_ids"]
        reached,unresolved=closure(index,ids)
        individual.append({
            "dimension":block["source_dimension"],
            "region_file":block["region_file"],
            "coordinates":block["coordinates"],
            "command_sha256":block["command_sha256"],
            "function_ids":ids,
            "review_categories":block["review_categories"],
            "transitive_functions":len(reached),
            "unresolved_in_graph":sorted(unresolved),
            "possible_operation_flags":sorted({v for fid in reached for v in index[fid]["categories"]}),
            "action":"REVIEW_REQUIRED_NO_PATCH"
        })
    scan=audit["region_command_block_scan"]
    return {
        "status":"STATIC_ANALYSIS_NOT_GAMEPLAY_OR_SAFE_DELETE_PROOF",
        "verified_audit_sha":AUDIT_SHA,"verified_locations_sha":POSITIONS_SHA,
        "original_functions":len(index),
        "load_tick_callgraph":len(auto),
        "all_command_block_callgraph":len(blockall),
        "command_block_only_not_auto":len(blockall-auto),
        "original_function_call_blocks":len(blocks),
        "offline_preview_disabled_tower_blocks":len(suppressed),
        "remaining_literal_function_call_blocks":len(remaining),
        "remaining_literal_function_calls":len(direct),
        "remaining_static_graph_closure":len(residual),
        "remaining_unknown_direct_calls":sorted(set(direct)-index.keys()),
        "other_command_blocks_unclassified":scan["command_block_count"]-len(blocks),
        "partial_region_scan_warning_count":len(scan["warnings"])+scan["warnings_truncated"],
        "original_unresolved_calls":len(audit["unresolved_calls"]),
        "residual_by_namespace":dict(sorted(collections.Counter(fid.split(":")[0] for fid in residual).items())),
        "residual_operation_flags":dict(sorted(collections.Counter(flag for fid in residual for flag in index[fid]["categories"]).items())),
        "remaining_triggers":individual,
        "map_modified":False,
        "reuse_of_original_code":"NONE_USER_APPROVAL_REQUIRED",
        "gaps":["Saved scheduled functions/advancement triggers not covered by function graph",
                "Command blocks not containing literal function calls need direct operation review",
                "Unreadable region files; dimensions and redstone mechanisms need inspection",
                "Graph reachability is not proof of execution; absence is not proof of safety"]
    }

def markdown(r):
    lines=["# Drehmal 2.2.2f: residual original gameplay entrypoints","","Static read-only analysis. No source reused or map edited.","",
           "| Evidence | Count |","|---|---:|"]
    for title,key in [
        ("Original function IDs","original_functions"),
        ("Original load/tick reachable function IDs","load_tick_callgraph"),
        ("Literal command-block reachable function IDs before patch","all_command_block_callgraph"),
        ("Command-block-only IDs (not load/tick reachable)","command_block_only_not_auto"),
        ("Confirmed original literal function-call blocks","original_function_call_blocks"),
        ("Previously verified copy-only tower command suppressions","offline_preview_disabled_tower_blocks"),
        ("Residual literal function-call blocks","remaining_literal_function_call_blocks"),
        ("Residual matched function invocations","remaining_literal_function_calls"),
        ("Potential residual static-call closure","remaining_static_graph_closure"),
        ("Other scanned command blocks not operation-classified","other_command_blocks_unclassified"),
        ("Original region scan warnings","partial_region_scan_warning_count"),
        ("Unresolved function references","original_unresolved_calls")
    ]:
        lines.append("| "+title+" | "+str(r[key])+" |")
    lines+=["","## Residual function-call blocks (not safe to delete)","",
            "| Dimension | Position | Function ID(s) | Review category |",
            "|---|---|---|---|"]
    for t in r["remaining_triggers"]:
        lines.append("| "+t["dimension"]+" | "+str(t["coordinates"])+" | "+", ".join(t["function_ids"])+
                     " | "+", ".join(t["review_categories"])+" |")
    lines+=["","## Limitations and next work",""]
    lines.extend("- "+gap for gap in r["gaps"])
    lines+=["","Recommended next evidence gate: inspect direct commands, scheduled functions and advancement/JSON triggers. Preserve terrain and mechanisms; original script reuse needs user approval.",""]
    return "\n".join(lines)

def main():
    p=argparse.ArgumentParser()
    for key in ("audit","positions","output-json","output-md"):
        p.add_argument("--"+key,required=True,type=Path)
    a=p.parse_args()
    paths=[a.audit.resolve(),a.positions.resolve(),a.output_json.resolve(),a.output_md.resolve()]
    if len(set(paths))!=4: p.error("Inputs/outputs must be distinct")
    raw_a=a.audit.read_bytes();raw_b=a.positions.read_bytes()
    if sha(raw_a)!=AUDIT_SHA or sha(raw_b)!=POSITIONS_SHA:
        raise ValueError("Unexpected official audit/positions bytes")
    r=classify(json.loads(raw_a),json.loads(raw_b))
    a.output_json.parent.mkdir(parents=True,exist_ok=True)
    a.output_md.parent.mkdir(parents=True,exist_ok=True)
    a.output_json.write_text(json.dumps(r,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    a.output_md.write_text(markdown(r),encoding="utf-8")
    print(json.dumps({"residual_blocks":r["remaining_literal_function_call_blocks"],
                      "residual_functions":r["remaining_static_graph_closure"],
                      "world_modified":False}))

if __name__=="__main__":
    main()
