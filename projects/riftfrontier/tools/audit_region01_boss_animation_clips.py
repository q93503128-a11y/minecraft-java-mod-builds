#!/usr/bin/env python3
"""Deterministically audit the accepted Region 01 boss glTF animation inventory.

The audit is art-neutral: it validates the accepted sanitized derivation SHA and
records source animation timing/channel metadata without inventing logical combat
bindings. It fails closed on unsupported accessor/layout features.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import math
import struct
from collections import Counter
from pathlib import Path
from typing import Any

FLOAT = 5126
ALLOWED_INTERPOLATION = {"STEP", "LINEAR", "CUBICSPLINE"}
ALLOWED_PATHS = {"translation", "rotation", "scale"}


class AuditError(ValueError):
    pass


def _load_json(path: Path) -> tuple[dict[str, Any], bytes]:
    raw = path.read_bytes()
    if path.suffix.lower() != ".gltf":
        raise AuditError("accepted Region 01 derivation must be .gltf")
    try:
        doc = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AuditError(f"invalid glTF JSON: {exc}") from exc
    if not isinstance(doc, dict) or doc.get("asset", {}).get("version") != "2.0":
        raise AuditError("asset.version must be glTF 2.0")
    return doc, raw


def _array(doc: dict[str, Any], key: str) -> list[Any]:
    value = doc.get(key, [])
    if not isinstance(value, list):
        raise AuditError(f"{key} must be an array")
    return value


def _embedded_buffer(doc: dict[str, Any], index: int) -> bytes:
    buffers = _array(doc, "buffers")
    if index < 0 or index >= len(buffers) or not isinstance(buffers[index], dict):
        raise AuditError(f"buffer index out of range: {index}")
    uri = buffers[index].get("uri")
    if not isinstance(uri, str) or not uri.startswith("data:application/octet-stream;base64,"):
        raise AuditError("animation audit requires embedded base64 application/octet-stream buffers")
    try:
        return base64.b64decode(uri.split(",", 1)[1], validate=True)
    except Exception as exc:
        raise AuditError("invalid embedded buffer base64") from exc


def _read_scalar_float_accessor(doc: dict[str, Any], accessor_index: Any) -> list[float]:
    if not isinstance(accessor_index, int):
        raise AuditError("animation sampler input accessor must be an integer")
    accessors = _array(doc, "accessors")
    if accessor_index < 0 or accessor_index >= len(accessors) or not isinstance(accessors[accessor_index], dict):
        raise AuditError(f"accessor index out of range: {accessor_index}")
    accessor = accessors[accessor_index]
    if accessor.get("componentType") != FLOAT or accessor.get("type") != "SCALAR":
        raise AuditError("animation input accessor must be FLOAT SCALAR")
    if "sparse" in accessor:
        raise AuditError("sparse animation input accessors are not supported")
    count = accessor.get("count")
    view_index = accessor.get("bufferView")
    if not isinstance(count, int) or count <= 0 or not isinstance(view_index, int):
        raise AuditError("animation input accessor requires positive count and bufferView")
    views = _array(doc, "bufferViews")
    if view_index < 0 or view_index >= len(views) or not isinstance(views[view_index], dict):
        raise AuditError(f"bufferView index out of range: {view_index}")
    view = views[view_index]
    stride = view.get("byteStride", 4)
    if stride != 4:
        raise AuditError("animation input accessor byteStride must be 4")
    buffer_index = view.get("buffer", 0)
    if not isinstance(buffer_index, int):
        raise AuditError("bufferView.buffer must be an integer")
    payload = _embedded_buffer(doc, buffer_index)
    view_offset = view.get("byteOffset", 0)
    accessor_offset = accessor.get("byteOffset", 0)
    view_length = view.get("byteLength")
    if not all(isinstance(v, int) for v in (view_offset, accessor_offset, view_length)):
        raise AuditError("animation input bufferView offsets/length must be integers")
    base = view_offset + accessor_offset
    if base < 0 or view_length < 0:
        raise AuditError("invalid animation input byte offset/length")
    end = base + count * 4
    view_end = view_offset + view_length
    if end > len(payload) or end > view_end:
        raise AuditError("animation input accessor exceeds its bufferView")
    values = list(struct.unpack_from(f"<{count}f", payload, base))
    if any(not math.isfinite(v) or v < 0.0 for v in values):
        raise AuditError("animation input times must be finite and non-negative")
    if any(values[i] > values[i + 1] for i in range(len(values) - 1)):
        raise AuditError("animation input times must be non-decreasing")
    return values


def audit(gltf_path: Path, acceptance_path: Path) -> dict[str, Any]:
    acceptance = json.loads(acceptance_path.read_text(encoding="utf-8"))
    expected_sha = acceptance.get("output", {}).get("geometry_sha256")
    expected_names = acceptance.get("expect", {}).get("animations")
    if not isinstance(expected_sha, str) or not isinstance(expected_names, list) or not all(isinstance(x, str) for x in expected_names):
        raise AuditError("acceptance receipt is missing geometry SHA or animation inventory")

    doc, raw = _load_json(gltf_path)
    actual_sha = hashlib.sha256(raw).hexdigest()
    if actual_sha != expected_sha:
        raise AuditError(f"accepted derivation SHA mismatch: expected {expected_sha}, got {actual_sha}")

    nodes = _array(doc, "nodes")
    animations = _array(doc, "animations")
    names = [a.get("name") if isinstance(a, dict) else None for a in animations]
    if names != expected_names:
        raise AuditError(f"animation inventory/order mismatch: expected {expected_names}, got {names}")
    if len(set(names)) != len(names):
        raise AuditError("animation names must be unique")

    clips: list[dict[str, Any]] = []
    for animation in animations:
        if not isinstance(animation, dict):
            raise AuditError("animation entry must be an object")
        samplers = animation.get("samplers")
        channels = animation.get("channels")
        if not isinstance(samplers, list) or not samplers or not isinstance(channels, list) or not channels:
            raise AuditError(f"animation {animation['name']} requires non-empty samplers/channels")

        sampler_times: list[list[float]] = []
        interpolation = []
        for sampler in samplers:
            if not isinstance(sampler, dict):
                raise AuditError("animation sampler must be an object")
            mode = sampler.get("interpolation", "LINEAR")
            if mode not in ALLOWED_INTERPOLATION:
                raise AuditError(f"unsupported interpolation {mode}")
            interpolation.append(mode)
            sampler_times.append(_read_scalar_float_accessor(doc, sampler.get("input")))

        path_counts: Counter[str] = Counter()
        interpolation_counts: Counter[str] = Counter()
        animated_nodes: set[int] = set()
        max_time = 0.0
        for channel in channels:
            if not isinstance(channel, dict):
                raise AuditError("animation channel must be an object")
            sampler_index = channel.get("sampler")
            target = channel.get("target")
            if not isinstance(sampler_index, int) or sampler_index < 0 or sampler_index >= len(samplers):
                raise AuditError("animation channel sampler index out of range")
            if not isinstance(target, dict):
                raise AuditError("animation channel target must be an object")
            node = target.get("node")
            path = target.get("path")
            if not isinstance(node, int) or node < 0 or node >= len(nodes):
                raise AuditError("animation channel target node out of range")
            if path not in ALLOWED_PATHS:
                raise AuditError(f"unsupported animation target path {path}")
            path_counts[path] += 1
            interpolation_counts[interpolation[sampler_index]] += 1
            animated_nodes.add(node)
            max_time = max(max_time, sampler_times[sampler_index][-1])

        clips.append({
            "name": animation["name"],
            "duration_seconds": max_time,
            "channel_count": len(channels),
            "sampler_count": len(samplers),
            "animated_node_count": len(animated_nodes),
            "target_path_counts": dict(sorted(path_counts.items())),
            "interpolation_counts": dict(sorted(interpolation_counts.items())),
        })

    return {
        "kind": "riftfrontier:region_01_boss_animation_audit",
        "schema_version": 1,
        "source": {
            "filename": gltf_path.name,
            "byte_size": len(raw),
            "sha256": actual_sha,
            "acceptance": acceptance_path.name,
        },
        "clip_count": len(clips),
        "clips": clips,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("gltf", type=Path)
    parser.add_argument("acceptance", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        report = audit(args.gltf, args.acceptance)
    except (AuditError, OSError, json.JSONDecodeError) as exc:
        print(f"ERROR: {exc}", file=__import__("sys").stderr)
        return 2
    text = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
