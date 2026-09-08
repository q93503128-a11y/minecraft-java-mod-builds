#!/usr/bin/env python3
"""Inspect glTF/GLB geometry and animation metadata for Riftfrontier asset intake.

This tool is a deterministic technical gate only. It does not approve visual style,
combat readability, licensing, or production selection.
"""

from __future__ import annotations

import argparse
import json
import math
import struct
import sys
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any

GLB_MAGIC = b"glTF"
GLB_VERSION = 2
JSON_CHUNK_TYPE = 0x4E4F534A
TRIANGLES_MODE = 4


class InspectionError(ValueError):
    pass


@dataclass(frozen=True)
class AssetMetrics:
    source: str
    format: str
    byte_size: int
    gltf_version: str
    node_count: int
    mesh_count: int
    primitive_count: int
    material_count: int
    texture_count: int
    image_count: int
    skin_count: int
    max_bones_per_skin: int
    animation_count: int
    animation_names: list[str]
    estimated_vertices: int
    estimated_triangles: int
    bounds_min: list[float] | None
    bounds_max: list[float] | None
    bounds_size: list[float] | None


@dataclass(frozen=True)
class GateIssue:
    code: str
    message: str


def _load_json_document(path: Path) -> tuple[dict[str, Any], str, int]:
    raw = path.read_bytes()
    suffix = path.suffix.lower()
    if suffix == ".glb":
        return _parse_glb_json(raw), "glb", len(raw)
    if suffix == ".gltf":
        try:
            document = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise InspectionError(f"invalid glTF JSON: {exc}") from exc
        if not isinstance(document, dict):
            raise InspectionError("glTF root must be a JSON object")
        return document, "gltf", len(raw)
    raise InspectionError("expected a .glb or .gltf file")


