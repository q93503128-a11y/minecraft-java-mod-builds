#!/usr/bin/env python3
"""Create a deterministic, art-neutral glTF derivation for the Region 01 boss.

The selected Dragon Evolved file is approved as geometry/rig derivation input, not as
final Riftfrontier art. This tool therefore verifies the pinned source contract via the
existing derivation gate, then rebuilds a minimal self-contained glTF containing only
geometry, skinning, node hierarchy and animation data. Material bindings, materials,
textures, images and texture samplers are deliberately excluded.
"""

from __future__ import annotations

import argparse
import base64
import copy
import hashlib
import json
import sys
from pathlib import Path
from typing import Any
from urllib.parse import unquote_to_bytes

try:
    from derive_region01_boss_source import DerivationError, _load_source, load_contract, validate_structure
except ImportError:  # pragma: no cover - supports package-style test loading
    from tools.derive_region01_boss_source import DerivationError, _load_source, load_contract, validate_structure

OUTPUT_KIND = "riftfrontier:boss_geometry_derivation"
OUTPUT_SCHEMA_VERSION = 1
_ALLOWED_ATTRIBUTES = {
    "POSITION",
    "NORMAL",
    "TANGENT",
    "TEXCOORD_0",
    "TEXCOORD_1",
    "JOINTS_0",
    "WEIGHTS_0",
    "COLOR_0",
}


