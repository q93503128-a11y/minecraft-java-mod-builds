#!/usr/bin/env python3
"""Inspect a locally acquired Azari Minecraft Java world archive without modifying it."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

REGION_RE = re.compile(r"(?:^|/)region/r\\.(-?\\d+)\\.(-?\\d+)\\.mca$")


@dataclass(frozen=True)
class RegionBounds:
    min_region_x: int
    max_region_x: int
    min_region_z: int
    max_region_z: int
    min_block_x: int
    max_block_x: int
    min_block_z: int
    max_block_z: int
    width_blocks: int
    depth_blocks: int
    region_file_count: int


@dataclass(frozen=True)
class WorldArchiveReport:
    source_archive: str
    sha256: str
    archive_bytes: int
    zip_entry_count: int
    world_root: str
    level_dat_path: str
    region_root: str
    has_datapacks: bool
    has_dimensions: bool
    region_bounds: RegionBounds
    warnings: list[str]


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def common_world_root(names: Iterable[str]) -> tuple[str, str]:
    files = [name.lstrip("/") for name in names if name and not name.endswith("/")]
    candidates: list[tuple[str, str]] = []
    for name in files:
        if name == "level.dat":
            candidates.append(("", name))
        elif name.endswith("/level.dat"):
            candidates.append((name[:-len("level.dat")], name))
    if not candidates:
        raise ValueError("Archive does not contain level.dat.")
    roots = {root for root, _ in candidates}
    if len(roots) != 1:
        raise ValueError(f"Archive contains multiple world roots: {sorted(roots)}")
    return candidates[0]


def collect_region_coords(
    names: Iterable[str],
    world_root: str,
) -> tuple[str, list[tuple[int, int]]]:
    overworld_prefix = f"{world_root}region/"
    overworld: list[tuple[int, int]] = []
    any_regions: list[tuple[int, int]] = []
    any_root = ""

    for name in names:
        match = REGION_RE.search(name)
        if not match:
            continue
        coord = (int(match.group(1)), int(match.group(2)))
        any_regions.append(coord)
        if not any_root:
            any_root = name[:name.rfind("r.")]
        if name.startswith(overworld_prefix):
            overworld.append(coord)

    if overworld:
        return overworld_prefix, overworld
    if any_regions:
        return any_root, any_regions
    raise ValueError("Archive contains no Anvil region/*.mca files.")


def calculate_bounds(coords: list[tuple[int, int]]) -> RegionBounds:
    xs = [x for x, _ in coords]
    zs = [z for _, z in coords]
    min_rx, max_rx = min(xs), max(xs)
    min_rz, max_rz = min(zs), max(zs)
    min_bx = min_rx * 512
    max_bx = (max_rx + 1) * 512 - 1
    min_bz = min_rz * 512
    max_bz = (max_rz + 1) * 512 - 1
    return RegionBounds(
        min_rx, max_rx, min_rz, max_rz,
        min_bx, max_bx, min_bz, max_bz,
        max_bx - min_bx + 1,
        max_bz - min_bz + 1,
        len(coords),
    )


def inspect_archive(path: Path) -> WorldArchiveReport:
    if not path.is_file():
        raise FileNotFoundError(path)
    if not zipfile.is_zipfile(path):
        raise ValueError("Input is not a ZIP archive.")

    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        world_root, level_dat = common_world_root(names)
        region_root, coords = collect_region_coords(names, world_root)
        bounds = calculate_bounds(coords)
        warnings: list[str] = []

        if bounds.width_blocks < 28_000 or bounds.depth_blocks < 28_000:
            warnings.append(
                "Overworld region coverage is smaller than expected for a 30k x 30k map."
            )
        if bounds.width_blocks > 33_000 or bounds.depth_blocks > 33_000:
            warnings.append(
                "Overworld region coverage is larger than the expected 30k-scale envelope."
            )

        has_datapacks = any(
            name.startswith(f"{world_root}datapacks/") for name in names
        )
        has_dimensions = any(
            name.startswith(f"{world_root}dimensions/")
            or name.startswith(f"{world_root}DIM-1/")
            or name.startswith(f"{world_root}DIM1/")
            for name in names
        )

    return WorldArchiveReport(
        path.name,
        sha256_file(path),
        path.stat().st_size,
        len(names),
        world_root,
        level_dat,
        region_root,
        has_datapacks,
        has_dimensions,
        bounds,
        warnings,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="Creator-acquired Azari world ZIP")
    parser.add_argument("--output", type=Path, help="Write JSON report here")
    args = parser.parse_args()

    try:
        report = inspect_archive(args.archive)
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    payload = json.dumps(asdict(report), ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload, encoding="utf-8")
    else:
        print(payload, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