def _parse_glb_json(raw: bytes) -> dict[str, Any]:
    if len(raw) < 12:
        raise InspectionError("GLB is shorter than the 12-byte header")
    magic, version, declared_length = struct.unpack_from("<4sII", raw, 0)
    if magic != GLB_MAGIC:
        raise InspectionError("invalid GLB magic")
    if version != GLB_VERSION:
        raise InspectionError(f"unsupported GLB version {version}; expected 2")
    if declared_length != len(raw):
        raise InspectionError(
            f"GLB declared length {declared_length} does not match actual length {len(raw)}"
        )

    offset = 12
    json_chunk: bytes | None = None
    while offset < len(raw):
        if offset + 8 > len(raw):
            raise InspectionError("truncated GLB chunk header")
        chunk_length, chunk_type = struct.unpack_from("<II", raw, offset)
        offset += 8
        end = offset + chunk_length
        if end > len(raw):
            raise InspectionError("truncated GLB chunk payload")
        payload = raw[offset:end]
        offset = end
        if chunk_type == JSON_CHUNK_TYPE and json_chunk is None:
            json_chunk = payload

    if json_chunk is None:
        raise InspectionError("GLB has no JSON chunk")
    try:
        document = json.loads(json_chunk.rstrip(b" \t\r\n\x00").decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise InspectionError(f"invalid GLB JSON chunk: {exc}") from exc
    if not isinstance(document, dict):
        raise InspectionError("GLB JSON root must be an object")
    return document


def _list(document: dict[str, Any], key: str) -> list[Any]:
    value = document.get(key, [])
    if value is None:
        return []
    if not isinstance(value, list):
        raise InspectionError(f"{key} must be an array")
    return value


def _accessor(document: dict[str, Any], index: Any) -> dict[str, Any] | None:
    if not isinstance(index, int):
        return None
    accessors = _list(document, "accessors")
    if index < 0 or index >= len(accessors):
        return None
    value = accessors[index]
    return value if isinstance(value, dict) else None


def _finite_vec3(value: Any) -> list[float] | None:
    if not isinstance(value, list) or len(value) < 3:
        return None
    result: list[float] = []
    for component in value[:3]:
        if not isinstance(component, (int, float)) or not math.isfinite(float(component)):
            return None
        result.append(float(component))
    return result


def inspect(path: Path) -> AssetMetrics:
    document, file_format, byte_size = _load_json_document(path)
    asset = document.get("asset")
    if not isinstance(asset, dict) or str(asset.get("version", "")) != "2.0":
        raise InspectionError("asset.version must be glTF 2.0")

    nodes = _list(document, "nodes")
    meshes = _list(document, "meshes")
    materials = _list(document, "materials")
    textures = _list(document, "textures")
    images = _list(document, "images")
    skins = _list(document, "skins")
    animations = _list(document, "animations")

    primitive_count = 0
    estimated_vertices = 0
    estimated_triangles = 0
    bound_mins: list[list[float]] = []
    bound_maxs: list[list[float]] = []

    for mesh in meshes:
        if not isinstance(mesh, dict):
            continue
        primitives = mesh.get("primitives", [])
        if not isinstance(primitives, list):
            raise InspectionError("mesh.primitives must be an array")
        primitive_count += len(primitives)
        for primitive in primitives:
            if not isinstance(primitive, dict):
                continue
            attributes = primitive.get("attributes", {})
            if isinstance(attributes, dict):
                position_accessor = _accessor(document, attributes.get("POSITION"))
                if position_accessor is not None:
                    count = position_accessor.get("count")
                    if isinstance(count, int) and count >= 0:
                        estimated_vertices += count
                    min_vec = _finite_vec3(position_accessor.get("min"))
                    max_vec = _finite_vec3(position_accessor.get("max"))
                    if min_vec and max_vec:
                        bound_mins.append(min_vec)
                        bound_maxs.append(max_vec)

            mode = primitive.get("mode", TRIANGLES_MODE)
            indices_accessor = _accessor(document, primitive.get("indices"))
            if mode == TRIANGLES_MODE and indices_accessor is not None:
                count = indices_accessor.get("count")
                if isinstance(count, int) and count >= 0:
                    estimated_triangles += count // 3
            elif mode == TRIANGLES_MODE and isinstance(attributes, dict):
                position_accessor = _accessor(document, attributes.get("POSITION"))
                if position_accessor is not None:
                    count = position_accessor.get("count")
                    if isinstance(count, int) and count >= 0:
                        estimated_triangles += count // 3

    max_bones = 0
    for skin in skins:
        if not isinstance(skin, dict):
            continue
        joints = skin.get("joints", [])
        if isinstance(joints, list):
            max_bones = max(max_bones, len(joints))

    animation_names = []
    for index, animation in enumerate(animations):
        if isinstance(animation, dict):
            name = animation.get("name")
            if isinstance(name, str) and name.strip():
                animation_names.append(name.strip())
            else:
                animation_names.append(f"<unnamed:{index}>")
        else:
            animation_names.append(f"<unnamed:{index}>")

    bounds_min = bounds_max = bounds_size = None
    if bound_mins and bound_maxs:
        bounds_min = [min(v[i] for v in bound_mins) for i in range(3)]
        bounds_max = [max(v[i] for v in bound_maxs) for i in range(3)]
        bounds_size = [bounds_max[i] - bounds_min[i] for i in range(3)]

    return AssetMetrics(
        source=str(path),
        format=file_format,
        byte_size=byte_size,
        gltf_version="2.0",
        node_count=len(nodes),
        mesh_count=len(meshes),
        primitive_count=primitive_count,
        material_count=len(materials),
        texture_count=len(textures),
        image_count=len(images),
        skin_count=len(skins),
        max_bones_per_skin=max_bones,
        animation_count=len(animations),
        animation_names=animation_names,
        estimated_vertices=estimated_vertices,
        estimated_triangles=estimated_triangles,
        bounds_min=bounds_min,
        bounds_max=bounds_max,
        bounds_size=bounds_size,
    )


def evaluate(metrics: AssetMetrics, args: argparse.Namespace) -> list[GateIssue]:
    issues: list[GateIssue] = []
    if args.require_skin and metrics.skin_count == 0:
        issues.append(GateIssue("GLTF_SKIN_REQUIRED", "asset has no skin"))
    if metrics.animation_count < args.min_animations:
        issues.append(
            GateIssue(
                "GLTF_ANIMATION_COUNT_LOW",
                f"animation count {metrics.animation_count} < required {args.min_animations}",
            )
        )
    if args.max_triangles is not None and metrics.estimated_triangles > args.max_triangles:
        issues.append(
            GateIssue(
                "GLTF_TRIANGLE_BUDGET_EXCEEDED",
                f"estimated triangles {metrics.estimated_triangles} > {args.max_triangles}",
            )
        )
    if args.max_materials is not None and metrics.material_count > args.max_materials:
        issues.append(
            GateIssue(
                "GLTF_MATERIAL_BUDGET_EXCEEDED",
                f"materials {metrics.material_count} > {args.max_materials}",
            )
        )
    if args.max_bones is not None and metrics.max_bones_per_skin > args.max_bones:
        issues.append(
            GateIssue(
                "GLTF_BONE_BUDGET_EXCEEDED",
                f"max bones per skin {metrics.max_bones_per_skin} > {args.max_bones}",
            )
        )

    lowered_names = [name.casefold() for name in metrics.animation_names]
    for token in args.require_animation:
        wanted = token.casefold()
        if not any(wanted in name for name in lowered_names):
            issues.append(
                GateIssue(
                    "GLTF_REQUIRED_ANIMATION_MISSING",
                    f"no animation name contains required token {token!r}",
                )
            )
    if args.require_named_animations and any(name.startswith("<unnamed:") for name in metrics.animation_names):
        issues.append(
            GateIssue(
                "GLTF_UNNAMED_ANIMATION",
                "one or more animation clips are unnamed",
            )
        )
    return issues


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Inspect a glTF/GLB candidate for Riftfrontier technical asset intake."
    )
    parser.add_argument("asset", type=Path)
    parser.add_argument("--json", action="store_true", help="emit machine-readable JSON")
    parser.add_argument("--require-skin", action="store_true")
    parser.add_argument("--require-named-animations", action="store_true")
    parser.add_argument("--min-animations", type=int, default=0)
    parser.add_argument("--max-triangles", type=int)
    parser.add_argument("--max-materials", type=int)
    parser.add_argument("--max-bones", type=int)
    parser.add_argument(
        "--require-animation",
        action="append",
        default=[],
        metavar="TOKEN",
        help="require at least one animation name containing TOKEN; repeatable",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    if args.min_animations < 0:
        parser.error("--min-animations must be non-negative")
    for option in ("max_triangles", "max_materials", "max_bones"):
        value = getattr(args, option)
        if value is not None and value < 0:
            parser.error(f"--{option.replace('_', '-')} must be non-negative")

    try:
        metrics = inspect(args.asset)
        issues = evaluate(metrics, args)
    except (OSError, InspectionError) as exc:
        if args.json:
            print(json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=False, indent=2))
        else:
            print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    payload = {
        "ok": not issues,
        "metrics": asdict(metrics),
        "issues": [asdict(issue) for issue in issues],
        "scope_note": (
            "Technical metadata gate only; visual silhouette, hitbox/animation alignment, "
            "license approval, and production selection still require separate review."
        ),
    }
    if args.json:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
    else:
        print(f"{metrics.source}:")
        print(f"  format={metrics.format} bytes={metrics.byte_size}")
        print(
            f"  nodes={metrics.node_count} meshes={metrics.mesh_count} "
            f"primitives={metrics.primitive_count} materials={metrics.material_count}"
        )
        print(
            f"  skins={metrics.skin_count} max_bones={metrics.max_bones_per_skin} "
            f"animations={metrics.animation_count}"
        )
        print(
            f"  estimated_vertices={metrics.estimated_vertices} "
            f"estimated_triangles={metrics.estimated_triangles}"
        )
        print(f"  animation_names={metrics.animation_names}")
        if metrics.bounds_size is not None:
            print(
                f"  bounds_min={metrics.bounds_min} bounds_max={metrics.bounds_max} "
                f"size={metrics.bounds_size}"
            )
        if issues:
            for issue in issues:
                print(f"FAIL {issue.code}: {issue.message}", file=sys.stderr)
        else:
            print("PASS: requested technical gates satisfied")
    return 0 if not issues else 1


if __name__ == "__main__":
    raise SystemExit(main())
