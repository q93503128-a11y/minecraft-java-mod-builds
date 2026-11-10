#!/usr/bin/env python3
"""Guardrails for protocol inspection of facility callback entrypoints; no map bytes."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import probe_drehmal_facility_callback_runtime as probe


class FacilityCallbackInspectionTest(unittest.TestCase):
    def test_stage_preflight_refuses_activated_or_missing_receipts(self):
        with tempfile.TemporaryDirectory() as tmp:
            w=Path(tmp)
            (w/"turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
            with self.assertRaisesRegex(ValueError,"activated"):
                probe.preflight(w,"seven")
            (w/"turnbound-world.marker").unlink()
            with self.assertRaisesRegex(ValueError,"two hash-pinned"):
                probe.preflight(w,"unverified")
            with patch.object(probe.seven,"verify",return_value={
                "seven_stage_offline_experiment_verified":False
            }):
                with self.assertRaisesRegex(ValueError,"not verified"):
                    probe.preflight(w,"seven")

    def test_requires_exactly_57_disabled_commands_and_zero_scan_warnings(self):
        with tempfile.TemporaryDirectory() as tmp:
            w=Path(tmp)
            for disabled,warnings in ((56,0),(57,1)):
                with patch.object(probe.seven,"verify",return_value={
                    "seven_stage_offline_experiment_verified":True
                }), patch.object(probe.scene,"count_source_commands",return_value=(disabled,warnings)):
                    with self.assertRaisesRegex(ValueError,"all 57"):
                        probe.preflight(w,"seven")
            with patch.object(probe.seven,"verify",return_value={
                "seven_stage_offline_experiment_verified":True
            }), patch.object(probe.scene,"count_source_commands",return_value=(57,0)):
                self.assertEqual(w.resolve(),probe.preflight(w,"seven"))

    def test_eighth_stage_is_also_pinned(self):
        with tempfile.TemporaryDirectory() as tmp:
            w=Path(tmp)
            with patch.object(probe.eight,"verify",return_value={"verified":True,"count":8}):
                with self.assertRaisesRegex(ValueError,"not verified"):
                    probe.preflight(w,"eight")
            with patch.object(probe.eight,"verify",return_value={"verified":True,"count":9}),patch.object(
                probe.scene,"count_source_commands",return_value=(57,0)
            ):
                self.assertEqual(w.resolve(),probe.preflight(w,"eight"))

    def test_scoreboard_change_summary_never_exports_names_values(self):
        before={("original-player","objective"):5,("lost","value"):9}
        after={("original-player","objective"):6,("added","value"):10}
        summary=probe.diff_scores(before,after)
        self.assertEqual({"changed_entries":3,"added_entries":1,"removed_entries":1},summary)
        self.assertNotIn("original-player",str(summary))
        self.assertNotIn("objective",str(summary))

    def test_duplicate_or_malformed_scoreboard_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            w=Path(tmp)
            d=w/"data";d.mkdir()
            (d/"scoreboard.dat").write_bytes(b"stand-in")
            rows=[{"Name":"p","Objective":"o","Score":5},
                  {"Name":"p","Objective":"o","Score":6}]
            with patch.object(probe,"read_gzip_nbt",return_value={"data":{"PlayerScores":rows}}):
                with self.assertRaisesRegex(ValueError,"Duplicate"):
                    probe.scoreboard_values(w)
            with patch.object(probe,"read_gzip_nbt",return_value={"data":{"PlayerScores":[{"Name":"p","Objective":"o","Score":"bad"}]}}):
                with self.assertRaisesRegex(ValueError,"Malformed"):
                    probe.scoreboard_values(w)

    def test_sample_tuning_and_location_are_not_game_start_approval(self):
        self.assertEqual([1.5,-2,0],probe.axes_delta((10.5,70,10),(9,72,10)))
        self.assertEqual({"foundry_lever","godmachine_button","avsal_terminal"},
                         {f for f,_ in probe.FACILITIES})


if __name__=="__main__":
    unittest.main()
