from __future__ import annotations

import importlib.util
import json
import struct
import tempfile
import unittest
import sys
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "inspect_gltf.py"
SPEC = importlib.util.spec_from_file_location("inspect_gltf", MODULE_PATH)
assert SPEC and SPEC.loader
inspect_gltf = importlib.util.module_from_spec(SPEC)
sys.modules["inspect_gltf"] = inspect_gltf
SPEC.loader.exec_module(inspect_gltf)


def make_document() -> dict:
    return {
        "asset": {"version": "2.0"},
        "nodes": [{"name": "root"}, {"name": "hand"}],
        "accessors": [
            {"count": 6, "type": "VEC3", "min": [-1, 0, -2], "max": [2, 3, 4]},
            {"count": 6, "type": "SCALAR"},
        ],
        "materials": [{"name": "body"}],
        "meshes": [
            {
                "primitives": [
                    {
                        "attributes": {"POSITION": 0},
                        "indices": 1,
                        "mode": 4,
                        "material": 0,
                    }
                ]
            }
        ],
        "skins": [{"joints": [0, 1]}],
        "animations": [
            {"name": "Idle"},
            {"name": "Attack_Slam"},
            {"name": "Charge"},
        ],
    }


def write_glb(path: Path, document: dict) -> None:
    payload = json.dumps(document, separators=(",", ":")).encode("utf-8")
    padding = (-len(payload)) % 4
    payload += b" " * padding
    total = 12 + 8 + len(payload)
    raw = struct.pack("<4sII", b"glTF", 2, total)
    raw += struct.pack("<II", len(payload), 0x4E4F534A)
    raw += payload
    path.write_bytes(raw)


class InspectGltfTest(unittest.TestCase):
    def test_reads_glb_metrics_and_bounds(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "candidate.glb"
            write_glb(path, make_document())
            metrics = inspect_gltf.inspect(path)

        self.assertEqual(metrics.format, "glb")
        self.assertEqual(metrics.animation_count, 3)
        self.assertEqual(metrics.animation_names, ["Idle", "Attack_Slam", "Charge"])
        self.assertEqual(metrics.max_bones_per_skin, 2)
        self.assertEqual(metrics.estimated_vertices, 6)
        self.assertEqual(metrics.estimated_triangles, 2)
        self.assertEqual(metrics.bounds_min, [-1.0, 0.0, -2.0])
        self.assertEqual(metrics.bounds_max, [2.0, 3.0, 4.0])
        self.assertEqual(metrics.bounds_size, [3.0, 3.0, 6.0])

    def test_gate_reports_stable_issue_codes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "candidate.gltf"
            path.write_text(json.dumps(make_document()), encoding="utf-8")
            metrics = inspect_gltf.inspect(path)

        args = inspect_gltf.build_parser().parse_args(
            [
                str(path),
                "--require-skin",
                "--require-named-animations",
                "--min-animations",
                "4",
                "--max-triangles",
                "1",
                "--max-materials",
                "0",
                "--max-bones",
                "1",
                "--require-animation",
                "recovery",
            ]
        )
        codes = {issue.code for issue in inspect_gltf.evaluate(metrics, args)}
        self.assertEqual(
            codes,
            {
                "GLTF_ANIMATION_COUNT_LOW",
                "GLTF_TRIANGLE_BUDGET_EXCEEDED",
                "GLTF_MATERIAL_BUDGET_EXCEEDED",
                "GLTF_BONE_BUDGET_EXCEEDED",
                "GLTF_REQUIRED_ANIMATION_MISSING",
            },
        )

    def test_rejects_invalid_glb_length(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "broken.glb"
            path.write_bytes(struct.pack("<4sII", b"glTF", 2, 999))
            with self.assertRaises(inspect_gltf.InspectionError):
                inspect_gltf.inspect(path)


if __name__ == "__main__":
    unittest.main()
