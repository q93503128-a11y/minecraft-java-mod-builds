#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import struct
import urllib.request
from pathlib import Path
from typing import Any

MIRROR_REPO = "trebeljahr/quaternius-showcase"
MIRROR_COMMIT = "e90ffea347393537703ae6f9d73e36492820f5a9"
MIRROR_PATH = "public/glb/easy_enemies_pack/Snake.glb"
SOURCE_URL = (
    "https://raw.githubusercontent.com/"
    f"{MIRROR_REPO}/{MIRROR_COMMIT}/{MIRROR_PATH}"
)
OFFICIAL_PACK_PAGE = "https://quaternius.itch.io/animated-easy-enemies"
POLY_PIZZA_MODEL_PAGE = "https://poly.pizza/m/x9x0viZs8V"
POLY_PIZZA_BUNDLE_PAGE = "https://poly.pizza/bundle/Animated-Enemies-a53OJwHrhh"

EXPECTED_SIZE = 216_696
EXPECTED_GIT_BLOB_SHA1 = "fdb107f06725f487fed66c7386b31c56c236e8ad"
EXPECTED_SHA256 = "70b4af1088dec939e167b07c0a5e69992a4851beac8bd24a9da1189b65ae101c"
EXPECTED_ANIMATIONS = {
    "SnakeArmature|Snake_Attack",
    "SnakeArmature|Snake_Idle",
    "SnakeArmature|Snake_Jump",
    "SnakeArmature|Snake_Walk",
}
EXPECTED_MATERIALS = {
    "DarkGreen",
    "LightGreen",
    "Red",
    "Teeth",
    "Purple",
    "Yellow",
    "DarkRed",
}
INDEPENDENT_MIRROR = {
    "repository": "marcoaureliomenezes/tauan-games",
    "path": "src/web-games/vendor/models/Snake.glb",
    "git_blob_sha1": EXPECTED_GIT_BLOB_SHA1,
    "bytes": EXPECTED_SIZE,
}
USER_AGENT = (
    "OpenworldRpgAssetIntake/0.1 "
    "(+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"
)

GLB_MAGIC = 0x46546C67
GLB_JSON_CHUNK = 0x4E4F534A
GLB_BIN_CHUNK = 0x004E4942


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def download_source() -> bytes:
    request = urllib.request.Request(
        SOURCE_URL,
        headers={"User-Agent": USER_AGENT},
    )
    with urllib.request.urlopen(request, timeout=90) as response:
        data = response.read()
    if len(data) != EXPECTED_SIZE:
        raise RuntimeError(
            f"Snake.glb byte-size mismatch: expected {EXPECTED_SIZE}, got {len(data)}"
        )
    actual_blob = git_blob_sha1(data)
    if actual_blob != EXPECTED_GIT_BLOB_SHA1:
        raise RuntimeError(
            "Snake.glb Git blob mismatch: "
            f"expected {EXPECTED_GIT_BLOB_SHA1}, got {actual_blob}"
        )
    actual_sha256 = sha256_bytes(data)
    if actual_sha256 != EXPECTED_SHA256:
        raise RuntimeError(
            "Snake.glb SHA-256 mismatch: "
            f"expected {EXPECTED_SHA256}, got {actual_sha256}"
        )
    return data


def parse_glb(data: bytes) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    if len(data) < 20:
        raise ValueError("GLB is too small.")
    magic, version, declared_length = struct.unpack_from("<III", data, 0)
    if magic != GLB_MAGIC:
        raise ValueError(f"Unexpected GLB magic: 0x{magic:08x}")
    if version != 2:
        raise ValueError(f"Unsupported GLB version: {version}")
    if declared_length != len(data):
        raise ValueError(
            f"GLB declared length {declared_length} != actual {len(data)}"
        )

    chunks: list[dict[str, Any]] = []
    json_payload: bytes | None = None
    offset = 12
    while offset < len(data):
        if offset + 8 > len(data):
            raise ValueError("Truncated GLB chunk header.")
        chunk_length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        end = offset + chunk_length
        if end > len(data):
            raise ValueError("Truncated GLB chunk payload.")
        payload = data[offset:end]
        chunks.append(
            {
                "type": f"0x{chunk_type:08x}",
                "bytes": chunk_length,
                "kind": (
                    "JSON"
                    if chunk_type == GLB_JSON_CHUNK
                    else "BIN"
                    if chunk_type == GLB_BIN_CHUNK
                    else "OTHER"
                ),
            }
        )
        if chunk_type == GLB_JSON_CHUNK:
            if json_payload is not None:
                raise ValueError("GLB contains multiple JSON chunks.")
            json_payload = payload
        offset = end

    if json_payload is None:
        raise ValueError("GLB has no JSON chunk.")

    doc = json.loads(json_payload.rstrip(b" \t\r\n\x00").decode("utf-8"))
    return doc, chunks


