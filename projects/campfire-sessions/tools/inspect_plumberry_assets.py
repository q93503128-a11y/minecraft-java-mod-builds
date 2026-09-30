#!/usr/bin/env python3
"""Read-only inspector for locally downloaded Plumberry Plains resident packs.

It never extracts or republishes the source assets. It records provenance hashes,
checks TERMS/README/CONTENTS, and reads GLB metadata to verify the shared rig,
prop sockets, and required lifestyle animations before any Minecraft conversion.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import struct
import sys
import zipfile
from typing import BinaryIO, Iterable

SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
DEFAULT_CONTRACT = SCRIPT_DIR / "plumberry_asset_contract.json"
MAX_TEXT_BYTES = 2 * 1024 * 1024


def sha256_file(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fp:
        for chunk in iter(lambda: fp.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalize(value: str) -> str:
    value = value.lower().replace("\\", "/")
    value = re.sub(r"\.[^./]+$", "", value)
    return re.sub(r"[^a-z0-9]+", "-", value).strip("-")


def validate_contract(data: dict) -> None:
    required = {"format", "sources", "required_bones", "required_sockets", "required_animations"}
    missing = sorted(required - set(data))
    if missing:
        raise ValueError(f"contract missing keys: {missing}")
    if data["format"] != 1:
        raise ValueError(f"unsupported contract format: {data['format']!r}")
    if [source.get("id") for source in data["sources"]] != ["vol1", "vol2"]:
        raise ValueError("contract must define vol1 then vol2")
    characters = []
    for source in data["sources"]:
        if len(source.get("characters", [])) != 10:
            raise ValueError(f"{source.get('id')}: expected 10 characters")
        characters.extend(source["characters"])
    if len(characters) != 20 or len(set(characters)) != 20:
        raise ValueError("contract must define 20 unique residents")
    for key in ("required_bones", "required_sockets", "required_animations"):
        values = data[key]
        if not values or len(values) != len(set(values)):
            raise ValueError(f"{key} must be a non-empty unique list")


def load_contract(path: pathlib.Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    validate_contract(data)
    return data


def contract_summary(contract: dict) -> dict:
    characters = [c for source in contract["sources"] for c in source["characters"]]
    return {
        "source_count": len(contract["sources"]),
        "character_count": len(characters),
        "characters": characters,
        "required_bones": contract["required_bones"],
        "required_sockets": contract["required_sockets"],
        "required_animations": contract["required_animations"],
    }


class SourceView:
    def names(self) -> list[str]:
        raise NotImplementedError

    def open(self, name: str) -> BinaryIO:
        raise NotImplementedError

    def read_text(self, name: str) -> str:
        with self.open(name) as fp:
            raw = fp.read(MAX_TEXT_BYTES + 1)
        if len(raw) > MAX_TEXT_BYTES:
            raise ValueError(f"text file too large: {name}")
        return raw.decode("utf-8", errors="replace").replace("\r\n", "\n")

    def close(self) -> None:
        pass


class ZipView(SourceView):
    def __init__(self, path: pathlib.Path):
        self.zf = zipfile.ZipFile(path)

    def names(self) -> list[str]:
        return [name for name in self.zf.namelist() if not name.endswith("/")]

    def open(self, name: str) -> BinaryIO:
        return self.zf.open(name, "r")

    def close(self) -> None:
        self.zf.close()


class DirectoryView(SourceView):
    def __init__(self, path: pathlib.Path):
        self.path = path

    def names(self) -> list[str]:
        return [p.relative_to(self.path).as_posix() for p in self.path.rglob("*") if p.is_file()]

    def open(self, name: str) -> BinaryIO:
        return (self.path / pathlib.PurePosixPath(name)).open("rb")


def read_exact(fp: BinaryIO, size: int) -> bytes:
    data = fp.read(size)
    if len(data) != size:
        raise ValueError(f"unexpected EOF: wanted {size}, got {len(data)}")
    return data


def read_glb_json(fp: BinaryIO) -> dict:
    magic, version, total = struct.unpack("<4sII", read_exact(fp, 12))
    if magic != b"glTF" or version != 2:
        raise ValueError(f"unsupported GLB header: magic={magic!r} version={version}")
    consumed = 12
    while consumed < total:
        chunk_size, chunk_type = struct.unpack("<II", read_exact(fp, 8))
        consumed += 8
        chunk = read_exact(fp, chunk_size)
        consumed += chunk_size
        if chunk_type == 0x4E4F534A:
            return json.loads(chunk.rstrip(b"\x00 \t\r\n").decode("utf-8"))
    raise ValueError("GLB JSON chunk not found")


def node_names(document: dict) -> set[str]:
    return {
        node["name"].lower()
        for node in document.get("nodes", [])
        if isinstance(node, dict) and isinstance(node.get("name"), str)
    }


def animation_names(document: dict) -> set[str]:
    values = set()
    for animation in document.get("animations", []):
        if isinstance(animation, dict) and isinstance(animation.get("name"), str):
            values.add(normalize(animation["name"]).replace("-", "_"))
    return values


def find_docs(names: Iterable[str]) -> dict[str, list[str]]:
    result = {"terms.md": [], "readme.md": [], "contents.md": []}
    for name in names:
        base = pathlib.PurePosixPath(name).name.lower()
        if base in result:
            result[base].append(name)
    return result


def find_characters(names: Iterable[str], expected: list[str]) -> dict[str, list[str]]:
    found = {slug: [] for slug in expected}
    for name in names:
        norm = normalize(name)
        for slug in expected:
            if normalize(slug) in norm:
                found[slug].append(name)
    return {slug: paths for slug, paths in found.items() if paths}


def license_signals(text: str) -> dict[str, bool]:
    low = " ".join(text.lower().split())
    return {
        "mentions_personal": "personal" in low,
        "mentions_commercial": "commercial" in low,
        "mentions_modify_or_adapt": "modify" in low or "adapt" in low,
        "mentions_embedded_builds": "embedded" in low and ("build" in low or "game" in low),
        "mentions_raw_redistribution_restriction": (
            "raw" in low and any(token in low for token in ("redistribut", "repackag", "resell"))
        ),
        "mentions_ai_training_restriction": "ai" in low and "train" in low,
    }


def inspect(path: pathlib.Path, contract: dict) -> dict:
    if path.is_dir():
        view: SourceView = DirectoryView(path)
        archive_hash = None
    elif zipfile.is_zipfile(path):
        view = ZipView(path)
        archive_hash = sha256_file(path)
    else:
        raise ValueError("expected a ZIP archive or extracted directory")

    try:
        names = view.names()
        docs = find_docs(names)
        terms = "\n\n".join(view.read_text(name) for name in docs["terms.md"])
        expected = [c for source in contract["sources"] for c in source["characters"]]
        found = find_characters(names, expected)
        glbs = [name for name in names if name.lower().endswith(".glb")]
        glb_reports = []

        for name in glbs:
            try:
                with view.open(name) as fp:
                    document = read_glb_json(fp)
                nodes = node_names(document)
                animations = animation_names(document)
                glb_reports.append({
                    "path": name,
                    "matched_characters": [slug for slug in expected if normalize(slug) in normalize(name)],
                    "node_count": len(document.get("nodes", [])),
                    "mesh_count": len(document.get("meshes", [])),
                    "material_count": len(document.get("materials", [])),
                    "animation_count": len(document.get("animations", [])),
                    "missing_bones": [b for b in contract["required_bones"] if b.lower() not in nodes],
                    "missing_sockets": [s for s in contract["required_sockets"] if s.lower() not in nodes],
                    "missing_animations": [
                        a for a in contract["required_animations"]
                        if normalize(a).replace("-", "_") not in animations
                    ],
                })
            except Exception as exc:
                glb_reports.append({"path": name, "error": f"{type(exc).__name__}: {exc}"})

        failures = []
        if not docs["terms.md"]:
            failures.append("TERMS.md not found")
        if not docs["readme.md"]:
            failures.append("README.md not found")
        if not docs["contents.md"]:
            failures.append("CONTENTS.md not found")
        if not glbs:
            failures.append("no GLB files found")
        broken = [
            row["path"] for row in glb_reports
            if row.get("error") or row.get("missing_bones") or row.get("missing_sockets") or row.get("missing_animations")
        ]
        if broken:
            failures.append(f"{len(broken)} GLB file(s) fail rig/socket/animation contract")

        return {
            "input": str(path),
            "sha256": archive_hash,
            "file_count": len(names),
            "documents": docs,
            "license_signals": license_signals(terms) if terms else {},
            "characters_detected": sorted(found),
            "character_evidence": found,
            "format_counts": {
                "glb": len(glbs),
                "fbx": sum(name.lower().endswith(".fbx") for name in names),
                "obj": sum(name.lower().endswith(".obj") for name in names),
            },
            "glb": glb_reports,
            "failures": failures,
        }
    finally:
        view.close()


def print_report(report: dict) -> None:
    contract = report["contract"]
    print(
        f"contract residents={contract['character_count']} "
        f"bones={len(contract['required_bones'])} "
        f"sockets={len(contract['required_sockets'])} "
        f"animations={len(contract['required_animations'])}"
    )
    for item in report["inputs"]:
        print(f"\n## {item['input']}")
        if item["sha256"]:
            print(f"sha256={item['sha256']}")
        counts = item["format_counts"]
        print(f"files={item['file_count']} glb={counts['glb']} fbx={counts['fbx']} obj={counts['obj']}")
        print(f"characters_detected={len(item['characters_detected'])}: {', '.join(item['characters_detected'])}")
        print(f"TERMS={item['documents']['terms.md'] or 'MISSING'}")
        print(f"README={item['documents']['readme.md'] or 'MISSING'}")
        print(f"CONTENTS={item['documents']['contents.md'] or 'MISSING'}")
        if item["license_signals"]:
            print("license_signals=" + json.dumps(item["license_signals"], sort_keys=True))
        if item["failures"]:
            for failure in item["failures"]:
                print(f"FAIL {failure}")
        else:
            print("asset_contract=PASS")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("inputs", nargs="*", help="Plumberry ZIP files or extracted directories")
    parser.add_argument("--contract", type=pathlib.Path, default=DEFAULT_CONTRACT)
    parser.add_argument("--contract-only", action="store_true")
    parser.add_argument("--json-output", type=pathlib.Path)
    args = parser.parse_args()

    contract = load_contract(args.contract)
    report = {"contract": contract_summary(contract), "inputs": []}

    if args.contract_only:
        print(json.dumps(report["contract"], indent=2))
        return 0
    if not args.inputs:
        parser.error("provide ZIP/directory inputs, or use --contract-only")

    failed = False
    for raw in args.inputs:
        path = pathlib.Path(raw)
        if not path.exists():
            print(f"ERROR missing input: {path}", file=sys.stderr)
            failed = True
            continue
        try:
            item = inspect(path, contract)
            report["inputs"].append(item)
            failed = failed or bool(item["failures"])
        except Exception as exc:
            print(f"ERROR {path}: {type(exc).__name__}: {exc}", file=sys.stderr)
            failed = True

    print_report(report)
    if args.json_output:
        args.json_output.parent.mkdir(parents=True, exist_ok=True)
        args.json_output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
