import base64
import importlib.util
import json
import struct
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "audit_region01_boss_runtime_mesh.py"
SPEC = importlib.util.spec_from_file_location("audit_region01_boss_runtime_mesh", MODULE_PATH)
AUDIT = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(AUDIT)


def _make_fixture(path: Path, joints, weights, indices=(0, 1, 2)):
    blob = bytearray()
    views = []
    accessors = []

    def add(data: bytes, component_type: int, count: int, type_name: str):
        offset = len(blob)
        blob.extend(data)
        views.append({"buffer": 0, "byteOffset": offset, "byteLength": len(data)})
        accessors.append({"bufferView": len(views) - 1, "componentType": component_type, "count": count, "type": type_name})
        return len(accessors) - 1

    joint_bytes = b"".join(struct.pack("<4B", *row) for row in joints)
    weight_bytes = b"".join(struct.pack("<4f", *row) for row in weights)
    index_bytes = struct.pack("<" + "H" * len(indices), *indices)
    j = add(joint_bytes, 5121, len(joints), "VEC4")
    w = add(weight_bytes, 5126, len(weights), "VEC4")
    idx = add(index_bytes, 5123, len(indices), "SCALAR")
    gltf = {
        "asset": {"version": "2.0"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"name": "Mesh", "mesh": 0, "skin": 0}, {"name": "BoneA"}, {"name": "BoneB"}],
        "skins": [{"joints": [1, 2]}],
        "meshes": [{"primitives": [{"mode": 4, "attributes": {"JOINTS_0": j, "WEIGHTS_0": w}, "indices": idx}]}],
        "accessors": accessors,
        "bufferViews": views,
        "buffers": [{"byteLength": len(blob), "uri": "data:application/octet-stream;base64," + base64.b64encode(blob).decode("ascii")}],
    }
    path.write_text(json.dumps(gltf), encoding="utf-8")
    return path


class RuntimeMeshAuditTests(unittest.TestCase):
    def test_accepts_single_bone_rigid_triangle(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = _make_fixture(Path(tmp) / "rigid.gltf", [(0, 0, 0, 0)] * 3, [(1.0, 0.0, 0.0, 0.0)] * 3)
            result = AUDIT.audit(path)
            self.assertTrue(result["lossless_rigid_bone_eligible"])
            self.assertEqual(0, result["blended_vertex_count"])
            self.assertEqual(0, result["cross_bone_triangle_count"])

    def test_rejects_linear_blend_skinning(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = _make_fixture(
                Path(tmp) / "blend.gltf",
                [(0, 1, 0, 0)] * 3,
                [(0.75, 0.25, 0.0, 0.0), (1.0, 0.0, 0.0, 0.0), (1.0, 0.0, 0.0, 0.0)],
            )
            result = AUDIT.audit(path)
            self.assertFalse(result["lossless_rigid_bone_eligible"])
            self.assertEqual("BOSS_RUNTIME_MESH_RIGID_POLYMESH_LOSSY", result["decision_code"])
            self.assertEqual(1, result["blended_vertex_count"])

    def test_rejects_triangle_spanning_rigid_bones(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = _make_fixture(
                Path(tmp) / "cross.gltf",
                [(0, 0, 0, 0), (1, 0, 0, 0), (1, 0, 0, 0)],
                [(1.0, 0.0, 0.0, 0.0)] * 3,
            )
            result = AUDIT.audit(path)
            self.assertFalse(result["lossless_rigid_bone_eligible"])
            self.assertEqual(1, result["cross_bone_triangle_count"])

    def test_fails_closed_without_skin_attributes(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = _make_fixture(Path(tmp) / "missing.gltf", [(0, 0, 0, 0)] * 3, [(1.0, 0.0, 0.0, 0.0)] * 3)
            gltf = json.loads(path.read_text(encoding="utf-8"))
            del gltf["meshes"][0]["primitives"][0]["attributes"]["WEIGHTS_0"]
            path.write_text(json.dumps(gltf), encoding="utf-8")
            with self.assertRaises(AUDIT.AuditError) as raised:
                AUDIT.audit(path)
            self.assertEqual("BOSS_RUNTIME_MESH_SKIN_ATTRIBUTES_REQUIRED", raised.exception.code)


if __name__ == "__main__":
    unittest.main()