def accessor_summary(doc: dict[str, Any], index: int) -> dict[str, Any]:
    accessors = doc.get("accessors", [])
    if not 0 <= index < len(accessors):
        raise ValueError(f"Accessor index out of range: {index}")
    accessor = accessors[index]
    return {
        "index": index,
        "count": accessor.get("count"),
        "componentType": accessor.get("componentType"),
        "type": accessor.get("type"),
        "min": accessor.get("min"),
        "max": accessor.get("max"),
        "normalized": bool(accessor.get("normalized", False)),
    }


def inspect(data: bytes) -> dict[str, Any]:
    doc, chunks = parse_glb(data)
    nodes = doc.get("nodes", [])
    meshes = doc.get("meshes", [])
    materials = doc.get("materials", [])
    skins = doc.get("skins", [])
    animations = doc.get("animations", [])
    accessors = doc.get("accessors", [])

    animation_reports: list[dict[str, Any]] = []
    for animation in animations:
        samplers = animation.get("samplers", [])
        sampler_reports = []
        for sampler_index, sampler in enumerate(samplers):
            input_index = sampler.get("input")
            output_index = sampler.get("output")
            sampler_reports.append(
                {
                    "index": sampler_index,
                    "interpolation": sampler.get("interpolation", "LINEAR"),
                    "input": accessor_summary(doc, input_index),
                    "output": accessor_summary(doc, output_index),
                }
            )

        channels = []
        for channel in animation.get("channels", []):
            target = channel.get("target", {})
            node_index = target.get("node")
            node_name = None
            if isinstance(node_index, int) and 0 <= node_index < len(nodes):
                node_name = nodes[node_index].get("name")
            channels.append(
                {
                    "sampler": channel.get("sampler"),
                    "target_node": node_index,
                    "target_node_name": node_name,
                    "path": target.get("path"),
                }
            )

        duration = 0.0
        for sampler in sampler_reports:
            maximum = sampler["input"].get("max")
            if isinstance(maximum, list) and maximum:
                duration = max(duration, float(maximum[0]))

        animation_reports.append(
            {
                "name": animation.get("name"),
                "duration_seconds": duration,
                "samplers": sampler_reports,
                "channels": channels,
            }
        )

    material_reports = []
    for material in materials:
        pbr = material.get("pbrMetallicRoughness", {})
        material_reports.append(
            {
                "name": material.get("name"),
                "baseColorFactor": pbr.get("baseColorFactor"),
                "metallicFactor": pbr.get("metallicFactor"),
                "roughnessFactor": pbr.get("roughnessFactor"),
                "doubleSided": bool(material.get("doubleSided", False)),
                "alphaMode": material.get("alphaMode", "OPAQUE"),
            }
        )

    mesh_reports = []
    position_bounds: list[dict[str, Any]] = []
    for mesh_index, mesh in enumerate(meshes):
        primitive_reports = []
        for primitive_index, primitive in enumerate(mesh.get("primitives", [])):
            attrs = primitive.get("attributes", {})
            attr_reports = {
                semantic: accessor_summary(doc, accessor_index)
                for semantic, accessor_index in sorted(attrs.items())
            }
            position = attr_reports.get("POSITION")
            if position is not None:
                position_bounds.append(
                    {
                        "mesh": mesh_index,
                        "primitive": primitive_index,
                        "min": position.get("min"),
                        "max": position.get("max"),
                    }
                )
            primitive_reports.append(
                {
                    "index": primitive_index,
                    "mode": primitive.get("mode", 4),
                    "material": primitive.get("material"),
                    "indices": (
                        accessor_summary(doc, primitive["indices"])
                        if "indices" in primitive
                        else None
                    ),
                    "attributes": attr_reports,
                }
            )
        mesh_reports.append(
            {
                "index": mesh_index,
                "name": mesh.get("name"),
                "primitives": primitive_reports,
            }
        )

    skin_reports = []
    for skin_index, skin in enumerate(skins):
        joints = skin.get("joints", [])
        skin_reports.append(
            {
                "index": skin_index,
                "name": skin.get("name"),
                "skeleton": skin.get("skeleton"),
                "joint_count": len(joints),
                "joint_nodes": joints,
                "joint_names": [
                    nodes[index].get("name")
                    if isinstance(index, int) and 0 <= index < len(nodes)
                    else None
                    for index in joints
                ],
                "inverse_bind_matrices": (
                    accessor_summary(doc, skin["inverseBindMatrices"])
                    if "inverseBindMatrices" in skin
                    else None
                ),
            }
        )

    animation_names = {
        animation.get("name")
        for animation in animations
        if animation.get("name")
    }
    material_names = {
        material.get("name")
        for material in materials
        if material.get("name")
    }

    if animation_names != EXPECTED_ANIMATIONS:
        raise RuntimeError(
            "Unexpected Snake animation set: "
            f"expected {sorted(EXPECTED_ANIMATIONS)}, got {sorted(animation_names)}"
        )
    if material_names != EXPECTED_MATERIALS:
        raise RuntimeError(
            "Unexpected Snake material set: "
            f"expected {sorted(EXPECTED_MATERIALS)}, got {sorted(material_names)}"
        )
    if len(skins) != 1:
        raise RuntimeError(f"Expected exactly one Snake skin, got {len(skins)}")
    if not meshes:
        raise RuntimeError("Snake GLB has no mesh.")
    if not accessors:
        raise RuntimeError("Snake GLB has no accessors.")

    images = doc.get("images", [])
    textures = doc.get("textures", [])
    return {
        "schema_version": 1,
        "asset": {
            "logical_id": "openworld_rpg:r01/meadow_viper/source_snake",
            "filename": "Snake.glb",
            "bytes": len(data),
            "sha256": sha256_bytes(data),
            "git_blob_sha1": git_blob_sha1(data),
        },
        "provenance": {
            "creator": "Quaternius",
            "pack": "LowPoly Animated Easy Enemies",
            "license": "CC0-1.0",
            "official_pack_page": OFFICIAL_PACK_PAGE,
            "poly_pizza_model_page": POLY_PIZZA_MODEL_PAGE,
            "poly_pizza_bundle_page": POLY_PIZZA_BUNDLE_PAGE,
            "transport_repository": MIRROR_REPO,
            "transport_commit": MIRROR_COMMIT,
            "transport_path": MIRROR_PATH,
            "transport_url": SOURCE_URL,
            "expected_git_blob_sha1": EXPECTED_GIT_BLOB_SHA1,
            "expected_sha256": EXPECTED_SHA256,
            "independent_mirror_cross_check": INDEPENDENT_MIRROR,
        },
        "glb": {
            "version": 2,
            "generator": doc.get("asset", {}).get("generator"),
            "scene": doc.get("scene"),
            "scenes": doc.get("scenes", []),
            "extensions_used": doc.get("extensionsUsed", []),
            "extensions_required": doc.get("extensionsRequired", []),
            "chunks": chunks,
            "node_count": len(nodes),
            "nodes": [
                {
                    "index": index,
                    "name": node.get("name"),
                    "mesh": node.get("mesh"),
                    "skin": node.get("skin"),
                    "children": node.get("children", []),
                    "translation": node.get("translation"),
                    "rotation": node.get("rotation"),
                    "scale": node.get("scale"),
                    "matrix": node.get("matrix"),
                }
                for index, node in enumerate(nodes)
            ],
            "mesh_count": len(meshes),
            "meshes": mesh_reports,
            "material_count": len(materials),
            "materials": material_reports,
            "skin_count": len(skins),
            "skins": skin_reports,
            "animation_count": len(animations),
            "animations": animation_reports,
            "accessor_count": len(accessors),
            "position_bounds": position_bounds,
            "image_count": len(images),
            "images": images,
            "texture_count": len(textures),
            "textures": textures,
        },
        "contract": {
            "expected_animations": sorted(EXPECTED_ANIMATIONS),
            "expected_materials": sorted(EXPECTED_MATERIALS),
            "animation_contract_pass": animation_names == EXPECTED_ANIMATIONS,
            "material_contract_pass": material_names == EXPECTED_MATERIALS,
            "ready_for_runtime_conversion": True,
            "minecraft_scale_accepted": False,
            "minecraft_ground_contact_accepted": False,
            "minecraft_attack_visual_reach_accepted": False,
            "production_presentation_ready": False,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Acquire and inspect the exact CC0 Quaternius Snake source "
            "selected for the R01 Meadow Viper."
        )
    )
    parser.add_argument(
        "output_directory",
        type=Path,
        help="Directory for the pinned source GLB and inspection report.",
    )
    args = parser.parse_args()

    output = args.output_directory.resolve()
    output.mkdir(parents=True, exist_ok=True)

    data = download_source()
    source_path = output / "Snake.glb"
    source_path.write_bytes(data)

    report = inspect(data)
    report_path = output / "meadow_viper_asset_intake.json"
    report_path.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(
        "MEADOW_VIPER_SOURCE_PASS "
        f"bytes={len(data)} "
        f"git_blob_sha1={report['asset']['git_blob_sha1']} "
        f"sha256={report['asset']['sha256']} "
        f"animations={report['glb']['animation_count']} "
        f"materials={report['glb']['material_count']} "
        f"skins={report['glb']['skin_count']} "
        f"nodes={report['glb']['node_count']} "
        f"meshes={report['glb']['mesh_count']}"
    )
    for animation in report["glb"]["animations"]:
        print(
            "MEADOW_VIPER_ANIMATION "
            f"name={animation['name']} "
            f"duration={animation['duration_seconds']:.6f} "
            f"channels={len(animation['channels'])}"
        )
    print(f"MEADOW_VIPER_REPORT path={report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
