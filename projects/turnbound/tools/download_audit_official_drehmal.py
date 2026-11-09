#!/usr/bin/env python3
"""One-shot read-only inspection of official Drehmal 2.2.2f release in ephemeral CI.

Downloads each GitHub release ZIP, verifies official SHA256, safely overlays disjoint
shards into a temporary world directory, runs existing local audit and publishes a
redacted summary only. No original world/source/function body is committed/uploaded.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import os
import shutil
import sys
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath

from drehmal_pack_boundary import inspect_world

UPSTREAM = "https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/"
SHARDS = {
    "shard_1.zip": (1484178230, "378f8bea9c88371c44d3a6fd6c6d91a886f14b6dfce97e6e6e994789b979b358"),
    "shard_2.zip": (1641289974, "2d250f04259404d81a3e9bd83a5592f1c26545f45f9c8a22a2277c9e523c1331"),
    "shard_3.zip": (861863079, "8c892c77c7ab9ef03aac7b517e5448490f55690163085b38c380523dd2d29b67"),
}
CHUNK = 4 * 1024 * 1024

def sha256(path: Path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        while data := stream.read(CHUNK):
            h.update(data)
    return h.hexdigest()

def download(url: str, dest: Path, expected_size: int, retries=3):
    """Resume only when server explicitly honors Range. Never use an unverified partial."""
    if dest.exists():
        if dest.stat().st_size == expected_size:
            return
        raise ValueError(f"Unverified partial download present: {dest}")
    staging = dest.with_name(dest.name + ".part")
    for attempt in range(retries):
        offset = staging.stat().st_size if staging.exists() else 0
        if offset == expected_size:
            staging.rename(dest)
            return
        if offset > expected_size:
            raise ValueError("Download exceeded official size")
        headers = {"User-Agent": "TURNBOUND-map-audit/1.0", "Accept-Encoding": "identity"}
        if offset:
            headers["Range"] = f"bytes={offset}-"
        request = urllib.request.Request(url, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                status = response.getcode()
                if offset and status != 206:
                    # Remote did not support resume; start fresh instead of appending.
                    staging.unlink()
                    offset = 0
                with staging.open("ab" if offset else "wb") as handle:
                    while data := response.read(CHUNK):
                        handle.write(data)
            if staging.stat().st_size == expected_size:
                staging.rename(dest)
                return
        except (OSError, urllib.error.URLError) as error:
            print(f"Download attempt {attempt + 1}: {error}", flush=True)
        if attempt + 1 < retries:
            time.sleep(5 * (attempt + 1))
    raise ValueError(f"Unable to obtain complete official file: {dest.name}")

def safe_extract(archive: Path, root: Path):
    count, bytes_written = 0, 0
    with zipfile.ZipFile(archive) as z:
        for entry in z.infolist():
            path = PurePosixPath(entry.filename.replace("\\", "/"))
            if (path.is_absolute() or not path.parts or ".." in path.parts
                    or path.parts[0] == ""):
                raise ValueError(f"Unsafe ZIP member in {archive.name}")
            if ((entry.external_attr >> 16) & 0o170000) == 0o120000:
                raise ValueError(f"Symlink in {archive.name}: {entry.filename}")
            dest = root.joinpath(*path.parts)
            if root.resolve() not in dest.resolve().parents and dest.resolve() != root.resolve():
                raise ValueError(f"Escaped extraction path in {archive.name}")
            if entry.is_dir():
                dest.mkdir(parents=True, exist_ok=True)
                continue
            dest.parent.mkdir(parents=True, exist_ok=True)
            # Independent shard overlap is not trusted: verify identical content, never overwrite silently.
            if dest.exists():
                if dest.stat().st_size != entry.file_size:
                    raise ValueError(f"Conflicting shard member: {entry.filename}")
                with z.open(entry) as reader, dest.open("rb") as existing:
                    while block := reader.read(CHUNK):
                        if existing.read(len(block)) != block:
                            raise ValueError(f"Conflicting shard member: {entry.filename}")
                continue
            with z.open(entry) as reader, dest.open("xb") as writer:
                shutil.copyfileobj(reader, writer, CHUNK)
            if dest.stat().st_size != entry.file_size:
                raise ValueError(f"Truncated extracted member: {entry.filename}")
            count += 1
            bytes_written += entry.file_size
    return count, bytes_written

def summarize(raw: dict, verified_shards: dict) -> dict:
    functions = raw["functions"]
    category_count = collections.Counter(c for f in functions for c in f["categories"])
    top_callers = sorted(functions, key=lambda f: len(f["called_by"]), reverse=True)[:80]
    # Only function IDs, call references, flags and hashes. Never store raw .mcfunction source.
    report = {
        "status": "AUDIT_NOT_A_GAMEPLAY_TEST",
        "map_release": "Drehmal: APOTHEOSIS v2.2.2f",
        "minecraft_version": "1.20.1",
        "upstream_release": "https://github.com/Drehmal-Team/map/releases/tag/v2.2.2f",
        "verified_shards": verified_shards,
        "world_level_dat_sha256": raw["level_dat_sha256"],
        "datapacks": raw["data_packs"],
        "function_count": raw["function_count"],
        "function_tag_count": raw["function_tag_count"],
        "reachable_from_load_or_tick_count": raw["reachable_function_count"],
        "category_counts": dict(category_count),
        "root_tags": [t for t in raw["tags"] if t["id"] in ("#minecraft:load", "#minecraft:tick")],
        "functions": [{
            "id": f["id"], "pack": f["pack"], "source_sha256": f["sha256"],
            "calls": f["calls"], "categories": f["categories"],
            "called_by": f["called_by"], "reachable_from_load_or_tick": f["reachable_from_load_or_tick"],
            "line_count": f["line_count"]
        } for f in functions],
        "top_inbound_functions": [{"id": f["id"], "callers": len(f["called_by"])}
                                  for f in top_callers],
        "unresolved_calls": raw["unresolved_calls"],
        "duplicate_function_ids": raw["duplicate_function_ids"],
        "region_command_block_scan": {
            "status": raw["command_blocks"]["status"],
            "region_files": raw["command_blocks"].get("files", 0),
            "command_block_count": len(raw["command_blocks"]["command_blocks"]),
            "warnings": raw["command_blocks"].get("warnings", [])[:100],
            "warnings_truncated": max(0, len(raw["command_blocks"].get("warnings", [])) - 100),
            "function_references": dict(collections.Counter(
                call for obj in raw["command_blocks"]["command_blocks"]
                for call in __import__("re").findall(
                    r"(?:function|schedule function)\s+(#?[a-z0-9_.-]+:[a-z0-9_./-]+)",
                    obj["command"], flags=__import__("re").I)
            ))
        },
        "license": "Read-only analysis; no third-party assets or command source is redistributed",
        "classification": "NO_ORIGINAL_GAMEPLAY_DISABLED",
        "next_gate": "Review actual function graph and separate original story/rewards from world structure controls on a disposable copy only."
    }
    return report

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--download-dir", type=Path, required=True)
    p.add_argument("--world-dir", type=Path, required=True)
    p.add_argument("--report", type=Path, required=True)
    args = p.parse_args()
    download_dir = args.download_dir.resolve()
    world = args.world_dir.resolve()
    report_file = args.report.resolve()
    if world.exists() or (download_dir.exists() and any(download_dir.iterdir())):
        raise ValueError("Refusing to reuse nonempty download or existing world directory")
    if report_file == world or world in report_file.parents:
        raise ValueError("Report cannot be in world directory")
    download_dir.mkdir(parents=True, exist_ok=True)
    world.mkdir(parents=True, exist_ok=False)
    verified = {}
    for file, (expected_length, expected_hash) in SHARDS.items():
        dest = download_dir / file
        print(f"Downloading official {file} ({expected_length} bytes)", flush=True)
        download(UPSTREAM + file, dest, expected_length)
        if dest.stat().st_size != expected_length or sha256(dest) != expected_hash:
            raise ValueError(f"Official release SHA mismatch: {file}")
        verified[file] = {"bytes": expected_length, "sha256": expected_hash}
        print(f"Verified {file}; extracting safe entries", flush=True)
        count, size = safe_extract(dest, world)
        dest.unlink()
        print(f"Extracted {file}: {count} new files, {size} bytes; archive removed", flush=True)
    if not (world / "level.dat").is_file() or not (world / "datapacks").is_dir():
        raise ValueError("Official shards did not yield expected Minecraft world layout")
    print("Read-only datapack and region NBT inspection started", flush=True)
    raw = inspect_world(world)
    result = summarize(raw, verified)
    report_file.parent.mkdir(parents=True, exist_ok=True)
    report_file.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print(json.dumps({k: result[k] for k in (
        "map_release", "function_count", "function_tag_count",
        "reachable_from_load_or_tick_count", "category_counts"
    )}, ensure_ascii=False), flush=True)
    print("Region command-block scan:", result["region_command_block_scan"]["status"],
          "blocks", result["region_command_block_scan"]["command_block_count"], flush=True)
    print(f"Summary report generated: {report_file} (no world bytes uploaded)", flush=True)

if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, zipfile.BadZipFile) as exc:
        print(f"OFFICIAL DREHMAL AUDIT BLOCKED: {exc}", file=sys.stderr)
        sys.exit(2)
