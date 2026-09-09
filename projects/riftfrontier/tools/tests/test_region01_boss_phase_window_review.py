import hashlib
import json
import math
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RECEIPT = ROOT / "assets" / "sources" / "region_01_boss_dragon_evolved.phase_window_review.json"
RUNTIME = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "riftfrontier"
    / "boss_presentation"
    / "region_01"
    / "dragon_evolved.sanitized.v1.gltf"
)
EXPECTED_SHA256 = "ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac"
EXPECTED = {
    "Headbutt": {
        "native_intervals": 45,
        "windows": [
            ("ANTICIPATION", 0, 7),
            ("ACTION", 7, 9),
            ("RECOVERY", 9, 22),
        ],
    },
    "Punch": {
        "native_intervals": 40,
        "windows": [
            ("ANTICIPATION", 0, 8),
            ("ACTION", 8, 11),
            ("RECOVERY", 11, 40),
        ],
    },
}


class Region01BossPhaseWindowReviewTest(unittest.TestCase):
    def setUp(self):
        self.receipt = json.loads(RECEIPT.read_text(encoding="utf-8"))

    def test_receipt_is_pinned_to_exact_accepted_runtime_derivation(self):
        self.assertEqual(
            "riftfrontier:region_01_boss_animation_phase_window_review",
            self.receipt["kind"],
        )
        self.assertEqual(1, self.receipt["schema_version"])
        self.assertEqual(EXPECTED_SHA256, self.receipt["accepted_derivation"]["sha256"])
        self.assertEqual(
            EXPECTED_SHA256,
            hashlib.sha256(RUNTIME.read_bytes()).hexdigest(),
        )
        self.assertEqual(
            "assets/sources/region_01_boss_dragon_evolved.motion_review.json",
            self.receipt["accepted_derivation"]["source_motion_review"],
        )

    def test_reviewed_boundaries_are_exact_native_frame_boundaries_and_contiguous(self):
        for clip_name, expected in EXPECTED.items():
            reviewed = self.receipt["reviewed_clips"][clip_name]
            intervals = reviewed["native_intervals"]
            self.assertEqual(expected["native_intervals"], intervals)
            windows = reviewed["windows"]
            self.assertEqual(len(expected["windows"]), len(windows))
            for actual, (segment, start_frame, end_frame) in zip(windows, expected["windows"]):
                self.assertEqual(segment, actual["segment"])
                self.assertEqual(start_frame, actual["start_frame"])
                self.assertEqual(end_frame, actual["end_frame"])
                self.assertTrue(actual["observed_motion"].strip())
                self.assertTrue(
                    math.isclose(actual["normalized_start"], start_frame / intervals, abs_tol=1e-12)
                )
                self.assertTrue(
                    math.isclose(actual["normalized_end"], end_frame / intervals, abs_tol=1e-12)
                )
            for left, right in zip(windows, windows[1:]):
                self.assertEqual(left["end_frame"], right["start_frame"])
                self.assertTrue(
                    math.isclose(left["normalized_end"], right["normalized_start"], abs_tol=1e-12)
                )

    def test_receipt_does_not_claim_gameplay_damage_timing(self):
        binding = self.receipt["gameplay_binding"]
        self.assertEqual("NOT_AUTHORED", binding["status"])
        scope = self.receipt["method"]["scope"]
        self.assertIn("not a Minecraft damage/hit-window authorization", scope)
        self.assertIn("attack_pattern", binding["note"])


if __name__ == "__main__":
    unittest.main()
