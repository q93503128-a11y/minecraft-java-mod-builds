#!/usr/bin/env python3
"""Derive a deterministic Region 01 boss rig/animation binding plan from a pinned glTF source.

This is deliberately not a renderer-format converter. It verifies the exact selected
source bytes and structural invariants, then emits only the source skeleton hierarchy,
skin binding, and animation-channel inventory needed by the later production converter.
Materials, textures, images, buffers, and mesh payloads are excluded by design because
the selected Dragon Evolved source is approved only as geometry/rig derivation input,
not as final Riftfrontier art.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

CONTRACT_KIND = "riftfrontier:boss_source_contract"
CONTRACT_SCHEMA_VERSION = 1
OUTPUT_KIND = "riftfrontier:boss_rig_derivation"
OUTPUT_SCHEMA_VERSION = 1
_ALLOWED_ANIMATION_PATHS = {"translation", "rotation", "scale", "weights"}


class DerivationError(ValueError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


@dataclass(frozen=True)
class SourceContract:
    source_id: str
    filename: str
    sha256: str
    license_record: str
    gltf_version: str
    mesh_count: int
    skin_count: int
    joint_count: int
    animation_count: int
    required_animations: tuple[str, ...]


def _require_object(value: Any, code: str, message: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise DerivationError(code, message)
    return value


def _require_list(value: Any, code: str, message: str) -> list[Any]:
    if not isinstance(value, list):
        raise DerivationError(code, message)
    return value


def _require_nonempty_string(value: Any, code: str, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise DerivationError(code, f"{field} must be a non-empty string")
    return value.strip()


def _require_nonnegative_int(value: Any, code: str, field: str) -> int:
    if not isinstance(value, int) or isinstance(value, bool) or value < 0:
        raise DerivationError(code, f"{field} must be a non-negative integer")
    return value


def _read_json(path: Path, code: str) -> dict[str, Any]:
    try:
        raw = path.read_text(encoding="utf-8")
        value = json.loads(raw)
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise DerivationError(code, f"cannot read JSON {path}: {exc}") from exc
    return _require_object(value, code, f"JSON root in {path} must be an object")


def load_contract(path: Path) -> SourceContract:
    root = _read_json(path, "BOSS_SOURCE_CONTRACT_INVALID")
    if root.get("kind") != CONTRACT_KIND:
        raise DerivationError("BOSS_SOURCE_CONTRACT_KIND_INVALID", f"contract kind must be {CONTRACT_KIND!r}")
    if root.get("schema_version") != CONTRACT_SCHEMA_VERSION:
        raise DerivationError("BOSS_SOURCE_CONTRACT_SCHEMA_UNSUPPORTED", f"contract schema_version must be {CONTRACT_SCHEMA_VERSION}")

    source = _require_object(root.get("source"), "BOSS_SOURCE_CONTRACT_INVALID", "source must be an object")
    expect = _require_object(root.get("expect"), "BOSS_SOURCE_CONTRACT_INVALID", "expect must be an object")
    sha256 = _require_nonempty_string(source.get("sha256"), "BOSS_SOURCE_CONTRACT_INVALID", "source.sha256").lower()
    if len(sha256) != 64 or any(ch not in "0123456789abcdef" for ch in sha256):
        raise DerivationError("BOSS_SOURCE_CONTRACT_INVALID", "source.sha256 must be 64 lowercase hex characters")

    required = _require_list(expect.get("required_animations"), "BOSS_SOURCE_CONTRACT_INVALID", "expect.required_animations must be an array")
    required_names = [
        _require_nonempty_string(value, "BOSS_SOURCE_CONTRACT_INVALID", f"expect.required_animations[{index}]")
        for index, value in enumerate(required)
    ]
    if len(required_names) != len(set(required_names)):
        raise DerivationError("BOSS_SOURCE_CONTRACT_INVALID", "required animation names must be unique")

    return SourceContract(
        source_id=_require_nonempty_string(source.get("id"), "BOSS_SOURCE_CONTRACT_INVALID", "source.id"),
        filename=_require_nonempty_string(source.get("filename"), "BOSS_SOURCE_CONTRACT_INVALID", "source.filename"),
        sha256=sha256,
        license_record=_require_nonempty_string(source.get("license_record"), "BOSS_SOURCE_CONTRACT_INVALID", "source.license_record"),
        gltf_version=_require_nonempty_string(expect.get("gltf_version"), "BOSS_SOURCE_CONTRACT_INVALID", "expect.gltf_version"),
        mesh_count=_require_nonnegative_int(expect.get("mesh_count"), "BOSS_SOURCE_CONTRACT_INVALID", "expect.mesh_count"),
        skin_count=_require_nonnegative_int(expect.get("skin_count"), "BOSS_SOURCE_CONTRACT_INVALID", "expect.skin_count"),
        joint_count=_require_nonnegative_int(expect.get("joint_count"), "BOSS_SOURCE_CONTRACT_INVALID", "expect.joint_count"),
        animation_count=_require_nonnegative_int(expect.get("animation_count"), "BOSS_SOURCE_CONTRACT_INVALID", "expect.animation_count"),
        required_animations=tuple(required_names),
    )


def _load_source(path: Path, contract: SourceContract) -> tuple[dict[str, Any], str]:
    try:
        raw = path.read_bytes()
    except OSError as exc:
        raise DerivationError("BOSS_SOURCE_READ_FAILED", f"cannot read source {path}: {exc}") from exc
    actual_sha = hashlib.sha256(raw).hexdigest()
    if actual_sha != contract.sha256:
        raise DerivationError("BOSS_SOURCE_SHA256_MISMATCH", f"source SHA-256 {actual_sha} does not match pinned {contract.sha256}")
    if path.name != contract.filename:
        raise DerivationError("BOSS_SOURCE_FILENAME_MISMATCH", f"source filename {path.name!r} does not match pinned {contract.filename!r}")
    if path.suffix.lower() != ".gltf":
        raise DerivationError("BOSS_SOURCE_FORMAT_UNSUPPORTED", "selected Region 01 source contract requires the original .gltf JSON file")
    try:
        document = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise DerivationError("BOSS_SOURCE_GLTF_INVALID", f"invalid glTF JSON: {exc}") from exc
    return _require_object(document, "BOSS_SOURCE_GLTF_INVALID", "glTF root must be a JSON object"), actual_sha


def _array(document: dict[str, Any], key: str) -> list[Any]:
    value = document.get(key, [])
    if value is None:
        return []
    return _require_list(value, "BOSS_SOURCE_GLTF_INVALID", f"{key} must be an array")


def _check_index(index: Any, size: int, code: str, label: str) -> int:
    if not isinstance(index, int) or isinstance(index, bool) or index < 0 or index >= size:
        raise DerivationError(code, f"{label} index {index!r} is outside [0, {size})")
    return index


def _finite_number(value: Any, label: str) -> float:
    if not isinstance(value, (int, float)) or isinstance(value, bool) or not math.isfinite(float(value)):
        raise DerivationError("BOSS_SOURCE_GLTF_INVALID", f"{label} must be finite")
    return float(value)


def _optional_vector(node: dict[str, Any], key: str, length: int, node_index: int) -> list[float] | None:
    if key not in node:
        return None
    value = node[key]
    if not isinstance(value, list) or len(value) != length:
        raise DerivationError("BOSS_SOURCE_GLTF_INVALID", f"nodes[{node_index}].{key} must have {length} numbers")
    return [_finite_number(component, f"nodes[{node_index}].{key}") for component in value]


def _accessor_duration(document: dict[str, Any], accessor_index: Any) -> float | None:
    accessors = _array(document, "accessors")
    index = _check_index(accessor_index, len(accessors), "BOSS_SOURCE_REFERENCE_INVALID", "animation sampler input accessor")
    accessor = _require_object(accessors[index], "BOSS_SOURCE_GLTF_INVALID", f"accessors[{index}] must be an object")
    minimum = accessor.get("min")
    maximum = accessor.get("max")
    if not isinstance(minimum, list) or not minimum or not isinstance(maximum, list) or not maximum:
        return None
    lo = _finite_number(minimum[0], f"accessors[{index}].min[0]")
    hi = _finite_number(maximum[0], f"accessors[{index}].max[0]")
    if hi < lo:
        raise DerivationError("BOSS_SOURCE_GLTF_INVALID", f"accessors[{index}] animation time max is below min")
    return hi - lo


def validate_structure(document: dict[str, Any], contract: SourceContract) -> None:
    asset = _require_object(document.get("asset"), "BOSS_SOURCE_GLTF_INVALID", "asset must be an object")
    if str(asset.get("version", "")) != contract.gltf_version:
        raise DerivationError("BOSS_SOURCE_GLTF_VERSION_MISMATCH", f"asset.version {asset.get('version')!r} != pinned {contract.gltf_version!r}")
    meshes = _array(document, "meshes")
    skins = _array(document, "skins")
    animations = _array(document, "animations")
    nodes = _array(document, "nodes")
    if len(meshes) != contract.mesh_count:
        raise DerivationError("BOSS_SOURCE_MESH_COUNT_MISMATCH", f"mesh count {len(meshes)} != pinned {contract.mesh_count}")
    if len(skins) != contract.skin_count:
        raise DerivationError("BOSS_SOURCE_SKIN_COUNT_MISMATCH", f"skin count {len(skins)} != pinned {contract.skin_count}")
    total_joints = 0
    for skin_index, raw_skin in enumerate(skins):
        skin = _require_object(raw_skin, "BOSS_SOURCE_GLTF_INVALID", f"skins[{skin_index}] must be an object")
        joints = _require_list(skin.get("joints"), "BOSS_SOURCE_GLTF_INVALID", f"skins[{skin_index}].joints must be an array")
        total_joints += len(joints)
        for joint_index in joints:
            _check_index(joint_index, len(nodes), "BOSS_SOURCE_REFERENCE_INVALID", f"skins[{skin_index}] joint")
    if total_joints != contract.joint_count:
        raise DerivationError("BOSS_SOURCE_JOINT_COUNT_MISMATCH", f"joint count {total_joints} != pinned {contract.joint_count}")
    if len(animations) != contract.animation_count:
        raise DerivationError("BOSS_SOURCE_ANIMATION_COUNT_MISMATCH", f"animation count {len(animations)} != pinned {contract.animation_count}")
    names = [
        _require_nonempty_string(
            _require_object(animation, "BOSS_SOURCE_GLTF_INVALID", f"animations[{index}] must be an object").get("name"),
            "BOSS_SOURCE_ANIMATION_NAME_INVALID",
            f"animations[{index}].name",
        )
        for index, animation in enumerate(animations)
    ]
    if len(names) != len(set(names)):
        raise DerivationError("BOSS_SOURCE_ANIMATION_NAME_INVALID", "animation names must be unique")
    for required in contract.required_animations:
        if required not in names:
            raise DerivationError("BOSS_SOURCE_REQUIRED_ANIMATION_MISSING", f"required source animation {required!r} is missing")


def _derive_nodes(document: dict[str, Any]) -> list[dict[str, Any]]:
    nodes = _array(document, "nodes")
    result = []
    for index, raw_node in enumerate(nodes):
        node = _require_object(raw_node, "BOSS_SOURCE_GLTF_INVALID", f"nodes[{index}] must be an object")
        children = _require_list(node.get("children", []), "BOSS_SOURCE_GLTF_INVALID", f"nodes[{index}].children must be an array")
        entry: dict[str, Any] = {
            "index": index,
            "name": node.get("name") if isinstance(node.get("name"), str) else None,
            "children": [_check_index(child, len(nodes), "BOSS_SOURCE_REFERENCE_INVALID", f"nodes[{index}] child") for child in children],
        }
        if "mesh" in node:
            entry["mesh"] = _check_index(node["mesh"], len(_array(document, "meshes")), "BOSS_SOURCE_REFERENCE_INVALID", f"nodes[{index}].mesh")
        if "skin" in node:
            entry["skin"] = _check_index(node["skin"], len(_array(document, "skins")), "BOSS_SOURCE_REFERENCE_INVALID", f"nodes[{index}].skin")
        for key, length in (("translation", 3), ("rotation", 4), ("scale", 3), ("matrix", 16)):
            vector = _optional_vector(node, key, length, index)
            if vector is not None:
                entry[key] = vector
        result.append(entry)
    return result


def _derive_skins(document: dict[str, Any]) -> list[dict[str, Any]]:
    nodes = _array(document, "nodes")
    result = []
    for index, raw_skin in enumerate(_array(document, "skins")):
        skin = _require_object(raw_skin, "BOSS_SOURCE_GLTF_INVALID", f"skins[{index}] must be an object")
        joints = _require_list(skin.get("joints"), "BOSS_SOURCE_GLTF_INVALID", f"skins[{index}].joints must be an array")
        entry: dict[str, Any] = {
            "index": index,
            "name": skin.get("name") if isinstance(skin.get("name"), str) else None,
            "joints": [_check_index(joint, len(nodes), "BOSS_SOURCE_REFERENCE_INVALID", f"skins[{index}] joint") for joint in joints],
        }
        if "skeleton" in skin:
            entry["skeleton"] = _check_index(skin["skeleton"], len(nodes), "BOSS_SOURCE_REFERENCE_INVALID", f"skins[{index}].skeleton")
        result.append(entry)
    return result


def _derive_animations(document: dict[str, Any]) -> list[dict[str, Any]]:
    nodes = _array(document, "nodes")
    accessors = _array(document, "accessors")
    result = []
    for animation_index, raw_animation in enumerate(_array(document, "animations")):
        animation = _require_object(raw_animation, "BOSS_SOURCE_GLTF_INVALID", f"animations[{animation_index}] must be an object")
        samplers = _require_list(animation.get("samplers", []), "BOSS_SOURCE_GLTF_INVALID", f"animations[{animation_index}].samplers must be an array")
        channels = _require_list(animation.get("channels", []), "BOSS_SOURCE_GLTF_INVALID", f"animations[{animation_index}].channels must be an array")
        derived_channels = []
        durations = []
        for channel_index, raw_channel in enumerate(channels):
            channel = _require_object(raw_channel, "BOSS_SOURCE_GLTF_INVALID", f"animations[{animation_index}].channels[{channel_index}] must be an object")
            sampler_index = _check_index(channel.get("sampler"), len(samplers), "BOSS_SOURCE_REFERENCE_INVALID", f"animations[{animation_index}] channel sampler")
            sampler = _require_object(samplers[sampler_index], "BOSS_SOURCE_GLTF_INVALID", f"animations[{animation_index}].samplers[{sampler_index}] must be an object")
            input_accessor = _check_index(sampler.get("input"), len(accessors), "BOSS_SOURCE_REFERENCE_INVALID", f"animations[{animation_index}] sampler input")
            output_accessor = _check_index(sampler.get("output"), len(accessors), "BOSS_SOURCE_REFERENCE_INVALID", f"animations[{animation_index}] sampler output")
            target = _require_object(channel.get("target"), "BOSS_SOURCE_GLTF_INVALID", f"animations[{animation_index}].channels[{channel_index}].target must be an object")
            target_node = _check_index(target.get("node"), len(nodes), "BOSS_SOURCE_REFERENCE_INVALID", f"animations[{animation_index}] target node")
            path = target.get("path")
            if path not in _ALLOWED_ANIMATION_PATHS:
                raise DerivationError("BOSS_SOURCE_ANIMATION_PATH_UNSUPPORTED", f"unsupported animation target path {path!r}")
            interpolation = sampler.get("interpolation", "LINEAR")
            if not isinstance(interpolation, str) or not interpolation:
                raise DerivationError("BOSS_SOURCE_GLTF_INVALID", "animation sampler interpolation must be a string")
            duration = _accessor_duration(document, input_accessor)
            if duration is not None:
                durations.append(duration)
            derived_channels.append({
                "target_node": target_node,
                "path": path,
                "sampler": {"input_accessor": input_accessor, "output_accessor": output_accessor, "interpolation": interpolation},
            })
        result.append({
            "index": animation_index,
            "name": animation["name"],
            "duration_seconds": max(durations) if durations else None,
            "channels": derived_channels,
        })
    return result


def derive(source_path: Path, contract_path: Path) -> dict[str, Any]:
    contract = load_contract(contract_path)
    document, actual_sha = _load_source(source_path, contract)
    validate_structure(document, contract)
    return {
        "kind": OUTPUT_KIND,
        "schema_version": OUTPUT_SCHEMA_VERSION,
        "source": {"id": contract.source_id, "filename": contract.filename, "sha256": actual_sha, "license_record": contract.license_record},
        "scope": {
            "approved": ["geometry_rig_derivation", "source_animation_reference"],
            "excluded": ["materials", "textures", "images", "mesh_payloads", "final_art_approval", "hit_timing"],
        },
        "rig": {"nodes": _derive_nodes(document), "skins": _derive_skins(document)},
        "animations": _derive_animations(document),
    }


def canonical_bytes(payload: dict[str, Any]) -> bytes:
    return (json.dumps(payload, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")


def write_output(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(canonical_bytes(payload))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Verify the selected Region 01 boss source and emit a deterministic rig binding plan.")
    parser.add_argument("source", type=Path, help="exact selected Dragon_Evolved.gltf source")
    parser.add_argument("--contract", required=True, type=Path, help="pinned source contract JSON")
    parser.add_argument("--output", type=Path, help="write deterministic derivation JSON")
    parser.add_argument("--verify-only", action="store_true", help="validate source without emitting JSON")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    if args.verify_only and args.output is not None:
        print("ERROR BOSS_SOURCE_ARGUMENT_CONFLICT: --verify-only cannot be combined with --output", file=sys.stderr)
        return 2
    try:
        payload = derive(args.source, args.contract)
        if not args.verify_only:
            if args.output is None:
                sys.stdout.buffer.write(canonical_bytes(payload))
            else:
                write_output(args.output, payload)
    except DerivationError as exc:
        print(f"ERROR {exc.code}: {exc.message}", file=sys.stderr)
        return 1
    except OSError as exc:
        print(f"ERROR BOSS_SOURCE_WRITE_FAILED: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
