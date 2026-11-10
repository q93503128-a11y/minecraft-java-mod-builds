#!/usr/bin/env python3
"""Safe movement-attribution behavior: never treat CI points as an approved START."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import probe_drehmal_movement_attribution as m

class MovementAttributionGateTest(unittest.TestCase):
    def test_stage_guard_rejects_active_game_and_unverified_copies(self):
        with tempfile.TemporaryDirectory() as tmp:
            w=Path(tmp)
            (w/"turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
            with self.assertRaisesRegex(ValueError,"activated"):
                m.probe_preflight(w)
            (w/"turnbound-world.marker").unlink()
            with patch.object(m.seven,"verify",return_value={"seven_stage_offline_experiment_verified":False}):
                with self.assertRaisesRegex(ValueError,"Not a pinned"):
                    m.probe_preflight(w)
            with patch.object(m.seven,"verify",return_value={"seven_stage_offline_experiment_verified":True}),patch.object(
                m.scene,"count_source_commands",return_value=(56,0)):
                with self.assertRaisesRegex(ValueError,"57"):
                    m.probe_preflight(w)
            with patch.object(m.seven,"verify",return_value={"seven_stage_offline_experiment_verified":True}),patch.object(
                m.scene,"count_source_commands",return_value=(57,0)):
                self.assertEqual(w.resolve(),m.probe_preflight(w))

    def test_command_block_differential_classifies_only_confirmed_evidence(self):
        self.assertEqual("DISPLACEMENT_DEPENDS_ON_COMMAND_BLOCK_EXECUTION_IN_THIS_PROBE",
                         m.classify(120,0.5))
        self.assertEqual("DISPLACEMENT_PERSISTS_WITH_COMMAND_BLOCKS_DISABLED",
                         m.classify(120,100))
        self.assertEqual("OFF_ONLY_DISPLACEMENT_UNEXPECTED_REQUIRES_REPRODUCTION",
                         m.classify(0,100))
        self.assertEqual("NO_LARGE_HORIZONTAL_DISPLACEMENT_REPRODUCED",
                         m.classify(1,2))

    def test_nonfinite_positions_are_rejected(self):
        with self.assertRaisesRegex(ValueError,"Invalid"):
            m.location_difference((1,float("nan"),3),(0,0,0))
        with self.assertRaisesRegex(ValueError,"Invalid"):
            m.classify(float("inf"),0)
        with self.assertRaisesRegex(ValueError,"Invalid"):
            m.classify(10,-1)

    def test_horizontal_distance_separated_from_gravity(self):
        delta=m.location_difference((26520.5,177,-84.5),m.TARGET)
        self.assertEqual(62,delta["delta_x"])
        self.assertEqual(-46,delta["delta_y"])
        self.assertEqual(201,delta["delta_z"])
        self.assertGreater(delta["horizontal"],200)
        self.assertEqual(0,m.location_difference((m.TARGET[0],m.TARGET[1]-40,m.TARGET[2]),m.TARGET)["horizontal"])

    def test_existing_server_directory_must_be_disposable(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)
            (p/"run").mkdir()
            with self.assertRaisesRegex(ValueError,"pre-existing"):
                m.configure_server(p,True)

if __name__=="__main__":
    unittest.main()
