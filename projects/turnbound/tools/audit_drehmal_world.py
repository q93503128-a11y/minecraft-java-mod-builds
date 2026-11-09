#!/usr/bin/env python3
"""Read-only inventory of source Drehmal datapack behavior. NEVER modifies a world."""
import argparse
import json
import re
import zipfile
from collections import Counter
from pathlib import Path

AREAS = ("functions/", "tags/functions/", "advancements/", "loot_tables/",
         "structures/", "dimension/", "dimension_type/", "worldgen/",
         "predicates/", "item_modifiers/")
COMMANDS = ("function ", "schedule function ", "execute ", "scoreboard ",
            "tp ", "teleport ", "give ", "loot ", "summon ", "setblock ", "fill ",
            "data modify ", "advancement ", "effect ", "playsound ")
INTEREST = ("load.json", "tick.json", "pack.mcmeta")

def inspect_pack(path: Path):
    counts = Counter()
    candidates = []
    errors = []
    def observe(name, raw):
        normalized = name.replace("\\", "/")
        parts = normalized.split("/")
        if len(parts) >= 3 and parts[0] == "data":
            namespace, kind = parts[1], "/".join(parts[2:])
            for area in AREAS:
                if kind.startswith(area):
                    counts[f"{namespace}/{area}"] += 1
        if normalized.endswith((".mcfunction", ".json")):
            text = raw.decode("utf-8", errors="replace")
            matches = sorted({command.strip() for command in COMMANDS if command in text})
            if matches or any(normalized.endswith(x) for x in INTEREST):
                candidates.append({"path": normalized, "signals": matches,
                                   "size_bytes": len(raw)})
    if path.is_file() and path.suffix.lower() == ".zip":
        with zipfile.ZipFile(path) as archive:
            for entry in archive.infolist():
                if entry.is_dir(): continue
                if entry.filename.endswith((".mcfunction", ".json", "pack.mcmeta")):
                    if entry.file_size > 4_000_000:
                        errors.append(f"skipped oversized text: {entry.filename}")
                        continue
                    observe(entry.filename, archive.read(entry))
    elif path.is_dir():
        for entry in path.rglob("*"):
            if entry.is_file() and entry.name.endswith((".mcfunction", ".json", "pack.mcmeta")):
                if entry.stat().st_size > 4_000_000:
                    errors.append(f"skipped oversized text: {entry}")
                    continue
                observe(entry.relative_to(path).as_posix(), entry.read_bytes())
    else:
        errors.append("unrecognized datapack layout: " + str(path))
    return {"pack": str(path), "counts": dict(counts),
            "candidate_automation_files": candidates, "warnings": errors}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--world", type=Path, required=True,
                        help="EXTRACTED original Drehmal world directory; read only")
    parser.add_argument("--output", type=Path, required=True,
                        help="JSON report outside world folder")
    args = parser.parse_args()
    world = args.world.resolve(strict=True)
    output = args.output.resolve()
    if not world.is_dir() or not (world / "level.dat").is_file():
        parser.error("expected extracted world folder containing level.dat")
    if output == world or world in output.parents:
        parser.error("report must be outside the original world directory")
    packs = world / "datapacks"
    results = [inspect_pack(p) for p in sorted(packs.iterdir())] if packs.is_dir() else []
    report = {
        "world": str(world), "has_level_dat": True,
        "has_region": (world / "region").is_dir(),
        "has_datapacks": packs.is_dir(), "datapacks": results,
        "status": "INVENTORY_ONLY_NOT_DISABLED",
        "safety_note": "Do not remove function load/tick or datapack files until dependent doors, portals, boss scripts, maps and structures are classified."
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Report written to {output}; original world unchanged. Packs: {len(results)}")

if __name__ == "__main__":
    main()
