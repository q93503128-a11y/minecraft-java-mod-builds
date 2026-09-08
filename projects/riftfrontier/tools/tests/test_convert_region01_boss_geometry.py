from __future__ import annotations

import base64
import json
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

import convert_region01_boss_geometry as mod


def _data_uri(raw: bytes) -> str:
    return "data:application/octet-stream;base64," + base64.b64encode(raw).decode("ascii")


def _fixture(buffer_uri: str | None = None) -> dict:
    # Eight 4-byte chunks. View 6 is image/custom-only and should be pruned.
    raw = b"POS0NRM0JNT0WGT0IDX0IBM0IMG0ANI0"
    return {
        "asset": {"version": "2.0", "generator": "fixture"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [
            {"name": "Root", "children": [1]},
            {"name": "Mesh", "mesh": 0, "skin": 0},
        ],
        "meshes": [{
            "name": "BossMesh",
            "primitives": [{
                "attributes": {"POSITION": 0, "NORMAL": 1, "JOINTS_0": 2, "WEIGHTS_0": 3},
                "indices": 4,
                "material": 0,
            }],
        }],
        "skins": [{"name": "Rig", "joints": [0, 1], "skeleton": 0, "inverseBindMatrices": 5}],
        "animations": [{
            "name": "Punch",
            "samplers": [{"input": 6, "output": 7, "interpolation": "LINEAR"}],
            "channels": [{"sampler": 0, "target": {"node": 1, "path": "rotation"}}],
        }],
        "accessors": [
            {"bufferView": 0, "componentType": 5126, "count": 1, "type": "VEC3"},
            {"bufferView": 1, "componentType": 5126, "count": 1, "type": "VEC3"},
            {"bufferView": 2, "componentType": 5121, "count": 1, "type": "VEC4"},
            {"bufferView": 3, "componentType": 5126, "count": 1, "type": "VEC4"},
            {"bufferView": 4, "componentType": 5123, "count": 3, "type": "SCALAR"},
            {"bufferView": 5, "componentType": 5126, "count": 2, "type": "MAT4"},
            {"bufferView": 7, "componentType": 5126, "count": 2, "type": "SCALAR", "min": [0.0], "max": [1.0]},
            {"bufferView": 7, "componentType": 5126, "count": 2, "type": "VEC4"},
            {"bufferView": 6, "componentType": 5126, "count": 1, "type": "SCALAR"},
        ],
        "bufferViews": [
            {"buffer": 0, "byteOffset": 0, "byteLength": 4},
            {"buffer": 0, "byteOffset": 4, "byteLength": 4},
            {"buffer": 0, "byteOffset": 8, "byteLength": 4},
            {"buffer": 0, "byteOffset": 12, "byteLength": 4},
            {"buffer": 0, "byteOffset": 16, "byteLength": 4},
            {"buffer": 0, "byteOffset": 20, "byteLength": 4},
            {"buffer": 0, "byteOffset": 24, "byteLength": 4},
            {"buffer": 0, "byteOffset": 28, "byteLength": 4},
        ],
        "buffers": [{"byteLength": len(raw), "uri": buffer_uri or _data_uri(raw)}],
        "materials": [{"name": "Atlas"}],
        "textures": [{"source": 0}],
        "images": [{"bufferView": 6, "mimeType": "image/png"}],
        "samplers": [{}],
    }


class BossGeometrySanitizerTests(unittest.TestCase):
    def test_strips_unapproved_art_and_prunes_image_only_bytes(self):
        doc = _fixture()
        sanitized, stats = mod.sanitize_document(doc, Path("Dragon_Evolved.gltf"))
        self.assertNotIn("materials", sanitized)
        self.assertNotIn("textures", sanitized)
        self.assertNotIn("images", sanitized)
        self.assertNotIn("samplers", sanitized)
        primitive = sanitized["meshes"][0]["primitives"][0]
        self.assertNotIn("material", primitive)
        self.assertEqual(8, stats["retained_accessor_count"])
        self.assertEqual(7, stats["retained_buffer_view_count"])
        self.assertEqual(28, stats["embedded_buffer_bytes"])
        raw = mod._decode_data_uri(sanitized["buffers"][0]["uri"], "output")
        self.assertNotIn(b"IMG0", raw)

    def test_canonical_output_is_deterministic(self):
        doc = _fixture()
        first, _ = mod.sanitize_document(doc, Path("Dragon_Evolved.gltf"))
        second, _ = mod.sanitize_document(json.loads(json.dumps(doc)), Path("Dragon_Evolved.gltf"))
        self.assertEqual(mod._canonical_json_bytes(first), mod._canonical_json_bytes(second))

    def test_relative_external_buffer_is_reembedded(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            raw = b"POS0NRM0JNT0WGT0IDX0IBM0IMG0ANI0"
            (root / "Dragon_Evolved.bin").write_bytes(raw)
            doc = _fixture("Dragon_Evolved.bin")
            sanitized, _ = mod.sanitize_document(doc, root / "Dragon_Evolved.gltf")
            self.assertTrue(sanitized["buffers"][0]["uri"].startswith("data:application/octet-stream;base64,"))

    def test_buffer_path_escape_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp) / "source"
            root.mkdir()
            doc = _fixture("../outside.bin")
            with self.assertRaises(mod.GeometryConversionError) as caught:
                mod.sanitize_document(doc, root / "Dragon_Evolved.gltf")
            self.assertEqual("BOSS_GEOMETRY_BUFFER_PATH_ESCAPE", caught.exception.code)

    def test_unknown_mesh_attribute_fails_closed(self):
        doc = _fixture()
        doc["meshes"][0]["primitives"][0]["attributes"]["CUSTOM_0"] = 8
        with self.assertRaises(mod.GeometryConversionError) as caught:
            mod.sanitize_document(doc, Path("Dragon_Evolved.gltf"))
        self.assertEqual("BOSS_GEOMETRY_ATTRIBUTE_UNSUPPORTED", caught.exception.code)

    def test_required_extension_fails_closed(self):
        doc = _fixture()
        doc["extensionsRequired"] = ["KHR_draco_mesh_compression"]
        with self.assertRaises(mod.GeometryConversionError) as caught:
            mod.sanitize_document(doc, Path("Dragon_Evolved.gltf"))
        self.assertEqual("BOSS_GEOMETRY_EXTENSION_UNSUPPORTED", caught.exception.code)

    def test_provenance_marks_material_as_not_approved(self):
        output = b"{}\n"
        provenance = mod.build_provenance("source", "0" * 64, output, {"mesh_count": 1})
        self.assertFalse(provenance["art_policy"]["final_material_approved"])
        self.assertIn("materials", provenance["art_policy"]["excluded"])
        self.assertEqual(mod.hashlib.sha256(output).hexdigest(), provenance["output_sha256"])


if __name__ == "__main__":
    unittest.main()
