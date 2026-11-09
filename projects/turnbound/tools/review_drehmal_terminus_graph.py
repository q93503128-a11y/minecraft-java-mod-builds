#!/usr/bin/env python3
"""Read-only review of verified Drehmal 2.2.2f function-call metadata."""
from __future__ import annotations
import argparse
import hashlib
import json
from collections import Counter, deque
from pathlib import Path

AUDIT_SHA = "e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826"
LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
RISK = (
    "players:gamemode/set_survival",
    "players:gamemode/set_adventure",
    "terminus:unlock_tower/",
    "terminus:gifts/",
    "players:spawn/",
    "players:music/",
    "core:scene/terminals/",
)

def audit_index(source):
    if source.get("map_release") != "Drehmal: APOTHEOSIS v2.2.2f" or source.get("world_level_dat_sha256") != LEVEL_SHA:
        raise ValueError("Unsupported map release or level.dat")
    roots = {tag["id"]: tag["values"] for tag in source.get("root_tags", [])}
    if roots.get("#minecraft:load") != ["core:init"] or roots.get("#minecraft:tick") != ["core:main", "entities:main"]:
        raise ValueError("Unexpected source auto roots")
    entries = source.get("functions", [])
    index = {f["id"]: f for f in entries}
    if len(entries) != len(index) or len(entries) != source.get("function_count"):
        raise ValueError("Duplicate or incomplete functions")
    return index

def path_to(index, start, target):
    if start not in index:
        return None
    queue = deque([(start,)])
    visited = {start}
    while queue:
        path = queue.popleft()
        current = path[-1]
        if len(path) > 1 and current.startswith(target):
            return list(path)
        if len(path) > 11:
            continue
        for neighbor in index[current].get("calls", []):
            if neighbor in index and neighbor not in visited:
                visited.add(neighbor)
                queue.append(path + (neighbor,))
    return None

def category(identifier):
    if identifier.startswith("terminus:teleport_from_tower/"):
        return "tower_arrival_replace"
    if identifier.startswith("terminus:teleport_to_tower/"):
        return "tower_departure_replace"
    if identifier.startswith(("core:scene/", "shuttle:")):
        return "structure_review"
    if identifier.startswith(("players:", "drop:", "dialogue:", "abilities:", "projectiles:")):
        return "original_gameplay_review"
    return "unknown_review"

def analyze(source):
    index = audit_index(source)
    scan = source.get("region_command_block_scan", {})
    references = scan.get("function_references")
    if not isinstance(references, dict):
        raise ValueError("No command block references")
    review = []
    counts = Counter()
    missing = []
    for name, count in sorted(references.items()):
        if not isinstance(count, int) or count < 1:
            raise ValueError("Invalid reference count")
        group = category(name)
        counts[group] += count
        meta = index.get(name)
        if meta is None:
            missing.append(name)
            continue
        risks = {prefix: path for prefix in RISK if (path := path_to(index, name, prefix))}
        review.append({
            "id": name, "direct_block_invocations": count, "group": group,
            "pack": meta["pack"], "source_sha256": meta["source_sha256"],
            "called_functions": meta["calls"], "risk_paths": risks,
            "reachable_from_original_auto_roots": meta["reachable_from_load_or_tick"],
            "decision": "NOT_APPROVED_FOR_REMOVAL"
        })
    return {
        "status": "SOURCE_REFERENCES_REVIEWED_NOT_WORLD_MODIFIED",
        "source_audit_sha256": AUDIT_SHA,
        "map_level_sha256": LEVEL_SHA,
        "block_refs_unique": len(references),
        "block_refs_total": sum(references.values()),
        "command_block_matches": scan.get("command_block_count"),
        "region_warnings_lower_bound": len(scan.get("warnings", [])) + scan.get("warnings_truncated", 0),
        "group_counts": dict(counts),
        "unresolved": missing,
        "review": review,
        "limitations": [
            "Static references, not proof of actual runtime execution.",
            "Command block coordinates/complete commands are absent from upstream audit.",
            "Region NBT scan was partial; do not assume all command triggers are known.",
            "No function is approved for deletion or direct reuse."
        ]
    }

def markdown(result):
    q = chr(96)
    lines = [
        "# Drehmal Terminus command-block dependency review",
        "",
        "Source: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568764777",
        "",
        "No map bytes modified. No command blocks disabled. This is not a safe-delete list.",
        "",
        "- Unique command-referenced function IDs: " + str(result["block_refs_unique"]),
        "- Total matched function calls: " + str(result["block_refs_total"]),
        "- Command block matches: " + str(result["command_block_matches"]),
        "- Region scan warnings at least: " + str(result["region_warnings_lower_bound"]),
        "",
        "| Review group | Matched invocations |",
        "|---|---:|"
    ]
    for group, count in sorted(result["group_counts"].items()):
        lines.append("| " + group + " | " + str(count) + " |")
    lines += [
        "",
        "## Pinned static dependency examples",
        "",
    ]
    for sample in (
        "terminus:teleport_from_tower/capital_valley_terminus",
        "terminus:teleport_to_tower/capital_valley_teleport"
    ):
        entry = next((x for x in result["review"] if x["id"] == sample), None)
        if entry:
            lines.append("- " + q + sample + q)
            for prefix, path in entry["risk_paths"].items():
                lines.append("  - " + " -> ".join(q + part + q for part in path))
    lines += [
        "",
        "## Required next integration boundary",
        "",
        "1. Preserve tower geometry and regional routes.",
        "2. Implement server-owned travel/unlock, without invoking the original survival gamemode, music, quest or gift chain.",
        "3. Obtain position-aware command-block metadata for each trigger, including the source dimension and invocation context.",
        "4. Review each door, portal, shuttle and scene command before touching a world copy.",
        "5. No gameplay, licensing, multiplayer, or final map separation is verified by these static references.",
        ""
    ]
    return "\n".join(lines)

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--audit", type=Path, required=True)
    p.add_argument("--out-json", type=Path, required=True)
    p.add_argument("--out-md", type=Path, required=True)
    p.add_argument("--verify-source", action="store_true")
    args = p.parse_args()
    if len({args.audit.resolve(), args.out_json.resolve(), args.out_md.resolve()}) != 3:
        p.error("Source and output paths must be distinct")
    raw = args.audit.read_bytes()
    if args.verify_source and hashlib.sha256(raw).hexdigest() != AUDIT_SHA:
        raise ValueError("Official metadata hash mismatch")
    result = analyze(json.loads(raw))
    args.out_json.parent.mkdir(parents=True, exist_ok=True)
    args.out_md.parent.mkdir(parents=True, exist_ok=True)
    args.out_json.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    args.out_md.write_text(markdown(result), encoding="utf-8")
    print("Analyzed", result["block_refs_unique"], "command-referenced function IDs; no world edits")

if __name__ == "__main__":
    main()
