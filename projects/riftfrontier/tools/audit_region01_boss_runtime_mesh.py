#!/usr/bin/env python3
"""Fail-closed audit for lossless rigid-bone PolyMesh eligibility.

This gate answers one narrow production question: can a skinned glTF primitive be
represented by a renderer that attaches an entire PolyMesh rigidly to one bone
without changing vertex deformation? It does not judge visual quality or claim
that a compatible renderer is selected.
"""

from __future__ import annotations

import argparse
import base64
import json
import math
import struct
import sys
from pathlib import Path
from typing import Any


class AuditError(RuntimeError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


_COMPONENT = {
    5120: ("b", 1),
    5121: ("B", 1),
    5122: ("h", 2),
    5123: ("H", 2),
    5125: ("I", 4),
    5126: ("f", 4),
}
_COMPONENTS = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}


def _load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AuditError("BOSS_RUNTIME_MESH_INVALID_GLTF", f"cannot read glTF JSON: {exc}") from exc
    if not isinstance(value, dict) or value.get("asset", {}).get("version") != "2.0":
        raise AuditError("BOSS_RUNTIME_MESH_INVALID_GLTF", "expected glTF 2.0 JSON object")
    return value


def _decode_buffer(gltf_path: Path, buffer: dict[str, Any]) -> bytes:
    uri = buffer.get("uri")
    if not isinstance(uri, str):
        raise AuditError("BOSS_RUNTIME_MESH_EXTERNAL_BUFFER", "GLB/no-URI buffers are outside this JSON gate")
    if uri.startswith("data:"):
        marker = ";base64,"
        if marker not in uri:
            raise AuditError("BOSS_RUNTIME_MESH_INVALID_BUFFER", "embedded buffer must use base64 data URI")
        try:
            return base64.b64decode(uri.split(marker, 1)[1], validate=True)
        except ValueError as exc:
            raise AuditError("BOSS_RUNTIME_MESH_INVALID_BUFFER", "invalid base64 buffer") from exc
    candidate = (gltf_path.parent / uri).resolve()
    if gltf_path.parent.resolve() not in candidate.parents and candidate != gltf_path.parent.resolve():
        raise AuditError("BOSS_RUNTIME_MESH_EXTERNAL_BUFFER", "buffer path escapes source directory")
    try:
        return candidate.read_bytes()
    except OSError as exc:
        raise AuditError("BOSS_RUNTIME_MESH_EXTERNAL_BUFFER", f"cannot read buffer {uri}: {exc}") from exc


def _normalized(value: int, component_type: int) -> float:
    if component_type == 5120:
        return max(value / 127.0, -1.0)
    if component_type == 5121:
        return value / 255.0
    if component_type == 5122:
        return max(value / 32767.0, -1.0)
    if component_type == 5123:
        return value / 65535.0
    raise AuditError("BOSS_RUNTIME_MESH_UNSUPPORTED_ACCESSOR", f"unsupported normalized component type {component_type}")


