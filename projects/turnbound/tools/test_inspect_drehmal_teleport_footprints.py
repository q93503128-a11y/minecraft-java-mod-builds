#!/usr/bin/env python3
"""Narrow, fail-closed tests for the *read-only* copied map footprint inspector."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import inspect_drehmal_teleport_footprints as m


class GeometrySafetyTest(unittest.TestCase):
    def test_airborne_floor_is_not_safe(self):
        self.assertEqual("AIRBORNE_NO_FLOOR",m.classify_space(
            "minecraft:air","minecraft:air","minecraft:air"))
        self.assertEqual("FEET_OR_HEAD_NOT_AIR_COLLISION_UNRESOLVED",
                         m.classify_space("minecraft:stone","minecraft:stone","minecraft:air"))
        self.assertEqual("FLUID_PRESENT_NOT_SAFE",
                         m.classify_space("minecraft:stone","minecraft:water","minecraft:air"))
        self.assertEqual("UNKNOWN_FLOOR_SHAPE_NO_SAFETY_CLAIM",
                         m.classify_space("minecraft:oak_slab","minecraft:air","minecraft:air"))
        self.assertEqual("CONSERVATIVE_FULL_FLOOR_TWO_BLOCK_AIR",
                         m.classify_space("minecraft:stone","minecraft:air","minecraft:air"))

    def test_identifies_below_feet_head_and_above_without_claiming_collision(self):
        states={69:"minecraft:stone",70:"minecraft:air",71:"minecraft:air",72:"minecraft:glass"}
        with patch.object(m,"full_block",side_effect=lambda view,x,y,z:(states[y],{})):
            f=m.footprint(None,(10.5,70.,-2.5))
        self.assertEqual(f["ground"],"minecraft:stone")
        self.assertEqual(f["above_head"],"minecraft:glass")
        self.assertFalse(f["collision_shapes_runtime_verified"])
        self.assertEqual(f["standing_space"],"CONSERVATIVE_FULL_FLOOR_TWO_BLOCK_AIR")

    def test_only_centered_probe_and_finite_positions_allowed(self):
        for xyz in ((10.2,70,2.5),(10.5,70.2,2.5),
                    (float("nan"),70,2.5),(10.5,70,3.0)):
            with self.assertRaises(ValueError):
                m.footprint(None,xyz)

    def test_candidate_search_only_known_full_cube_floor(self):
        def ground(view,p):
            if p==(3.5,70,3.5):
                return {"standing_space":"CONSERVATIVE_FULL_FLOOR_TWO_BLOCK_AIR"}
            return {"standing_space":"AIRBORNE_NO_FLOOR"}
        with patch.object(m,"footprint",side_effect=ground):
            candidates,count=m.conservative_candidates(None,(2.5,70,3.5),
                                                        radius=2,y_range=1)
        self.assertEqual(count,1)
        self.assertEqual(candidates[0]["relative_to_old_target"],[1,0,0])
        self.assertIn("NOT_APPROVED",candidates[0]["candidate_role"])
        with self.assertRaisesRegex(ValueError,"Bounded"):
            m.conservative_candidates(None,(2.5,70,3.5),radius=40)

    def test_refuses_nonisolated_or_activated_world_and_modified_source(self):
        with tempfile.TemporaryDirectory() as temp:
            original=Path(temp)/"original"
            copy=Path(temp)/"copy"
            original.mkdir();copy.mkdir()
            (copy/"turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
            with patch.object(m,"check_official_source",return_value=True):
                with self.assertRaisesRegex(ValueError,"Activated"):
                    m.inspect(original,copy)
            (copy/"turnbound-world.marker").unlink()
            with patch.object(m,"check_official_source",side_effect=ValueError("SHA mismatch")):
                with self.assertRaisesRegex(ValueError,"SHA mismatch"):
                    m.inspect(original,copy)
            with patch.object(m,"check_official_source",return_value=True),patch.object(
                    m,"verify_seven",return_value={"seven_stage_offline_experiment_verified":False}):
                with self.assertRaisesRegex(ValueError,"seven-stage"):
                    m.inspect(original,copy)

if __name__=="__main__":
    unittest.main()
