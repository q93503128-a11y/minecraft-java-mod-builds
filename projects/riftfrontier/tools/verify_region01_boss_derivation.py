#!/usr/bin/env python3
"""Verify a Region 01 boss geometry derivation against a pinned production receipt."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import math
import sys
from pathlib import Path
from typing import Any

FORBIDDEN_TOP_LEVEL = ("materials", "textures", "images", "samplers")
FORBIDDEN_PRIMITIVE_FIELDS = ("material",)


class VerificationError(ValueError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def _json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise VerificationError("BOSS_DERIVATION_READ_FAILED", f"{path}: {exc}") from exc
    if not isinstance(value, dict):
        raise VerificationError("BOSS_DERIVATION_JSON_INVALID", f"{path}: root must be an object")
    return value


def _sha256(path: Path) -> str:
    try:
        return hashlib.sha256(path.read_bytes()).hexdigest()
    except OSError as exc:
        raise VerificationError("BOSS_DERIVATION_READ_FAILED", f"{path}: {exc}") from exc


def _number_list(value: Any, length: int, label: str) -> list[float]:
    if not isinstance(value, list) or len(value) != length:
        raise VerificationError("BOSS_DERIVATION_RECEIPT_INVALID", f"{label} must contain {length} numbers")
    result = []
    for item in value:
        if not isinstance(item, (int, float)) or isinstance(item, bool) or not math.isfinite(float(item)):
            raise VerificationError("BOSS_DERIVATION_RECEIPT_INVALID", f"{label} must contain finite numbers")
        result.append(float(item))
    return result


def _expect_equal(actual: Any, expected: Any, code: str, label: str) -> None:
    if actual != expected:
        raise VerificationError(code, f"{label}: expected {expected!r}, got {actual!r}")


def verify(receipt_path: Path, geometry_path: Path, provenance_path: Path) -> dict[str, Any]:
    receipt = _json(receipt_path)
    geometry = _json(geometry_path)
    provenance = _json(provenance_path)

    _expect_equal(receipt.get("kind"), "riftfrontier:boss_geometry_acceptance", "BOSS_DERIVATION_RECEIPT_INVALID", "receipt.kind")
    _expect_equal(receipt.get("schema_version"), 1, "BOSS_DERIVATION_RECEIPT_INVALID", "receipt.schema_version")

    source = receipt.get("source")
    output = receipt.get("output")
    expect = receipt.get("expect")
    if not isinstance(source, dict) or not isinstance(output, dict) or not isinstance(expect, dict):
        raise VerificationError("BOSS_DERIVATION_RECEIPT_INVALID", "source/output/expect must be objects")

    actual_geometry_sha = _sha256(geometry_path)
    actual_provenance_sha = _sha256(provenance_path)
    _expect_equal(actual_geometry_sha, output.get("geometry_sha256"), "BOSS_DERIVATION_SHA256_MISMATCH", "geometry SHA-256")
    _expect_equal(actual_provenance_sha, output.get("provenance_sha256"), "BOSS_DERIVATION_SHA256_MISMATCH", "provenance SHA-256")
    _expect_equal(provenance.get("source_sha256"), source.get("sha256"), "BOSS_DERIVATION_PROVENANCE_MISMATCH", "provenance source SHA-256")
    _expect_equal(provenance.get("output_sha256"), actual_geometry_sha, "BOSS_DERIVATION_PROVENANCE_MISMATCH", "provenance output SHA-256")

    for key in FORBIDDEN_TOP_LEVEL:
        if key in geometry:
            raise VerificationError("BOSS_DERIVATION_ART_POLICY_VIOLATION", f"forbidden top-level {key!r} is present")
    meshes = geometry.get("meshes", [])
    if not isinstance(meshes, list):
        raise VerificationError("BOSS_DERIVATION_GLTF_INVALID", "meshes must be an array")
    for mesh_index, mesh in enumerate(meshes):
        if not isinstance(mesh, dict) or not isinstance(mesh.get("primitives"), list):
            raise VerificationError("BOSS_DERIVATION_GLTF_INVALID", f"meshes[{mesh_index}] is invalid")
        for primitive_index, primitive in enumerate(mesh["primitives"]):
            if not isinstance(primitive, dict):
                raise VerificationError("BOSS_DERIVATION_GLTF_INVALID", f"meshes[{mesh_index}].primitives[{primitive_index}] is invalid")
            for field in FORBIDDEN_PRIMITIVE_FIELDS:
                if field in primitive:
                    raise VerificationError("BOSS_DERIVATION_ART_POLICY_VIOLATION", f"forbidden primitive field {field!r} is present")

    skins = geometry.get("skins", [])
    animations = geometry.get("animations", [])
    nodes = geometry.get("nodes", [])
    accessors = geometry.get("accessors", [])
    buffer_views = geometry.get("bufferViews", [])
    _expect_equal(len(meshes), expect.get("mesh_count"), "BOSS_DERIVATION_STRUCTURE_MISMATCH", "mesh count")
    _expect_equal(len(skins), expect.get("skin_count"), "BOSS_DERIVATION_STRUCTURE_MISMATCH", "skin count")
    _expect_equal(len(nodes), expect.get("node_count"), "BOSS_DERIVATION_STRUCTURE_MISMATCH", "node count")
    _expect_equal(len(accessors), expect.get("accessor_count"), "BOSS_DERIVATION_STRUCTURE_MISMATCH", "accessor count")
    _expect_equal(len(buffer_views), expect.get("buffer_view_count"), "BOSS_DERIVATION_STRUCTURE_MISMATCH", "bufferView count")
    _expect_equal(sum(len(skin.get("joints", [])) for skin in skins if isinstance(skin, dict)), expect.get("joint_count"), "BOSS_DERIVATION_STRUCTURE_MISMATCH", "joint count")

    names = [animation.get("name") for animation in animations if isinstance(animation, dict)]
    _expect_equal(names, expect.get("animations"), "BOSS_DERIVATION_STRUCTURE_MISMATCH", "animation names")

    primitive = meshes[0]["primitives"][0]
    position_accessor_index = primitive.get("attributes", {}).get("POSITION")
    if not isinstance(position_accessor_index, int) or not 0 <= position_accessor_index < len(accessors):
        raise VerificationError("BOSS_DERIVATION_GLTF_INVALID", "POSITION accessor is missing or invalid")
    position_accessor = accessors[position_accessor_index]
    actual_min = _number_list(position_accessor.get("min"), 3, "POSITION.min")
    actual_max = _number_list(position_accessor.get("max"), 3, "POSITION.max")
    expected_min = _number_list(expect.get("bounds_min"), 3, "expect.bounds_min")
    expected_max = _number_list(expect.get("bounds_max"), 3, "expect.bounds_max")
    for label, actual, expected_value in (("bounds_min", actual_min, expected_min), ("bounds_max", actual_max, expected_max)):
        if any(abs(a - b) > 1e-6 for a, b in zip(actual, expected_value)):
            raise VerificationError("BOSS_DERIVATION_BOUNDS_MISMATCH", f"{label}: expected {expected_value!r}, got {actual!r}")

    buffers = geometry.get("buffers", [])
    if len(buffers) != 1 or not isinstance(buffers[0], dict):
        raise VerificationError("BOSS_DERIVATION_GLTF_INVALID", "exactly one embedded buffer is required")
    uri = buffers[0].get("uri")
    if not isinstance(uri, str) or not uri.startswith("data:application/octet-stream;base64,"):
        raise VerificationError("BOSS_DERIVATION_GLTF_INVALID", "buffer must be an embedded octet-stream data URI")
    try:
        decoded = base64.b64decode(uri.split(",", 1)[1], validate=True)
    except ValueError as exc:
        raise VerificationError("BOSS_DERIVATION_GLTF_INVALID", "embedded buffer is not valid base64") from exc
    _expect_equal(len(decoded), expect.get("embedded_buffer_bytes"), "BOSS_DERIVATION_STRUCTURE_MISMATCH", "embedded buffer bytes")

    return {
        "geometry_sha256": actual_geometry_sha,
        "provenance_sha256": actual_provenance_sha,
        "mesh_count": len(meshes),
        "joint_count": sum(len(skin.get("joints", [])) for skin in skins if isinstance(skin, dict)),
        "animation_count": len(animations),
        "art_payload_stripped": True,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--receipt", required=True, type=Path)
    parser.add_argument("--geometry", required=True, type=Path)
    parser.add_argument("--provenance", required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        result = verify(args.receipt, args.geometry, args.provenance)
    except VerificationError as exc:
        print(f"{exc.code}: {exc}", file=sys.stderr)
        return 2
    print(json.dumps(result, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
