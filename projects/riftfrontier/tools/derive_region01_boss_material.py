#!/usr/bin/env python3
"""Derive the first Region 01 Dragon Evolved material candidate from the accepted CC0 source.

This is deliberately a deterministic source-material adaptation, not automatic art approval.
It verifies the pinned source receipt, decodes the exact PNG, produces a Minecraft-sized
albedo candidate, and records source-to-runtime provenance. Human Minecraft review remains
required before the production material gate may be opened.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

SOURCE_SHA256 = "cf323f68f6a784bf160d5394524ca2a606b6909b37c5a906601668afeb45bde4"
SOURCE_SIZE = 94658158
SOURCE_DIMENSIONS = (4096, 4096)
OUTPUT_SIZE = (1024, 1024)
ALGORITHM = "riftfrontier-dark-rock-dragon-albedo-v1"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--source", required=True, type=Path)
    p.add_argument("--output", required=True, type=Path)
    p.add_argument("--provenance", required=True, type=Path)
    args = p.parse_args()

    if args.source.stat().st_size != SOURCE_SIZE:
        raise SystemExit(f"source size mismatch: {args.source.stat().st_size} != {SOURCE_SIZE}")
    source_sha = sha256(args.source)
    if source_sha != SOURCE_SHA256:
        raise SystemExit(f"source SHA-256 mismatch: {source_sha}")

    with Image.open(args.source) as image:
        image.load()
        if image.size != SOURCE_DIMENSIONS:
            raise SystemExit(f"source dimensions mismatch: {image.size}")
        # Preserve the real rock structure while adapting it to Minecraft's distance readability.
        # LANCZOS is deterministic for the pinned Pillow version in the intake workflow.
        albedo = image.convert("RGB").resize(OUTPUT_SIZE, Image.Resampling.LANCZOS)
        albedo = ImageOps.autocontrast(albedo, cutoff=0)
        albedo = ImageEnhance.Contrast(albedo).enhance(1.12)
        albedo = ImageEnhance.Color(albedo).enhance(0.72)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        albedo.save(args.output, format="PNG", optimize=False, compress_level=9)

    output_sha = sha256(args.output)
    receipt = {
        "schema_version": 1,
        "kind": "riftfrontier:boss_material_derivation",
        "algorithm": ALGORITHM,
        "source": {
            "asset_id": "polyhaven:dark_rock",
            "sha256": SOURCE_SHA256,
            "byte_size": SOURCE_SIZE,
            "dimensions": list(SOURCE_DIMENSIONS),
            "license": "CC0",
        },
        "output": {
            "resource": "riftfrontier:textures/entity/region_01/dragon_evolved_dark_rock_v1.png",
            "sha256": output_sha,
            "dimensions": list(OUTPUT_SIZE),
            "color_mode": "RGB",
        },
        "operations": [
            "decode exact accepted 4096x4096 PNG",
            "convert to RGB",
            "LANCZOS resize to 1024x1024",
            "full-range autocontrast",
            "contrast multiplier 1.12",
            "color saturation multiplier 0.72",
            "PNG encode compress_level=9 optimize=false",
        ],
        "acceptance": "FIELD_REVIEW_CANDIDATE_ONLY",
        "human_visual_acceptance": False,
    }
    args.provenance.parent.mkdir(parents=True, exist_ok=True)
    args.provenance.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
