#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import sys
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path, PurePosixPath

SUPPORTED_MODEL_SUFFIXES = {".gltf", ".glb", ".fbx", ".obj"}

PACKS = {
    "medieval_village_standard": {
        "archive_name": "medieval_village_megakitstandard.zip",
        "source_page": "https://opengameart.org/content/medieval-village-megakit",
        "direct_url": "https://opengameart.org/sites/default/files/medieval_village_megakitstandard.zip",
        "license": "CC0",
        "published_model_count": 176,
        "required_suffix_paths": [
            "glTF/Wall_Plaster_Straight.gltf",
            "glTF/Wall_Plaster_Window_Wide_Round.gltf",
            "glTF/Wall_Plaster_Door_Round.gltf",
            "glTF/Wall_UnevenBrick_Straight.gltf",
            "glTF/Roof_RoundTiles_6x8.gltf",
            "glTF/Roof_Front_Brick6.gltf",
            "glTF/Prop_Chimney.gltf",
            "glTF/Balcony_Cross_Straight.gltf",
            "glTF/Prop_Vine1.gltf",
            "glTF/Prop_Vine4.gltf",
        ],
        "required_basenames": [],
    },
    "fantasy_props_standard": {
        "archive_name": "fantasy_props_megakitstandard.zip",
        "source_page": "https://opengameart.org/content/fantasy-props-megakit",
        "direct_url": "https://opengameart.org/sites/default/files/fantasy_props_megakitstandard.zip",
        "license": "CC0",
        "published_model_count": 94,
        "required_suffix_paths": [],
        "required_basenames": [
            "Potion_1.gltf",
            "Potion_2.gltf",
            "Potion_3.gltf",
            "Potion_4.gltf",
        ],
    },
}


@dataclass(frozen=True)
class PackReport:
    pack_id: str
    archive: str
    sha256: str
    archive_bytes: int
    zip_entry_count: int
    source_page: str
    direct_url: str
    license: str
    published_model_count: int
    detected_unique_model_basenames: int
    required_files_found: list[str]
    missing_required_files: list[str]
    warnings: list[str]


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalized_names(archive: zipfile.ZipFile) -> list[str]:
    names: list[str] = []
    for info in archive.infolist():
        name = info.filename.replace("\\", "/").lstrip("/")
        relative = PurePosixPath(name)
        if relative.is_absolute() or ".." in relative.parts:
            raise ValueError(f"unsafe archive member: {info.filename}")
        if not info.is_dir():
            names.append(name)
    return names


def inspect_pack(pack_id: str, path: Path) -> PackReport:
    spec = PACKS[pack_id]
    if not path.is_file():
        raise FileNotFoundError(path)
    if not zipfile.is_zipfile(path):
        raise ValueError(f"not a ZIP archive: {path}")

    with zipfile.ZipFile(path) as archive:
        names = normalized_names(archive)

    found: list[str] = []
    missing: list[str] = []

    for suffix in spec["required_suffix_paths"]:
        matches = [name for name in names if name.lower().endswith(suffix.lower())]
        if matches:
            found.append(matches[0])
        else:
            missing.append(suffix)

    for basename in spec["required_basenames"]:
        matches = [
            name
            for name in names
            if PurePosixPath(name).name.lower() == basename.lower()
        ]
        if matches:
            found.append(matches[0])
        else:
            missing.append(basename)

    unique_models = {
        PurePosixPath(name).stem.lower()
        for name in names
        if PurePosixPath(name).suffix.lower() in SUPPORTED_MODEL_SUFFIXES
    }

    warnings: list[str] = []
    published_count = spec["published_model_count"]
    if len(unique_models) < published_count:
        warnings.append(
            "detected unique model basenames "
            f"{len(unique_models)} < published Standard count {published_count}"
        )

    return PackReport(
        pack_id=pack_id,
        archive=path.name,
        sha256=sha256_file(path),
        archive_bytes=path.stat().st_size,
        zip_entry_count=len(names),
        source_page=spec["source_page"],
        direct_url=spec["direct_url"],
        license=spec["license"],
        published_model_count=published_count,
        detected_unique_model_basenames=len(unique_models),
        required_files_found=sorted(found),
        missing_required_files=sorted(missing),
        warnings=warnings,
    )


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Verify creator-acquired R01 Quaternius Standard "
            "structure/prop archives."
        )
    )
    parser.add_argument("--medieval", type=Path)
    parser.add_argument("--props", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    if not args.medieval and not args.props:
        parser.error("provide --medieval and/or --props")

    reports: list[PackReport] = []
    try:
        if args.medieval:
            reports.append(inspect_pack("medieval_village_standard", args.medieval))
        if args.props:
            reports.append(inspect_pack("fantasy_props_standard", args.props))
    except Exception as exception:
        print(f"ERROR: {exception}", file=sys.stderr)
        return 2

    payload = {
        "schema_version": 1,
        "packs": [asdict(report) for report in reports],
        "ready_for_visual_review": all(
            not report.missing_required_files and not report.warnings
            for report in reports
        ),
    }
    rendered = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")

    return 1 if any(
        report.missing_required_files or report.warnings
        for report in reports
    ) else 0


if __name__ == "__main__":
    raise SystemExit(main())
