#!/usr/bin/env python3
"""Read-only semantic audit and safe staging of the official 1.20.1 Drehmal world.

This program never disables original functionality automatically.
Only use its output to review a future targeted adapter on a copy.
No third-party world bytes are redistributed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import sys
import zipfile
from collections import defaultdict, Counter
from pathlib import Path, PurePosixPath
from scan_drehmal_command_blocks import scan_world

FUNCTION = re.compile(r"^data/([a-z0-9_.-]+)/functions/([a-z0-9_./-]+)\.mcfunction$")
TAG = re.compile(r"^data/([a-z0-9_.-]+)/tags/functions/([a-z0-9_./-]+)\.json$")
FUNC_CALL = re.compile(r"(?:^|\s)(?:function|schedule\s+function)\s+(#?[a-z0-9_.-]+:[a-z0-9_./-]+)", re.I)
COMMAND_PREFIX = re.compile(r"^\s*(?:execute\b.*\brun\s+)?([a-z][a-z0-9_]*)\b", re.I)
RISK = {
    "world_edit": {"setblock", "fill", "clone", "place", "fillbiome"},
    "rewards": {"give", "loot", "item", "experience", "xp", "recipe", "enchant"},
    "progression": {"advancement", "scoreboard", "bossbar", "attribute"},
    "teleport": {"tp", "teleport", "spreadplayers", "execute", "ride"},
    "entities": {"summon", "kill", "damage", "effect", "data"},
    "presentation": {"playsound", "stopsound", "particle", "title", "tellraw", "say"}
}
MAX_TEXT = 8 * 1024 * 1024

def is_safe_member(name: str) -> bool:
    p = PurePosixPath(name.replace("\\", "/"))
    return not p.is_absolute() and ".." not in p.parts and bool(p.parts)

def read_pack(pack: Path):
    """Return (source_relative_path, bytes) for function and function-tag files."""
    def wanted(path: str) -> bool:
        return bool(FUNCTION.fullmatch(path) or TAG.fullmatch(path))
    if pack.is_symlink():
        raise ValueError(f"Symlinked pack prohibited: {pack}")
    if pack.is_dir():
        for file in sorted(pack.rglob("*")):
            if file.is_symlink():
                raise ValueError(f"Symlinked pack member prohibited: {file}")
            if file.is_file():
                relative = file.relative_to(pack).as_posix()
                if wanted(relative):
                    if file.stat().st_size > MAX_TEXT:
                        raise ValueError(f"Datapack function/tag too large: {file}")
                    yield relative, file.read_bytes()
    elif pack.is_file() and pack.suffix.lower() == ".zip":
        with zipfile.ZipFile(pack) as z:
            for info in z.infolist():
                if not is_safe_member(info.filename):
                    raise ValueError(f"Unsafe ZIP path: {info.filename}")
                if info.is_dir() or not wanted(info.filename):
                    continue
                if info.file_size > MAX_TEXT:
                    raise ValueError(f"Datapack function/tag too large: {info.filename}")
                yield info.filename, z.read(info)
    else:
        raise ValueError(f"Unsupported datapack entry: {pack}")

def inspect_world(world: Path) -> dict:
    if not world.is_dir() or not (world / "level.dat").is_file():
        raise ValueError("Expected extracted Minecraft world folder containing level.dat")
    packs_dir = world / "datapacks"
    if not packs_dir.is_dir():
        raise ValueError("World datapacks directory missing; cannot establish Drehmal compatibility")
    records = {}
    tags = {}
    duplicate_ids = []
    files = []
    for pack in sorted(packs_dir.iterdir()):
        if pack.name.startswith("."):
            continue
        for relative, raw in read_pack(pack):
            sha = hashlib.sha256(raw).hexdigest()
            identifier = None
            func_match = FUNCTION.fullmatch(relative)
            tag_match = TAG.fullmatch(relative)
            if func_match:
                identifier = f"{func_match.group(1)}:{func_match.group(2)}"
                content = raw.decode("utf-8", errors="replace")
                calls = []
                categories = set()
                commands = Counter()
                for line in content.splitlines():
                    line = line.strip()
                    if not line or line.startswith("#"):
                        continue
                    found = FUNC_CALL.findall(line)
                    calls.extend(found)
                    cmd = COMMAND_PREFIX.match(line)
                    if cmd:
                        commands[cmd.group(1).lower()] += 1
                    for key, keywords in RISK.items():
                        if any(re.search(r"\b" + re.escape(word) + r"\b", line, re.I) for word in keywords):
                            categories.add(key)
                value = {
                    "id": identifier, "pack": pack.name, "path": relative,
                    "sha256": sha, "calls": sorted(set(calls)), "categories": sorted(categories),
                    "commands": dict(commands), "line_count": len(content.splitlines())
                }
                if identifier in records:
                    duplicate_ids.append(identifier)
                records[identifier] = value
            elif tag_match:
                identifier = f"#{tag_match.group(1)}:{tag_match.group(2)}"
                try:
                    data = json.loads(raw)
                    values = [v if isinstance(v, str) else v.get("id", "")
                              for v in data.get("values", [])]
                except (ValueError, TypeError, AttributeError) as error:
                    raise ValueError(f"Invalid function tag {pack.name}/{relative}: {error}") from error
                if not all(isinstance(v, str) and v for v in values):
                    raise ValueError(f"Invalid tag values in {pack.name}/{relative}")
                tags[identifier] = {"id": identifier, "pack": pack.name, "path": relative,
                                    "sha256": sha, "values": values, "replace": bool(data.get("replace", False))}
            files.append({"pack": pack.name, "path": relative, "sha256": sha})
    reverse = defaultdict(list)
    unresolved = []
    for fid, meta in records.items():
        for dest in meta["calls"]:
            reverse[dest].append(fid)
            if not dest.startswith("#") and dest not in records and not dest.startswith("minecraft:"):
                unresolved.append({"caller": fid, "missing": dest})
    # Root tags can transitively point to other tags. Do not assume only one tick/load root.
    roots = ["#minecraft:load", "#minecraft:tick"]
    root_functions = set()
    todo = list(roots)
    visited = set()
    while todo:
        node = todo.pop()
        if node in visited:
            continue
        visited.add(node)
        if node.startswith("#"):
            todo.extend(tags.get(node, {}).get("values", []))
        else:
            root_functions.add(node)
            todo.extend(records.get(node, {}).get("calls", []))
    for fid, meta in records.items():
        meta["called_by"] = sorted(reverse[fid])
        meta["reachable_from_load_or_tick"] = fid in root_functions
        meta["review_status"] = "REVIEW_REQUIRED"
        # Mechanism is reference classification, not an inferred safe-disable verdict.
    command_blocks = scan_world(world)
    return {
        "schema": 1, "source": str(world.resolve()),
        "level_dat_sha256": hashlib.sha256((world / "level.dat").read_bytes()).hexdigest(),
        "data_packs": [p.name for p in sorted(packs_dir.iterdir()) if not p.name.startswith(".")],
        "function_count": len(records), "function_tag_count": len(tags),
        "reachable_function_count": sum(x in root_functions for x in records),
        "functions": [records[k] for k in sorted(records)],
        "tags": [tags[k] for k in sorted(tags)],
        "unresolved_calls": unresolved,
        "duplicate_function_ids": sorted(set(duplicate_ids)),
        "command_blocks": command_blocks,
        "classification": "INSPECTION_ONLY",
        "safety": "A function classified as rewards or progression can still control map doors, teleporters or visuals. No automatic deactivation is permitted."
    }

def stage_copy(source: Path, destination: Path) -> None:
    source = source.resolve(strict=True)
    destination = destination.resolve()
    if destination.exists():
        raise ValueError("Destination already exists; refusing to overwrite")
    if source == destination or source in destination.parents or destination in source.parents:
        raise ValueError("Source and destination must be disjoint folders")
    if not (source / "level.dat").is_file():
        raise ValueError("Source is not a Minecraft world")
    for member in source.rglob("*"):
        if member.is_symlink():
            raise ValueError(f"Symlink in source world: {member}")
    shutil.copytree(source, destination, symlinks=False)
    try:
        level_sha = hashlib.sha256((source / "level.dat").read_bytes()).hexdigest()
        if hashlib.sha256((destination / "level.dat").read_bytes()).hexdigest() != level_sha:
            raise ValueError("Copied level.dat mismatch")
        (destination / "turnbound-stage.json").write_text(json.dumps({
            "schema": 1, "source_level_sha256": level_sha,
            "notice": "Copied world for offline audit. No original datapack disabled. Not activated for TURNBOUND."
        }, indent=2) + "\n", encoding="utf-8")
    except BaseException:
        shutil.rmtree(destination)
        raise

def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest="action", required=True)
    scan = sub.add_parser("audit", help="Inspect an extracted world; no mutations")
    scan.add_argument("--world", type=Path, required=True)
    scan.add_argument("--output", type=Path, required=True)
    stage = sub.add_parser("stage", help="Make one preserved complete copy (can require many GB)")
    stage.add_argument("--source", type=Path, required=True)
    stage.add_argument("--destination", type=Path, required=True)
    args = p.parse_args(argv)
    if args.action == "stage":
        stage_copy(args.source, args.destination)
        print(f"World copied to {args.destination}; original unchanged; datapack NOT disabled")
    else:
        world = args.world.resolve(strict=True)
        output = args.output.resolve()
        if world == output or world in output.parents:
            p.error("Audit output must be outside the original world")
        report = inspect_world(world)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"Audit written: {output} | functions={report['function_count']} "
              f"| tick/load reachable={report['reachable_function_count']} | command block region scan included")

if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        print(f"Drehmal audit refused: {error}", file=sys.stderr)
        sys.exit(2)
