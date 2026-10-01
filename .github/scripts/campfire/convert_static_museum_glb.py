#!/usr/bin/env python3
"""Convert a verified static glTF/GLB museum source into Campfire runtime mesh assets.

This converter is intentionally narrow. It preserves real production geometry and
material references for static museum exhibits and refuses unsupported glTF
features instead of silently degrading them.

Runtime mesh format (little endian), one file per glTF primitive:
- 4 bytes magic: CFMS
- uint32 version: 1
- uint32 vertex_count
- uint32 index_count
- 6 x float32 bounds: min xyz, max xyz
- vertex_count records of 8 x float32:
  position xyz, normal xyz, uv
- index_count x uint32

The JSON manifest keeps source provenance, node transforms, material factors and
texture bindings. No scale/orientation guess is baked into geometry.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import math
import pathlib
import struct
import sys
from dataclasses import dataclass
from typing import Any

MAGIC = b"CFMS"
VERSION = 1
VERTEX_FLOATS = 8
VERTEX_BYTES = VERTEX_FLOATS * 4

COMPONENTS = {
    5120: ("b", 1, True),   # BYTE
    5121: ("B", 1, False),  # UNSIGNED_BYTE
    5122: ("h", 2, True),   # SHORT
    5123: ("H", 2, False),  # UNSIGNED_SHORT
    5125: ("I", 4, False),  # UNSIGNED_INT
    5126: ("f", 4, None),   # FLOAT
}
TYPE_SIZES = {
    "SCALAR": 1,
    "VEC2": 2,
    "VEC3": 3,
    "VEC4": 4,
    "MAT2": 4,
    "MAT3": 9,
    "MAT4": 16,
}
IMAGE_EXTENSIONS = {
    "image/png": ".png",
    "image/jpeg": ".jpg",
    "image/webp": ".webp",
}


@dataclass(frozen=True)
class Glb:
    document: dict[str, Any]
    binary: bytes


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fp:
        for chunk in iter(lambda: fp.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def parse_glb(path: pathlib.Path) -> Glb:
    raw = path.read_bytes()
    if len(raw) < 20:
        raise ValueError("GLB is too short")
    magic, version, declared_length = struct.unpack_from("<4sII", raw, 0)
    if magic != b"glTF" or version != 2:
        raise ValueError(f"expected glTF 2 GLB, got magic={magic!r} version={version}")
    if declared_length != len(raw):
        raise ValueError(f"declared GLB length {declared_length} != actual {len(raw)}")

    offset = 12
    doc = None
    binary = b""
    while offset + 8 <= len(raw):
        chunk_length, chunk_type = struct.unpack_from("<II", raw, offset)
        offset += 8
        chunk = raw[offset:offset + chunk_length]
        offset += chunk_length
        if len(chunk) != chunk_length:
            raise ValueError("truncated GLB chunk")
        if chunk_type == 0x4E4F534A:
            doc = json.loads(chunk.rstrip(b"\x00 \t\r\n").decode("utf-8"))
        elif chunk_type == 0x004E4942:
            binary = chunk

    if doc is None:
        raise ValueError("GLB JSON chunk missing")
    if not binary:
        raise ValueError("GLB BIN chunk missing")

    return Glb(doc, binary)


def normalize_integer(value: int, component_type: int) -> float:
    if component_type == 5120:
        return max(-1.0, value / 127.0)
    if component_type == 5121:
        return value / 255.0
    if component_type == 5122:
        return max(-1.0, value / 32767.0)
    if component_type == 5123:
        return value / 65535.0
    if component_type == 5125:
        return value / 4294967295.0
    raise ValueError(f"cannot normalize component type {component_type}")


def read_accessor(glb: Glb, accessor_index: int) -> list[tuple[float | int, ...]]:
    doc = glb.document
    accessors = doc.get("accessors", [])
    views = doc.get("bufferViews", [])
    if not (0 <= accessor_index < len(accessors)):
        raise ValueError(f"invalid accessor index {accessor_index}")

    accessor = accessors[accessor_index]
    if "sparse" in accessor:
        raise ValueError(f"sparse accessor {accessor_index} is not supported")
    if "bufferView" not in accessor:
        raise ValueError(f"accessor {accessor_index} has no bufferView")

    view_index = accessor["bufferView"]
    if not (0 <= view_index < len(views)):
        raise ValueError(f"accessor {accessor_index} has invalid bufferView {view_index}")
    view = views[view_index]
    if view.get("buffer", 0) != 0:
        raise ValueError("only GLB buffer 0 is supported")

    component_type = accessor["componentType"]
    if component_type not in COMPONENTS:
        raise ValueError(f"unsupported componentType {component_type}")
    fmt, component_bytes, _signed = COMPONENTS[component_type]
    type_name = accessor["type"]
    component_count = TYPE_SIZES.get(type_name)
    if component_count is None:
        raise ValueError(f"unsupported accessor type {type_name}")

    count = int(accessor["count"])
    element_bytes = component_bytes * component_count
    stride = int(view.get("byteStride", element_bytes))
    if stride < element_bytes:
        raise ValueError(f"bufferView stride {stride} < element size {element_bytes}")

    base = int(view.get("byteOffset", 0)) + int(accessor.get("byteOffset", 0))
    view_end = int(view.get("byteOffset", 0)) + int(view["byteLength"])
    normalized = bool(accessor.get("normalized", False))
    unpack_fmt = "<" + fmt * component_count

    result: list[tuple[float | int, ...]] = []
    for i in range(count):
        offset = base + i * stride
        end = offset + element_bytes
        if end > len(glb.binary) or end > view_end:
            raise ValueError(f"accessor {accessor_index} reads beyond bufferView")
        values = struct.unpack_from(unpack_fmt, glb.binary, offset)
        if normalized and component_type != 5126:
            result.append(tuple(normalize_integer(int(v), component_type) for v in values))
        else:
            result.append(tuple(values))
    return result


def require_static_source(doc: dict[str, Any]) -> None:
    if doc.get("skins"):
        raise ValueError("museum static mesh source unexpectedly contains skins")
    if doc.get("animations"):
        raise ValueError("museum static mesh source unexpectedly contains animations")

    forbidden_required = set(doc.get("extensionsRequired", []))
    if forbidden_required:
        raise ValueError(
            "runtime source must be decoded before conversion; unsupported required extensions: "
            + ", ".join(sorted(forbidden_required))
        )

    for mesh_index, mesh in enumerate(doc.get("meshes", [])):
        if mesh.get("weights") is not None:
            raise ValueError(f"mesh {mesh_index} has morph weights")
        for primitive_index, primitive in enumerate(mesh.get("primitives", [])):
            if primitive.get("targets"):
                raise ValueError(f"mesh {mesh_index} primitive {primitive_index} has morph targets")
            if primitive.get("mode", 4) != 4:
                raise ValueError(f"mesh {mesh_index} primitive {primitive_index} is not TRIANGLES")
            attrs = primitive.get("attributes", {})
            for name in ("POSITION", "NORMAL", "TEXCOORD_0"):
                if name not in attrs:
                    raise ValueError(f"mesh {mesh_index} primitive {primitive_index} missing {name}")
            if any(key.startswith("JOINTS_") or key.startswith("WEIGHTS_") for key in attrs):
                raise ValueError(f"mesh {mesh_index} primitive {primitive_index} contains skin weights")
            if "indices" not in primitive:
                raise ValueError(f"mesh {mesh_index} primitive {primitive_index} is non-indexed")


def image_bytes(glb: Glb, image: dict[str, Any]) -> tuple[bytes, str]:
    mime = image.get("mimeType")
    if "bufferView" in image:
        views = glb.document.get("bufferViews", [])
        view_index = image["bufferView"]
        if not (0 <= view_index < len(views)):
            raise ValueError(f"invalid image bufferView {view_index}")
        view = views[view_index]
        if view.get("buffer", 0) != 0:
            raise ValueError("only GLB image buffer 0 is supported")
        start = int(view.get("byteOffset", 0))
        end = start + int(view["byteLength"])
        if end > len(glb.binary):
            raise ValueError("image bufferView exceeds GLB binary")
        if mime not in IMAGE_EXTENSIONS:
            raise ValueError(f"unsupported embedded image MIME {mime!r}")
        return glb.binary[start:end], IMAGE_EXTENSIONS[mime]

    uri = image.get("uri")
    if isinstance(uri, str) and uri.startswith("data:"):
        header, encoded = uri.split(",", 1)
        mime = header[5:].split(";", 1)[0]
        if ";base64" not in header:
            raise ValueError("only base64 data URI images are supported")
        if mime not in IMAGE_EXTENSIONS:
            raise ValueError(f"unsupported data URI MIME {mime!r}")
        return base64.b64decode(encoded), IMAGE_EXTENSIONS[mime]

    raise ValueError("external image URI is not allowed in verified GLB conversion")


def texture_image_index(doc: dict[str, Any], texture_info: Any) -> int | None:
    if not isinstance(texture_info, dict):
        return None
    if texture_info.get("texCoord", 0) != 0:
        raise ValueError(f"only TEXCOORD_0 is supported, got {texture_info.get('texCoord')}")
    if texture_info.get("extensions"):
        raise ValueError("texture-info extensions are not supported")
    texture_index = texture_info.get("index")
    if not isinstance(texture_index, int):
        return None
    textures = doc.get("textures", [])
    if not (0 <= texture_index < len(textures)):
        raise ValueError(f"invalid texture index {texture_index}")
    texture = textures[texture_index]
    if texture.get("extensions"):
        raise ValueError("texture extensions are not supported")
    source = texture.get("source")
    if not isinstance(source, int):
        raise ValueError(f"texture {texture_index} has no direct image source")
    return source


def write_primitive(
    glb: Glb,
    mesh_index: int,
    primitive_index: int,
    primitive: dict[str, Any],
    output_path: pathlib.Path,
) -> dict[str, Any]:
    attrs = primitive["attributes"]
    positions = read_accessor(glb, attrs["POSITION"])
    normals = read_accessor(glb, attrs["NORMAL"])
    uvs = read_accessor(glb, attrs["TEXCOORD_0"])
    indices_raw = read_accessor(glb, primitive["indices"])

    if not (len(positions) == len(normals) == len(uvs)):
        raise ValueError(
            f"mesh {mesh_index} primitive {primitive_index} attribute counts differ: "
            f"position={len(positions)} normal={len(normals)} uv={len(uvs)}"
        )
    if not indices_raw:
        raise ValueError("primitive has no indices")
    indices = [int(row[0]) for row in indices_raw]
    if len(indices) % 3 != 0:
        raise ValueError(f"triangle index count {len(indices)} is not divisible by 3")
    if min(indices) < 0 or max(indices) >= len(positions):
        raise ValueError("primitive index is outside vertex range")

    min_xyz = [math.inf, math.inf, math.inf]
    max_xyz = [-math.inf, -math.inf, -math.inf]
    for pos in positions:
        for axis in range(3):
            value = float(pos[axis])
            if not math.isfinite(value):
                raise ValueError("non-finite vertex position")
            min_xyz[axis] = min(min_xyz[axis], value)
            max_xyz[axis] = max(max_xyz[axis], value)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("wb") as fp:
        fp.write(MAGIC)
        fp.write(struct.pack("<III", VERSION, len(positions), len(indices)))
        fp.write(struct.pack("<6f", *(min_xyz + max_xyz)))
        for pos, normal, uv in zip(positions, normals, uvs, strict=True):
            nx, ny, nz = (float(normal[0]), float(normal[1]), float(normal[2]))
            length = math.sqrt(nx * nx + ny * ny + nz * nz)
            if length <= 1.0e-12:
                raise ValueError("zero-length normal in production source")
            nx, ny, nz = nx / length, ny / length, nz / length
            fp.write(
                struct.pack(
                    "<8f",
                    float(pos[0]), float(pos[1]), float(pos[2]),
                    nx, ny, nz,
                    float(uv[0]), float(uv[1]),
                )
            )
        for index in indices:
            fp.write(struct.pack("<I", index))

    expected = 4 + 12 + 24 + len(positions) * VERTEX_BYTES + len(indices) * 4
    actual = output_path.stat().st_size
    if actual != expected:
        raise AssertionError(f"runtime mesh size mismatch expected={expected} actual={actual}")

    return {
        "file": output_path.name,
        "sha256": sha256_file(output_path),
        "vertex_count": len(positions),
        "index_count": len(indices),
        "triangle_count": len(indices) // 3,
        "bounds": {"min": min_xyz, "max": max_xyz},
        "material_index": int(primitive.get("material", -1)),
    }


def extract_materials(glb: Glb, output_dir: pathlib.Path) -> list[dict[str, Any]]:
    doc = glb.document
    images = doc.get("images", [])
    texture_dir = output_dir / "textures"
    texture_dir.mkdir(parents=True, exist_ok=True)
    extracted: dict[int, dict[str, Any]] = {}

    def extract(index: int | None) -> dict[str, Any] | None:
        if index is None:
            return None
        if not (0 <= index < len(images)):
            raise ValueError(f"material references invalid image {index}")
        if index in extracted:
            return extracted[index]
        raw, extension = image_bytes(glb, images[index])
        name = f"image_{index:02d}{extension}"
        path = texture_dir / name
        path.write_bytes(raw)
        entry = {
            "image_index": index,
            "file": f"textures/{name}",
            "sha256": sha256_bytes(raw),
            "bytes": len(raw),
            "mime_type": images[index].get("mimeType"),
        }
        extracted[index] = entry
        return entry

    materials: list[dict[str, Any]] = []
    for index, material in enumerate(doc.get("materials", [])):
        if material.get("extensions"):
            raise ValueError(f"material {index} contains unsupported extensions")
        alpha_mode = material.get("alphaMode", "OPAQUE")
        if alpha_mode != "OPAQUE":
            raise ValueError(f"museum material {index} alphaMode={alpha_mode}; only OPAQUE supported")
        if material.get("doubleSided", False):
            raise ValueError(f"museum material {index} requires double-sided rendering")

        pbr = material.get("pbrMetallicRoughness", {}) or {}
        if pbr.get("metallicRoughnessTexture") is not None:
            raise ValueError(f"material {index} has metallicRoughnessTexture; pipeline not accepted yet")
        if material.get("emissiveTexture") is not None:
            raise ValueError(f"material {index} has emissiveTexture; pipeline not accepted yet")

        base = extract(texture_image_index(doc, pbr.get("baseColorTexture")))
        normal_info = material.get("normalTexture")
        normal = extract(texture_image_index(doc, normal_info))
        occlusion_info = material.get("occlusionTexture")
        occlusion = extract(texture_image_index(doc, occlusion_info))

        materials.append({
            "index": index,
            "name": material.get("name"),
            "base_color_factor": pbr.get("baseColorFactor", [1, 1, 1, 1]),
            "metallic_factor": pbr.get("metallicFactor", 1),
            "roughness_factor": pbr.get("roughnessFactor", 1),
            "base_color_texture": base,
            "normal_texture": normal,
            "normal_scale": normal_info.get("scale", 1) if isinstance(normal_info, dict) else None,
            "occlusion_texture": occlusion,
            "occlusion_strength": occlusion_info.get("strength", 1) if isinstance(occlusion_info, dict) else None,
        })
    return materials


def mesh_nodes(doc: dict[str, Any]) -> list[dict[str, Any]]:
    result = []
    for index, node in enumerate(doc.get("nodes", [])):
        if not isinstance(node.get("mesh"), int):
            continue
        entry = {
            "node_index": index,
            "name": node.get("name"),
            "mesh_index": node["mesh"],
        }
        if "matrix" in node:
            entry["matrix"] = node["matrix"]
        else:
            entry["translation"] = node.get("translation", [0, 0, 0])
            entry["rotation"] = node.get("rotation", [0, 0, 0, 1])
            entry["scale"] = node.get("scale", [1, 1, 1])
        result.append(entry)
    return result


def verify_runtime_mesh(path: pathlib.Path) -> dict[str, Any]:
    raw = path.read_bytes()
    if len(raw) < 40:
        raise ValueError(f"{path}: runtime mesh too short")
    magic = raw[:4]
    if magic != MAGIC:
        raise ValueError(f"{path}: invalid magic {magic!r}")
    version, vertex_count, index_count = struct.unpack_from("<III", raw, 4)
    if version != VERSION:
        raise ValueError(f"{path}: unsupported version {version}")
    bounds = struct.unpack_from("<6f", raw, 16)
    expected = 40 + vertex_count * VERTEX_BYTES + index_count * 4
    if len(raw) != expected:
        raise ValueError(f"{path}: expected {expected} bytes, got {len(raw)}")
    index_offset = 40 + vertex_count * VERTEX_BYTES
    max_index = -1
    for i in range(index_count):
        (index,) = struct.unpack_from("<I", raw, index_offset + i * 4)
        max_index = max(max_index, index)
    if max_index >= vertex_count:
        raise ValueError(f"{path}: max index {max_index} >= vertex count {vertex_count}")
    return {
        "file": path.name,
        "sha256": sha256_bytes(raw),
        "vertex_count": vertex_count,
        "index_count": index_count,
        "triangle_count": index_count // 3,
        "bounds": {"min": list(bounds[:3]), "max": list(bounds[3:])},
    }


def convert(input_path: pathlib.Path, output_dir: pathlib.Path, expected_source_sha256: str) -> dict[str, Any]:
    actual_source_sha256 = sha256_file(input_path)
    expected_source_sha256 = expected_source_sha256.lower()
    if actual_source_sha256.lower() != expected_source_sha256:
        raise ValueError(
            f"verified museum source hash mismatch: expected={expected_source_sha256} actual={actual_source_sha256}"
        )

    glb = parse_glb(input_path)
    doc = glb.document
    require_static_source(doc)

    meshes = doc.get("meshes", [])
    if not meshes:
        raise ValueError("source contains no meshes")

    output_dir.mkdir(parents=True, exist_ok=True)
    primitive_reports = []
    for mesh_index, mesh in enumerate(meshes):
        primitives = mesh.get("primitives", [])
        for primitive_index, primitive in enumerate(primitives):
            filename = f"mesh_{mesh_index:02d}_primitive_{primitive_index:02d}.cfmesh"
            report = write_primitive(
                glb,
                mesh_index,
                primitive_index,
                primitive,
                output_dir / filename,
            )
            report["mesh_index"] = mesh_index
            report["primitive_index"] = primitive_index
            primitive_reports.append(report)

    materials = extract_materials(glb, output_dir)

    manifest = {
        "format": "campfire-static-museum-mesh",
        "version": VERSION,
        "source": {
            "file": input_path.name,
            "sha256": actual_source_sha256,
            "verified_expected_sha256": expected_source_sha256,
            "asset_generator": doc.get("asset", {}).get("generator"),
            "gltf_version": doc.get("asset", {}).get("version"),
        },
        "coordinate_policy": {
            "geometry": "source glTF coordinates preserved",
            "uv": "source TEXCOORD_0 preserved",
            "production_scale": "not baked; must be supplied by curated exhibit metadata",
            "production_orientation": "not baked; must be supplied by curated exhibit metadata",
        },
        "scene_index": int(doc.get("scene", 0)),
        "scenes": doc.get("scenes", []),
        "mesh_nodes": mesh_nodes(doc),
        "primitives": primitive_reports,
        "materials": materials,
    }

    manifest_path = output_dir / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    verification = [verify_runtime_mesh(output_dir / item["file"]) for item in primitive_reports]
    summary = {
        "manifest": str(manifest_path),
        "manifest_sha256": sha256_file(manifest_path),
        "runtime_meshes": verification,
        "material_count": len(materials),
    }
    (output_dir / "verification.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"converted {input_path} -> {output_dir}")
    print(f"source_sha256={manifest['source']['sha256']}")
    for item in verification:
        print(
            f"  {item['file']}: vertices={item['vertex_count']} "
            f"indices={item['index_count']} triangles={item['triangle_count']} "
            f"sha256={item['sha256']}"
        )
    for material in materials:
        print(
            f"  material {material['index']}: "
            f"base={material['base_color_texture']['file'] if material['base_color_texture'] else 'factor-only'} "
            f"normal={material['normal_texture']['file'] if material['normal_texture'] else 'none'} "
            f"occlusion={material['occlusion_texture']['file'] if material['occlusion_texture'] else 'none'}"
        )
    print("Campfire static museum conversion: PASS")
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=pathlib.Path)
    parser.add_argument("output", type=pathlib.Path)
    parser.add_argument("--expected-source-sha256", required=True)
    args = parser.parse_args()

    try:
        convert(args.input, args.output, args.expected_source_sha256)
    except Exception as exc:
        print(f"ERROR: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
