from __future__ import annotations

import hashlib
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESOURCE = ROOT / "src/main/resources/assets/riftfrontier/boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf"
EXPECTED_SHA256 = "ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac"
EXPECTED_SIZE = 681_773
EXPECTED_CLIPS = ["Death", "Fast_Flying", "Flying_Idle", "Headbutt", "HitReact", "No", "Punch", "Yes"]


class Region01BossRuntimeResourceTest(unittest.TestCase):
    def test_vendored_resource_matches_accepted_derivation_exactly(self) -> None:
        payload = RESOURCE.read_bytes()
        self.assertEqual(EXPECTED_SIZE, len(payload))
        self.assertEqual(EXPECTED_SHA256, hashlib.sha256(payload).hexdigest())

    def test_vendored_resource_remains_art_neutral_and_preserves_source_clips(self) -> None:
        document = json.loads(RESOURCE.read_text(encoding="utf-8"))
        for forbidden in ("materials", "textures", "images", "samplers"):
            self.assertNotIn(forbidden, document)
        for mesh in document.get("meshes", []):
            for primitive in mesh.get("primitives", []):
                self.assertNotIn("material", primitive)
        self.assertEqual(EXPECTED_CLIPS, [clip.get("name") for clip in document.get("animations", [])])


if __name__ == "__main__":
    unittest.main()