def _accessor_values(gltf: dict[str, Any], buffers: list[bytes], accessor_index: int) -> list[tuple[float | int, ...]]:
    accessors = gltf.get("accessors", [])
    views = gltf.get("bufferViews", [])
    if not (0 <= accessor_index < len(accessors)):
        raise AuditError("BOSS_RUNTIME_MESH_INVALID_ACCESSOR", f"accessor {accessor_index} out of range")
    accessor = accessors[accessor_index]
    if "sparse" in accessor:
        raise AuditError("BOSS_RUNTIME_MESH_UNSUPPORTED_ACCESSOR", "sparse accessors are not accepted")
    view_index = accessor.get("bufferView")
    if not isinstance(view_index, int) or not (0 <= view_index < len(views)):
        raise AuditError("BOSS_RUNTIME_MESH_INVALID_ACCESSOR", f"accessor {accessor_index} has invalid bufferView")
    view = views[view_index]
    buffer_index = view.get("buffer", 0)
    if not isinstance(buffer_index, int) or not (0 <= buffer_index < len(buffers)):
        raise AuditError("BOSS_RUNTIME_MESH_INVALID_ACCESSOR", "invalid buffer index")
    component_type = accessor.get("componentType")
    accessor_type = accessor.get("type")
    if component_type not in _COMPONENT or accessor_type not in _COMPONENTS:
        raise AuditError("BOSS_RUNTIME_MESH_UNSUPPORTED_ACCESSOR", f"unsupported accessor format {component_type}/{accessor_type}")
    count = accessor.get("count")
    if not isinstance(count, int) or count < 0:
        raise AuditError("BOSS_RUNTIME_MESH_INVALID_ACCESSOR", "invalid accessor count")
    fmt, width = _COMPONENT[component_type]
    components = _COMPONENTS[accessor_type]
    element_size = width * components
    stride = view.get("byteStride", element_size)
    if not isinstance(stride, int) or stride < element_size:
        raise AuditError("BOSS_RUNTIME_MESH_INVALID_ACCESSOR", "invalid byteStride")
    start = int(view.get("byteOffset", 0)) + int(accessor.get("byteOffset", 0))
    data = buffers[buffer_index]
    values: list[tuple[float | int, ...]] = []
    unpack_fmt = "<" + fmt * components
    normalized = bool(accessor.get("normalized", False))
    for i in range(count):
        offset = start + i * stride
        if offset + element_size > len(data):
            raise AuditError("BOSS_RUNTIME_MESH_INVALID_ACCESSOR", "accessor exceeds buffer bounds")
        raw = struct.unpack_from(unpack_fmt, data, offset)
        if normalized:
            raw = tuple(_normalized(int(v), component_type) for v in raw)
        values.append(tuple(raw))
    return values


