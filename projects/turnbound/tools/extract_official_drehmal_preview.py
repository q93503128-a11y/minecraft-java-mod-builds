#!/usr/bin/env python3
"""Official Drehmal 2.2.2f transient world extraction; no datapack audit rerun."""
import argparse
import json
import sys
import zipfile
from pathlib import Path
from download_audit_official_drehmal import SHARDS, UPSTREAM, download, safe_extract, sha256

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--download-dir", type=Path, required=True)
    parser.add_argument("--world-dir", type=Path, required=True)
    args = parser.parse_args()
    downloads = args.download_dir.resolve()
    world = args.world_dir.resolve()
    if world.exists() or (downloads.exists() and any(downloads.iterdir())):
        raise ValueError("Refusing pre-existing download or world directory")
    world.mkdir(parents=True)
    downloads.mkdir(parents=True)
    for name, (size, expected_hash) in SHARDS.items():
        archive = downloads / name
        print(f"Retrieving {name}", flush=True)
        download(UPSTREAM + name, archive, size)
        if archive.stat().st_size != size or sha256(archive) != expected_hash:
            raise ValueError("Official shard SHA mismatch")
        safe_extract(archive, world)
        archive.unlink()
        print(f"Verified/extracted {name}", flush=True)
    if not (world / "level.dat").is_file():
        raise ValueError("No level.dat in official world")
    print("Official extracted world prepared, not audited or modified", flush=True)

if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"OFFICIAL MAP PREVIEW PREPARATION BLOCKED: {error}", file=sys.stderr)
        sys.exit(2)
