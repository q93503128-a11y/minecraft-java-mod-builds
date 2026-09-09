import base64
import hashlib
import importlib.util
import json
import struct
import tempfile
import unittest
from pathlib import Path

TOOL = Path(__file__).parents[1] / "audit_region01_boss_animation_clips.py"
spec = importlib.util.spec_from_file_location("boss_anim_audit", TOOL)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)


class BossAnimationAuditTest(unittest.TestCase):
    def _write_fixture(self, root: Path, names=("Idle", "Attack")):
        raw_times = struct.pack("<3f", 0.0, 0.5, 1.25)
        doc = {
            "asset": {"version": "2.0"},
            "nodes": [{}, {}],
            "buffers": [{
                "byteLength": len(raw_times),
                "uri": "data:application/octet-stream;base64," + base64.b64encode(raw_times).decode(),
            }],
            "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": len(raw_times)}],
            "accessors": [{
                "bufferView": 0,
                "componentType": 5126,
                "count": 3,
                "type": "SCALAR",
                "min": [0.0],
                "max": [1.25],
            }],
            "animations": [
                {
                    "name": name,
                    "samplers": [{"input": 0, "output": 0, "interpolation": "LINEAR"}],
                    "channels": [{
                        "sampler": 0,
                        "target": {"node": i % 2, "path": "rotation" if i else "translation"},
                    }],
                }
                for i, name in enumerate(names)
            ],
        }
        gltf = root / "boss.gltf"
        gltf.write_text(json.dumps(doc, separators=(",", ":")), encoding="utf-8")
        raw = gltf.read_bytes()
        acceptance = root / "acceptance.json"
        acceptance.write_text(json.dumps({
            "output": {"geometry_sha256": hashlib.sha256(raw).hexdigest()},
            "expect": {"animations": list(names)},
        }), encoding="utf-8")
        return gltf, acceptance

    def test_records_exact_clip_metrics(self):
        with tempfile.TemporaryDirectory() as tmp:
            gltf, acceptance = self._write_fixture(Path(tmp))
            report = mod.audit(gltf, acceptance)
            self.assertEqual(2, report["clip_count"])
            self.assertEqual(1.25, report["clips"][0]["duration_seconds"])
            self.assertEqual({"translation": 1}, report["clips"][0]["target_path_counts"])
            self.assertEqual({"LINEAR": 1}, report["clips"][1]["interpolation_counts"])

    def test_rejects_sha_mismatch(self):
        with tempfile.TemporaryDirectory() as tmp:
            gltf, acceptance = self._write_fixture(Path(tmp))
            data = json.loads(acceptance.read_text())
            data["output"]["geometry_sha256"] = "0" * 64
            acceptance.write_text(json.dumps(data))
            with self.assertRaisesRegex(mod.AuditError, "SHA mismatch"):
                mod.audit(gltf, acceptance)

    def test_rejects_inventory_drift(self):
        with tempfile.TemporaryDirectory() as tmp:
            gltf, acceptance = self._write_fixture(Path(tmp))
            data = json.loads(acceptance.read_text())
            data["expect"]["animations"] = ["Attack", "Idle"]
            acceptance.write_text(json.dumps(data))
            with self.assertRaisesRegex(mod.AuditError, "inventory/order mismatch"):
                mod.audit(gltf, acceptance)


if __name__ == "__main__":
    unittest.main()