def audit(path: Path, epsilon: float = 1.0e-5) -> dict[str, Any]:
    gltf = _load_json(path)
    raw_buffers = gltf.get("buffers", [])
    if not isinstance(raw_buffers, list):
        raise AuditError("BOSS_RUNTIME_MESH_INVALID_GLTF", "buffers must be an array")
    buffers = [_decode_buffer(path, b) for b in raw_buffers]

    meshes = gltf.get("meshes", [])
    nodes = gltf.get("nodes", [])
    skins = gltf.get("skins", [])
    mesh_skin_nodes: dict[int, list[int]] = {}
    for node_index, node in enumerate(nodes):
        mesh_index = node.get("mesh")
        if isinstance(mesh_index, int):
            skin_index = node.get("skin")
            if not isinstance(skin_index, int) or not (0 <= skin_index < len(skins)):
                raise AuditError("BOSS_RUNTIME_MESH_SKIN_REQUIRED", f"mesh node {node_index} has no valid skin")
            mesh_skin_nodes.setdefault(mesh_index, []).append(skin_index)

    total_vertices = 0
    blended_vertices = 0
    cross_bone_triangles = 0
    triangle_count = 0
    primitive_count = 0
    dominant_histogram: dict[int, int] = {}

    for mesh_index, mesh in enumerate(meshes):
        skin_indices = mesh_skin_nodes.get(mesh_index, [])
        if not skin_indices:
            continue
        if len(set(skin_indices)) != 1:
            raise AuditError("BOSS_RUNTIME_MESH_AMBIGUOUS_SKIN", f"mesh {mesh_index} is instanced with multiple skins")
        skin = skins[skin_indices[0]]
        joints = skin.get("joints", [])
        for primitive in mesh.get("primitives", []):
            primitive_count += 1
            if primitive.get("mode", 4) != 4:
                raise AuditError("BOSS_RUNTIME_MESH_TRIANGLES_REQUIRED", "only TRIANGLES primitives are accepted")
            attrs = primitive.get("attributes", {})
            joints_accessor = attrs.get("JOINTS_0")
            weights_accessor = attrs.get("WEIGHTS_0")
            if not isinstance(joints_accessor, int) or not isinstance(weights_accessor, int):
                raise AuditError("BOSS_RUNTIME_MESH_SKIN_ATTRIBUTES_REQUIRED", "skinned primitive requires JOINTS_0 and WEIGHTS_0")
            joint_values = _accessor_values(gltf, buffers, joints_accessor)
            weight_values = _accessor_values(gltf, buffers, weights_accessor)
            if len(joint_values) != len(weight_values):
                raise AuditError("BOSS_RUNTIME_MESH_SKIN_COUNT_MISMATCH", "JOINTS_0 and WEIGHTS_0 counts differ")
            total_vertices += len(joint_values)
            dominant: list[int] = []
            for vertex_joints, vertex_weights in zip(joint_values, weight_values):
                if len(vertex_joints) != 4 or len(vertex_weights) != 4:
                    raise AuditError("BOSS_RUNTIME_MESH_SKIN_VEC4_REQUIRED", "JOINTS_0/WEIGHTS_0 must be VEC4")
                weighted = [(int(j), float(w)) for j, w in zip(vertex_joints, vertex_weights) if float(w) > epsilon]
                if not weighted:
                    raise AuditError("BOSS_RUNTIME_MESH_ZERO_WEIGHT_VERTEX", "vertex has no positive skin weight")
                weight_sum = sum(w for _, w in weighted)
                if not math.isclose(weight_sum, 1.0, rel_tol=1.0e-4, abs_tol=1.0e-4):
                    raise AuditError("BOSS_RUNTIME_MESH_WEIGHT_SUM", f"skin weight sum {weight_sum} is not 1")
                if any(j < 0 or j >= len(joints) for j, _ in weighted):
                    raise AuditError("BOSS_RUNTIME_MESH_JOINT_RANGE", "JOINTS_0 references outside skin joints")
                if len(weighted) > 1:
                    blended_vertices += 1
                best = max(weighted, key=lambda p: p[1])[0]
                dominant.append(best)
                dominant_histogram[best] = dominant_histogram.get(best, 0) + 1

            indices_accessor = primitive.get("indices")
            if isinstance(indices_accessor, int):
                index_values = _accessor_values(gltf, buffers, indices_accessor)
                indices = [int(v[0]) for v in index_values]
            else:
                indices = list(range(len(dominant)))
            if len(indices) % 3 != 0:
                raise AuditError("BOSS_RUNTIME_MESH_INDEX_COUNT", "triangle index count is not divisible by 3")
            for i in range(0, len(indices), 3):
                tri = indices[i:i + 3]
                if any(v < 0 or v >= len(dominant) for v in tri):
                    raise AuditError("BOSS_RUNTIME_MESH_INDEX_RANGE", "triangle index references outside vertex range")
                triangle_count += 1
                if len({dominant[v] for v in tri}) != 1:
                    cross_bone_triangles += 1

    if primitive_count == 0:
        raise AuditError("BOSS_RUNTIME_MESH_NO_SKINNED_PRIMITIVE", "no skinned mesh primitive was found")

    lossless_rigid_bone = blended_vertices == 0 and cross_bone_triangles == 0
    result = {
        "kind": "riftfrontier:boss_runtime_mesh_audit",
        "schema_version": 1,
        "renderer_contract": "rigid_bone_polymesh",
        "skinned_primitive_count": primitive_count,
        "vertex_count": total_vertices,
        "triangle_count": triangle_count,
        "blended_vertex_count": blended_vertices,
        "cross_bone_triangle_count": cross_bone_triangles,
        "dominant_joint_count": len(dominant_histogram),
        "lossless_rigid_bone_eligible": lossless_rigid_bone,
    }
    if not lossless_rigid_bone:
        reasons = []
        if blended_vertices:
            reasons.append("linear-blend skinning is present")
        if cross_bone_triangles:
            reasons.append("triangles span dominant joints")
        result["decision_code"] = "BOSS_RUNTIME_MESH_RIGID_POLYMESH_LOSSY"
        result["decision"] = "REJECT_RIGID_BONE_POLYMESH"
        result["reasons"] = reasons
    else:
        result["decision_code"] = "BOSS_RUNTIME_MESH_RIGID_POLYMESH_ELIGIBLE"
        result["decision"] = "ELIGIBLE_FOR_FURTHER_RUNTIME_VALIDATION"
        result["reasons"] = []
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("gltf", type=Path)
    parser.add_argument("--require-lossless-rigid-bone", action="store_true")
    args = parser.parse_args()
    try:
        result = audit(args.gltf)
    except AuditError as exc:
        print(json.dumps({"ok": False, "code": exc.code, "message": str(exc)}, sort_keys=True))
        return 2
    print(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2))
    if args.require_lossless_rigid_bone and not result["lossless_rigid_bone_eligible"]:
        return 3
    return 0


if __name__ == "__main__":
    sys.exit(main())
