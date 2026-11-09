#!/usr/bin/env python3
"""Read-only spatial metadata of Drehmal command-block function triggers.

Never exports raw command strings, source NBT, original world bytes, or textures.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from scan_drehmal_command_blocks import scan_world

LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
REFERENCES = re.compile(r"(?:function|schedule\s+function)\s+(#?[a-z0-9_.-]+:[a-z0-9_./-]+)", re.I)

def dimension(region_path):
    path = Path(region_path).parts
    if not path:
        return "UNKNOWN"
    if path[0] == "DIM-1":
        return "minecraft:the_nether"
    if path[0] == "DIM1":
        return "minecraft:the_end"
    if len(path) >= 4 and path[0] == "dimensions":
        return path[1] + ":" + path[2]
    if path[0] in ("region", "entities"):
        return "minecraft:overworld"
    return "UNKNOWN"

def classify(fid):
    if fid.startswith("terminus:teleport_from_tower/"):
        return "TOWER_ARRIVAL_REPLACE"
    if fid.startswith("terminus:teleport_to_tower/"):
        return "TOWER_DEPARTURE_REPLACE"
    if fid.startswith(("core:scene/", "shuttle:")):
        return "STRUCTURE_REVIEW"
    if fid.startswith(("players:", "dialogue:", "drop:", "projectiles:", "abilities:")):
        return "ORIGINAL_GAMEPLAY_REVIEW"
    return "UNKNOWN_REVIEW"

def safe_trigger(record, root):
    command = record.get("command")
    if not isinstance(command, str):
        raise ValueError("Missing original command")
    ids = sorted(set(REFERENCES.findall(command)))
    if not ids:
        return None
    file_path = Path(record["file"]).resolve()
    root = Path(root).resolve()
    if root not in file_path.parents:
        raise ValueError("Region outside verified world root")
    region = file_path.relative_to(root).as_posix()
    coords = [record.get(axis) for axis in ("x", "y", "z")]
    known = all(isinstance(v, int) and not isinstance(v, bool) for v in coords)
    return {
        "source_dimension": dimension(region),
        "region_file": region,
        "chunk_slot": record["chunk_slot"],
        "command_actor_type": record["id"],
        "coordinates": coords if known else None,
        "position_source": "NBT" if known else "UNKNOWN_REQUIRES_INSPECTION",
        "command_sha256": hashlib.sha256(command.encode("utf-8")).hexdigest(),
        "function_ids": ids,
        "review_categories": sorted({classify(fid) for fid in ids}),
        "authorisation": "UNREVIEWED_NO_GAMEPLAY_PATCH"
    }

def report(world, source=None):
    world = Path(world).resolve(strict=True)
    level = world / "level.dat"
    if not level.is_file():
        raise ValueError("Not an extracted Minecraft world")
    if hashlib.sha256(level.read_bytes()).hexdigest() != LEVEL_SHA:
        raise ValueError("Official map hash mismatch")
    raw = source if source is not None else scan_world(world)
    records = []
    for actor in raw["command_blocks"]:
        trigger = safe_trigger(actor, world)
        if trigger is not None:
            records.append(trigger)
    counts = Counter(cat for item in records for cat in item["review_categories"])
    flags = Counter()
    for warning in raw["warnings"]:
        if "missing location header" in warning:
            flags["short_header"] += 1
        elif "External .mcc" in warning:
            flags["external_chunk"] += 1
        else:
            flags["other"] += 1
    return {
        "source": "Official Drehmal 2.2.2f, GitHub release v2.2.2f",
        "level_dat_sha256": LEVEL_SHA,
        "world_changed": False,
        "regions_scanned": raw["files"],
        "matched_command_blocks": len(raw["command_blocks"]),
        "function_call_triggers": len(records),
        "categories": dict(counts),
        "missing_position": sum(item["coordinates"] is None for item in records),
        "warning_groups": dict(flags),
        "scan_status": raw["status"],
        "triggers": records,
        "notice": (
            "Incomplete NBT scan may miss commands. Coordinates are unverified in Minecraft client, "
            "source command conditions are intentionally excluded. No automated modification approved."
        )
    }

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--world", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args()
    world = a.world.resolve(strict=True)
    out = a.output.resolve()
    if out == world or world in out.parents:
        p.error("Output must be outside official world")
    result = report(world)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("Matched", result["function_call_triggers"], "function-call command blocks;",
          result["missing_position"], "have no verifiable integer coordinates.")
    print("Partial scan warnings:", result["warning_groups"])

if __name__ == "__main__":
    main()
