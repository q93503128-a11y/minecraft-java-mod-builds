#!/usr/bin/env python3
"""Narrow fail-closed tests for actual copied-map protocol client probe."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import probe_drehmal_copy_player_presence as probe


class CopyWorldPlayerPresenceGateTest(unittest.TestCase):
    def test_rejects_activated_world_before_querying_map(self):
        with tempfile.TemporaryDirectory() as d:
            world=Path(d)
            (world/"turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
            with self.assertRaisesRegex(ValueError,"activated"):
                probe.preflight(world,"seven")

    def test_rejects_unapproved_stage_name(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaisesRegex(ValueError,"two hash-pinned"):
                probe.preflight(Path(d),"ninth")

    def test_seven_requires_exact_verified_receipt_and_all_commands_empty(self):
        with tempfile.TemporaryDirectory() as d:
            with patch.object(probe.seven,"verify",return_value={
                "seven_stage_offline_experiment_verified":False
            }):
                with self.assertRaisesRegex(ValueError,"not verified"):
                    probe.preflight(Path(d),"seven")
            with patch.object(probe.seven,"verify",return_value={
                "seven_stage_offline_experiment_verified":True
            }),patch.object(probe.scene,"count_source_commands",return_value=(56,0)):
                with self.assertRaisesRegex(ValueError,"all 57"):
                    probe.preflight(Path(d),"seven")
            with patch.object(probe.seven,"verify",return_value={
                "seven_stage_offline_experiment_verified":True
            }),patch.object(probe.scene,"count_source_commands",return_value=(57,0)):
                self.assertEqual(Path(d).resolve(),probe.preflight(Path(d),"seven"))

    def test_eight_requires_all_receipts_and_clean_command_scan(self):
        with tempfile.TemporaryDirectory() as d:
            with patch.object(probe.eight,"verify",return_value={
                "verified":True,"count":8
            }):
                with self.assertRaisesRegex(ValueError,"not verified"):
                    probe.preflight(Path(d),"eight")
            with patch.object(probe.eight,"verify",return_value={
                "verified":True,"count":9
            }),patch.object(probe.scene,"count_source_commands",return_value=(57,1)):
                with self.assertRaisesRegex(ValueError,"all 57"):
                    probe.preflight(Path(d),"eight")

    def test_ci_observation_position_utility(self):
        self.assertTrue(probe.near((1,2,3),(1.5,2,3)))
        self.assertFalse(probe.near((1,2,3),(4,2,3)))
        self.assertNotEqual(probe.digest_text("a"),probe.digest_text("b"))


if __name__=="__main__":
    unittest.main()
