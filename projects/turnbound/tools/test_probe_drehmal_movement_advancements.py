#!/usr/bin/env python3
"""Offline-mode UUID and advancement snapshot gates for isolated map telemetry."""
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import probe_drehmal_movement_advancements as a

class AdvancementMovementTraceTest(unittest.TestCase):
    def test_offline_uuid_matches_minecraft_1201_name_uuid_algorithm(self):
        self.assertEqual("37ac5511-7010-39c1-8a46-65d9b4f4f59b",
                         a.offline_player_id("DrehmalAliceCI"))

    def test_only_ci_players_awarded_advancements_read(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/"advancements";p.mkdir()
            (p/(a.offline_player_id(a.ACTOR)+".json")).write_text(json.dumps({
              "DataVersion":3465,
              "minecraft:one":{"criteria":{"a":"timestamp"},"done":True},
              "other:unfinished":{"criteria":{},"done":False}}))
            (p/"other-person.json").write_text("{not parseable}")
            self.assertEqual({"minecraft:one"},a.advancement_snapshot(Path(d),a.ACTOR))
            self.assertEqual(set(),a.advancement_snapshot(Path(d),"OtherNewCI"))

    def test_refuses_untrusted_receipts_and_activated_saves(self):
        with tempfile.TemporaryDirectory() as d:
            w=Path(d)
            (w/"turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
            with self.assertRaisesRegex(ValueError,"activated"):
                a.probe_preflight(w)
            (w/"turnbound-world.marker").unlink()
            with patch.object(a.seven,"verify",return_value={
                "seven_stage_offline_experiment_verified":False
            }):
                with self.assertRaisesRegex(ValueError,"Not a pinned"):
                    a.probe_preflight(w)

if __name__=="__main__":unittest.main()
