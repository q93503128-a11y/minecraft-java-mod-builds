#!/usr/bin/env python3
from __future__ import annotations

import copy
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from inspect_plumberry_assets import gltf_render_profile


def base_document() -> dict:
    return {
        "asset": {"version": "2.0"},
        "nodes": [{"name": "root", "mesh": 0}],
        "meshes": [{
            "primitives": [
                {
                    "attributes": {
                        "POSITION": 0,
                        "NORMAL": 1,
                        "TEXCOORD_0": 2,
                    },
                    "indices": 3,
                    "material": 0,
                    "mode": 4,
                },
                {
                    "attributes": {
                        "POSITION": 4,
                        "NORMAL": 5,
                        "TEXCOORD_0": 6,
                    },
                    "indices": 7,
                    "material": 1,
                    "mode": 4,
                },
            ]
        }],
        "materials": [
            {
                "name": "fur",
                "pbrMetallicRoughness": {
                    "baseColorFactor": [1, 0.8, 0.7, 1],
                    "baseColorTexture": {"index": 0},
                    "metallicFactor": 0,
                    "roughnessFactor": 1,
                },
            },
            {
                "name": "shirt",
                "pbrMetallicRoughness": {
                    "baseColorTexture": {"index": 1},
                    "metallicFactor": 0,
                    "roughnessFactor": 1,
                },
                "alphaMode": "MASK",
                "alphaCutoff": 0.5,
            },
        ],
        "textures": [
            {"source": 0},
            {"source": 1},
        ],
        "images": [
            {"mimeType": "image/png", "bufferView": 0},
            {"mimeType": "image/png", "bufferView": 1},
        ],
        "animations": [{
            "samplers": [
                {"input": 8, "output": 9, "interpolation": "LINEAR"},
                {"input": 8, "output": 10, "interpolation": "CUBICSPLINE"},
            ],
            "channels": [
                {"sampler": 0, "target": {"node": 0, "path": "translation"}},
                {"sampler": 1, "target": {"node": 0, "path": "rotation"}},
            ],
        }],
    }


def main() -> None:
    valid = gltf_render_profile(base_document())
    assert valid["candidate"], valid["blockers"]
    assert valid["material_count"] == 2
    assert valid["used_material_indices"] == [0, 1]
    assert valid["interpolation_counts"] == {"LINEAR": 1, "CUBICSPLINE": 1}

    bad_material = base_document()
    bad_material["materials"][0]["normalTexture"] = {"index": 0}
    bad_material["materials"][1]["alphaMode"] = "BLEND"
    blocked = gltf_render_profile(bad_material)
    assert not blocked["candidate"]
    assert any("normalTexture" in reason for reason in blocked["blockers"])
    assert any("alphaMode=BLEND" in reason for reason in blocked["blockers"])

    bad_animation = base_document()
    bad_animation["animations"][0]["samplers"][0]["interpolation"] = "CATMULLROM"
    blocked_animation = gltf_render_profile(bad_animation)
    assert not blocked_animation["candidate"]
    assert any("CATMULLROM" in reason for reason in blocked_animation["blockers"])

    bad_texture = base_document()
    bad_texture["materials"][0]["pbrMetallicRoughness"]["baseColorTexture"]["texCoord"] = 1
    blocked_texture = gltf_render_profile(bad_texture)
    assert not blocked_texture["candidate"]
    assert any("TEXCOORD_1" in reason for reason in blocked_texture["blockers"])

    print("Plumberry render-profile self-test: PASS")


if __name__ == "__main__":
    main()
