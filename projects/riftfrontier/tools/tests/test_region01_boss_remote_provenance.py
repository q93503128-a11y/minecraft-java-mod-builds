from __future__ import annotations

import json
import unittest
from pathlib import Path


EXPECTED_SOURCE_SHA256 = "39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c"
EXPECTED_SANITIZED_SHA256 = "ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac"
EXPECTED_PROVENANCE_SHA256 = "3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c"
EXPECTED_CLIPS = [
    ("Death", 0.6666666865348816),
    ("Fast_Flying", 0.8333333134651184),
    ("Flying_Idle", 1.5),
    ("Headbutt", 1.5),
    ("HitReact", 0.6666666865348816),
    ("No", 1.1666666269302368),
    ("Punch", 1.3333333730697632),
    ("Yes", 1.1666666269302368),
]


class Region01BossRemoteProvenanceTest(unittest.TestCase):
    """Offline regression gate for the independently reproduced Dragon Evolved receipt.

    Network reacquisition was a one-shot evidence operation. CI now validates the committed receipt against the
    canonical source/acceptance contracts so routine builds stay deterministic and do not trust mutable network state.
    """

    def test_committed_reacquisition_receipt_matches_canonical_contracts(self) -> None:
        project_root = Path(__file__).resolve().parents[2]
        source_contract = json.loads(
            (project_root / "assets/sources/region_01_boss_dragon_evolved.source.json").read_text(encoding="utf-8")
        )
        acceptance = json.loads(
            (project_root / "assets/sources/region_01_boss_dragon_evolved.acceptance.json").read_text(encoding="utf-8")
        )
        receipt = json.loads(
            (project_root / "assets/sources/region_01_boss_dragon_evolved.animation_audit.json").read_text(encoding="utf-8")
        )

        self.assertEqual("riftfrontier:region_01_boss_animation_audit_receipt", receipt["kind"])
        self.assertEqual(1, receipt["schema_version"])
        self.assertEqual(EXPECTED_SOURCE_SHA256, source_contract["source"]["sha256"])
        self.assertEqual(EXPECTED_SOURCE_SHA256, receipt["derivation"]["source_sha256"])
        self.assertEqual(EXPECTED_SOURCE_SHA256, receipt["source_reacquisition"]["accepted_mirror"]["sha256"])

        self.assertEqual(EXPECTED_SANITIZED_SHA256, acceptance["output"]["geometry_sha256"])
        self.assertEqual(EXPECTED_SANITIZED_SHA256, receipt["derivation"]["sanitized_sha256"])
        self.assertEqual(acceptance["output"]["geometry_bytes"], receipt["derivation"]["sanitized_byte_size"])
        self.assertEqual(EXPECTED_PROVENANCE_SHA256, acceptance["output"]["provenance_sha256"])
        self.assertEqual(EXPECTED_PROVENANCE_SHA256, receipt["derivation"]["provenance_sha256"])

        clips = receipt["clips"]
        self.assertEqual(source_contract["expect"]["required_animations"], [clip["name"] for clip in clips])
        self.assertEqual(EXPECTED_CLIPS, [(clip["name"], clip["duration_seconds"]) for clip in clips])
        for clip in clips:
            self.assertEqual(90, clip["channel_count"])
            self.assertEqual(90, clip["sampler_count"])
            self.assertEqual(45, clip["animated_node_count"])
            self.assertEqual({"rotation": 45, "translation": 45}, clip["target_path_counts"])
            self.assertEqual({"LINEAR": 90}, clip["interpolation_counts"])

        accepted = receipt["source_reacquisition"]["accepted_mirror"]
        self.assertEqual("laoniutoushx/TD-demo-2024-04-03", accepted["repository"])
        self.assertEqual("87051774343f2a0df215639e8674178437228b71", accepted["commit"])
        self.assertTrue(accepted["path"].endswith("Dragon_Evolved.gltf"))
        rejected = receipt["source_reacquisition"]["rejected_mirrors"]
        self.assertGreaterEqual(len(rejected), 1)
        self.assertNotEqual(EXPECTED_SOURCE_SHA256, rejected[0]["observed_sha256"])

        self.assertEqual("NOT_TESTED", receipt["motion_semantics"]["status"])


if __name__ == "__main__":
    unittest.main()