class GeometryConversionError(ValueError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


def _object(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise GeometryConversionError("BOSS_GEOMETRY_GLTF_INVALID", f"{label} must be an object")
    return value


def _array(value: Any, label: str) -> list[Any]:
    if not isinstance(value, list):
        raise GeometryConversionError("BOSS_GEOMETRY_GLTF_INVALID", f"{label} must be an array")
    return value


def _index(value: Any, size: int, label: str) -> int:
    if not isinstance(value, int) or isinstance(value, bool) or value < 0 or value >= size:
        raise GeometryConversionError("BOSS_GEOMETRY_REFERENCE_INVALID", f"{label} index {value!r} outside [0, {size})")
    return value


def _canonical_json_bytes(value: Any) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def _decode_data_uri(uri: str, label: str) -> bytes:
    if not uri.startswith("data:"):
        raise GeometryConversionError("BOSS_GEOMETRY_BUFFER_URI_INVALID", f"{label} is not a data URI")
    try:
        header, payload = uri.split(",", 1)
    except ValueError as exc:
        raise GeometryConversionError("BOSS_GEOMETRY_BUFFER_URI_INVALID", f"{label} has no comma separator") from exc
    if ";base64" in header:
        try:
            return base64.b64decode(payload, validate=True)
        except ValueError as exc:
            raise GeometryConversionError("BOSS_GEOMETRY_BUFFER_URI_INVALID", f"{label} has invalid base64 payload") from exc
    return unquote_to_bytes(payload)


def _read_buffer(source_path: Path, raw_buffer: dict[str, Any], buffer_index: int) -> bytes:
    uri = raw_buffer.get("uri")
    if not isinstance(uri, str) or not uri:
        raise GeometryConversionError(
            "BOSS_GEOMETRY_BUFFER_URI_INVALID",
            f"buffers[{buffer_index}].uri must be an embedded data URI or a relative local file",
        )
    if uri.startswith("data:"):
        raw = _decode_data_uri(uri, f"buffers[{buffer_index}].uri")
    else:
        if "://" in uri or uri.startswith(("/", "\\")):
            raise GeometryConversionError("BOSS_GEOMETRY_BUFFER_URI_INVALID", f"buffers[{buffer_index}] remote/absolute URI is forbidden")
        base = source_path.parent.resolve()
        candidate = (base / uri).resolve()
        try:
            candidate.relative_to(base)
        except ValueError as exc:
            raise GeometryConversionError("BOSS_GEOMETRY_BUFFER_PATH_ESCAPE", f"buffers[{buffer_index}] escapes the source directory") from exc
        try:
            raw = candidate.read_bytes()
        except OSError as exc:
            raise GeometryConversionError("BOSS_GEOMETRY_BUFFER_READ_FAILED", f"cannot read buffers[{buffer_index}] from {candidate}: {exc}") from exc
    declared = raw_buffer.get("byteLength")
    if not isinstance(declared, int) or isinstance(declared, bool) or declared < 0:
        raise GeometryConversionError("BOSS_GEOMETRY_GLTF_INVALID", f"buffers[{buffer_index}].byteLength must be a non-negative integer")
    if len(raw) < declared:
        raise GeometryConversionError("BOSS_GEOMETRY_BUFFER_TRUNCATED", f"buffers[{buffer_index}] has {len(raw)} bytes but declares {declared}")
    return raw[:declared]


def _collect_accessor_refs(document: dict[str, Any]) -> set[int]:
    accessors = _array(document.get("accessors", []), "accessors")
    refs: set[int] = set()
    for mesh_index, raw_mesh in enumerate(_array(document.get("meshes", []), "meshes")):
        mesh = _object(raw_mesh, f"meshes[{mesh_index}]")
        for primitive_index, raw_primitive in enumerate(_array(mesh.get("primitives", []), f"meshes[{mesh_index}].primitives")):
            primitive = _object(raw_primitive, f"meshes[{mesh_index}].primitives[{primitive_index}]")
            if primitive.get("targets"):
                raise GeometryConversionError("BOSS_GEOMETRY_MORPH_TARGET_UNSUPPORTED", "morph targets are not allowed in the selected-source conversion")
            if primitive.get("extensions"):
                raise GeometryConversionError("BOSS_GEOMETRY_EXTENSION_UNSUPPORTED", "mesh primitive extensions require an explicit converter update")
            attributes = _object(primitive.get("attributes"), f"meshes[{mesh_index}].primitives[{primitive_index}].attributes")
            unknown = sorted(set(attributes) - _ALLOWED_ATTRIBUTES)
            if unknown:
                raise GeometryConversionError("BOSS_GEOMETRY_ATTRIBUTE_UNSUPPORTED", f"unsupported mesh attributes: {unknown}")
            for semantic, accessor_index in attributes.items():
                refs.add(_index(accessor_index, len(accessors), f"mesh attribute {semantic}"))
            if "indices" in primitive:
                refs.add(_index(primitive["indices"], len(accessors), "mesh indices"))
    for skin_index, raw_skin in enumerate(_array(document.get("skins", []), "skins")):
        skin = _object(raw_skin, f"skins[{skin_index}]")
        if "inverseBindMatrices" in skin:
            refs.add(_index(skin["inverseBindMatrices"], len(accessors), f"skins[{skin_index}].inverseBindMatrices"))
    for animation_index, raw_animation in enumerate(_array(document.get("animations", []), "animations")):
        animation = _object(raw_animation, f"animations[{animation_index}]")
        for sampler_index, raw_sampler in enumerate(_array(animation.get("samplers", []), f"animations[{animation_index}].samplers")):
            sampler = _object(raw_sampler, f"animations[{animation_index}].samplers[{sampler_index}]")
            refs.add(_index(sampler.get("input"), len(accessors), "animation sampler input"))
            refs.add(_index(sampler.get("output"), len(accessors), "animation sampler output"))
    return refs


def _copy_accessor(raw_accessor: dict[str, Any], old_to_new_view: dict[int, int], view_count: int, label: str) -> dict[str, Any]:
    allowed = {"bufferView", "byteOffset", "componentType", "normalized", "count", "type", "max", "min", "sparse", "name"}
    unknown = set(raw_accessor) - allowed
    if unknown:
        raise GeometryConversionError("BOSS_GEOMETRY_ACCESSOR_FIELD_UNSUPPORTED", f"{label} has unsupported fields: {sorted(unknown)}")
    copied = copy.deepcopy(raw_accessor)
    if "bufferView" in copied:
        old = _index(copied["bufferView"], view_count, f"{label}.bufferView")
        copied["bufferView"] = old_to_new_view[old]
    if "sparse" in copied:
        sparse = _object(copied["sparse"], f"{label}.sparse")
        indices = _object(sparse.get("indices"), f"{label}.sparse.indices")
        values = _object(sparse.get("values"), f"{label}.sparse.values")
        for entry, entry_label in ((indices, "indices"), (values, "values")):
            old = _index(entry.get("bufferView"), view_count, f"{label}.sparse.{entry_label}.bufferView")
            entry["bufferView"] = old_to_new_view[old]
    return copied


def sanitize_document(document: dict[str, Any], source_path: Path) -> tuple[dict[str, Any], dict[str, Any]]:
    if document.get("extensionsRequired"):
        raise GeometryConversionError("BOSS_GEOMETRY_EXTENSION_UNSUPPORTED", "required glTF extensions need an explicit production converter")

    accessors = _array(document.get("accessors", []), "accessors")
    buffer_views = _array(document.get("bufferViews", []), "bufferViews")
    buffers = _array(document.get("buffers", []), "buffers")
    accessor_refs = _collect_accessor_refs(document)

    needed_views: set[int] = set()
    for accessor_index in accessor_refs:
        accessor = _object(accessors[accessor_index], f"accessors[{accessor_index}]")
        if "bufferView" in accessor:
            needed_views.add(_index(accessor["bufferView"], len(buffer_views), f"accessors[{accessor_index}].bufferView"))
        if "sparse" in accessor:
            sparse = _object(accessor["sparse"], f"accessors[{accessor_index}].sparse")
            for field in ("indices", "values"):
                entry = _object(sparse.get(field), f"accessors[{accessor_index}].sparse.{field}")
                needed_views.add(_index(entry.get("bufferView"), len(buffer_views), f"accessors[{accessor_index}].sparse.{field}.bufferView"))

    decoded_buffers = [_read_buffer(source_path, _object(raw, f"buffers[{i}]"), i) for i, raw in enumerate(buffers)]
    view_map: dict[int, int] = {}
    rebuilt_views: list[dict[str, Any]] = []
    payload = bytearray()
    for old_view_index in sorted(needed_views):
        raw_view = _object(buffer_views[old_view_index], f"bufferViews[{old_view_index}]")
        allowed = {"buffer", "byteOffset", "byteLength", "byteStride", "target", "name"}
        unknown = set(raw_view) - allowed
        if unknown:
            raise GeometryConversionError("BOSS_GEOMETRY_BUFFERVIEW_FIELD_UNSUPPORTED", f"bufferViews[{old_view_index}] has unsupported fields: {sorted(unknown)}")
        source_buffer = _index(raw_view.get("buffer"), len(decoded_buffers), f"bufferViews[{old_view_index}].buffer")
        offset = raw_view.get("byteOffset", 0)
        length = raw_view.get("byteLength")
        if not isinstance(offset, int) or isinstance(offset, bool) or offset < 0 or not isinstance(length, int) or isinstance(length, bool) or length < 0:
            raise GeometryConversionError("BOSS_GEOMETRY_GLTF_INVALID", f"bufferViews[{old_view_index}] offset/length must be non-negative integers")
        end = offset + length
        raw = decoded_buffers[source_buffer]
        if end > len(raw):
            raise GeometryConversionError("BOSS_GEOMETRY_BUFFERVIEW_RANGE_INVALID", f"bufferViews[{old_view_index}] exceeds buffers[{source_buffer}]")
        while len(payload) % 4:
            payload.append(0)
        new_offset = len(payload)
        payload.extend(raw[offset:end])
        copied = {key: copy.deepcopy(value) for key, value in raw_view.items() if key not in {"buffer", "byteOffset", "byteLength"}}
        copied["buffer"] = 0
        copied["byteOffset"] = new_offset
        copied["byteLength"] = length
        view_map[old_view_index] = len(rebuilt_views)
        rebuilt_views.append(copied)

    accessor_map = {old: new for new, old in enumerate(sorted(accessor_refs))}
    rebuilt_accessors = [
        _copy_accessor(_object(accessors[old], f"accessors[{old}]"), view_map, len(buffer_views), f"accessors[{old}]")
        for old in sorted(accessor_refs)
    ]

    rebuilt_meshes: list[dict[str, Any]] = []
    for mesh_index, raw_mesh in enumerate(_array(document.get("meshes", []), "meshes")):
        mesh = _object(raw_mesh, f"meshes[{mesh_index}]")
        out_mesh: dict[str, Any] = {}
        if isinstance(mesh.get("name"), str):
            out_mesh["name"] = mesh["name"]
        out_primitives = []
        for primitive_index, raw_primitive in enumerate(_array(mesh.get("primitives", []), f"meshes[{mesh_index}].primitives")):
            primitive = _object(raw_primitive, f"meshes[{mesh_index}].primitives[{primitive_index}]")
            attributes = _object(primitive.get("attributes"), f"meshes[{mesh_index}].primitives[{primitive_index}].attributes")
            out_primitive: dict[str, Any] = {
                "attributes": {semantic: accessor_map[index] for semantic, index in sorted(attributes.items())}
            }
            if "indices" in primitive:
                out_primitive["indices"] = accessor_map[primitive["indices"]]
            if "mode" in primitive:
                out_primitive["mode"] = primitive["mode"]
            out_primitives.append(out_primitive)
        out_mesh["primitives"] = out_primitives
        rebuilt_meshes.append(out_mesh)

    nodes = copy.deepcopy(_array(document.get("nodes", []), "nodes"))
    for index, raw_node in enumerate(nodes):
        node = _object(raw_node, f"nodes[{index}]")
        allowed = {"name", "children", "mesh", "skin", "matrix", "translation", "rotation", "scale", "weights", "camera"}
        unknown = set(node) - allowed
        if unknown:
            raise GeometryConversionError("BOSS_GEOMETRY_NODE_FIELD_UNSUPPORTED", f"nodes[{index}] has unsupported fields: {sorted(unknown)}")
        if "camera" in node:
            raise GeometryConversionError("BOSS_GEOMETRY_CAMERA_UNSUPPORTED", "camera nodes are not part of production boss geometry")

    skins = copy.deepcopy(_array(document.get("skins", []), "skins"))
    for skin_index, skin in enumerate(skins):
        obj = _object(skin, f"skins[{skin_index}]")
        if "inverseBindMatrices" in obj:
            obj["inverseBindMatrices"] = accessor_map[obj["inverseBindMatrices"]]

    animations = copy.deepcopy(_array(document.get("animations", []), "animations"))
    for animation_index, animation in enumerate(animations):
        obj = _object(animation, f"animations[{animation_index}]")
        for sampler_index, sampler in enumerate(_array(obj.get("samplers", []), f"animations[{animation_index}].samplers")):
            s = _object(sampler, f"animations[{animation_index}].samplers[{sampler_index}]")
            s["input"] = accessor_map[s["input"]]
            s["output"] = accessor_map[s["output"]]

    encoded = base64.b64encode(bytes(payload)).decode("ascii")
    result: dict[str, Any] = {
        "asset": {"version": "2.0", "generator": "Riftfrontier deterministic boss geometry sanitizer v1"},
        "scene": document.get("scene", 0),
        "scenes": copy.deepcopy(_array(document.get("scenes", []), "scenes")),
        "nodes": nodes,
        "meshes": rebuilt_meshes,
        "skins": skins,
        "animations": animations,
        "accessors": rebuilt_accessors,
        "bufferViews": rebuilt_views,
        "buffers": [{"byteLength": len(payload), "uri": "data:application/octet-stream;base64," + encoded}],
    }
    for key in ["scenes", "nodes", "meshes", "skins", "animations", "accessors", "bufferViews"]:
        if not result[key]:
            result.pop(key)

    stats = {
        "retained_accessor_count": len(rebuilt_accessors),
        "retained_buffer_view_count": len(rebuilt_views),
        "embedded_buffer_bytes": len(payload),
        "mesh_count": len(rebuilt_meshes),
        "skin_count": len(skins),
        "animation_count": len(animations),
    }
    return result, stats


def build_provenance(source_id: str, source_sha256: str, output_bytes: bytes, stats: dict[str, Any]) -> dict[str, Any]:
    return {
        "kind": OUTPUT_KIND,
        "schema_version": OUTPUT_SCHEMA_VERSION,
        "source_id": source_id,
        "source_sha256": source_sha256,
        "output_sha256": hashlib.sha256(output_bytes).hexdigest(),
        "art_policy": {
            "retained": ["geometry", "indices", "vertex attributes", "skin", "joint hierarchy", "animation channels"],
            "excluded": ["primitive material bindings", "materials", "textures", "images", "texture samplers"],
            "final_material_approved": False,
        },
        "stats": stats,
    }


def convert(contract_path: Path, source_path: Path, output_path: Path, provenance_path: Path) -> dict[str, Any]:
    contract = load_contract(contract_path)
    document, actual_sha = _load_source(source_path, contract)
    validate_structure(document, contract)
    sanitized, stats = sanitize_document(document, source_path)
    output_bytes = _canonical_json_bytes(sanitized)
    provenance = build_provenance(contract.source_id, actual_sha, output_bytes, stats)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    provenance_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(output_bytes)
    provenance_path.write_bytes(_canonical_json_bytes(provenance))
    return provenance


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contract", required=True, type=Path)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--provenance", required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        provenance = convert(args.contract, args.source, args.output, args.provenance)
    except (DerivationError, GeometryConversionError) as exc:
        code = getattr(exc, "code", "BOSS_GEOMETRY_CONVERSION_FAILED")
        print(f"{code}: {exc}", file=sys.stderr)
        return 2
    print(json.dumps(provenance, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
