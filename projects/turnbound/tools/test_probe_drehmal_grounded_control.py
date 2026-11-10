#!/usr/bin/env python3
"""No user START approvals from CI-only airborne/supported-floor comparison."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import probe_drehmal_grounded_control as p

class AirborneGroundedComparisonTest(unittest.TestCase):
    def test_distinguishes_supported_location_only_from_shared_rollback(self):
        self.assertEqual("UNSUPPORTED_AIRBORNE_LOCATION_SPECIFIC_ROLLBACK_IN_THIS_PROBE",
                         p.classify(210.345,0))
        self.assertEqual("BOTH_LOCATIONS_ROLL_BACK_INVESTIGATE_SHARED_SERVER_OR_MAP_TRIGGER",
                         p.classify(210.345,190))
        self.assertEqual("NEITHER_LOCATION_ROLLED_BACK_IN_THIS_PROBE",
                         p.classify(0,0))
        self.assertEqual("MIXED_RESULT_NO_CAUSE_CONFIRMED",p.classify(30,6))

    def test_no_nan_or_untrusted_offsets(self):
        with self.assertRaisesRegex(ValueError,"Invalid"):
            p.classify(float("nan"),1)
        with self.assertRaisesRegex(ValueError,"Invalid"):
            p.distance((1,2,float("inf")),(1,2,3))
        self.assertEqual(0,p.distance((1,10,3),(1,2,3)))

    def test_requires_both_official_geometry_constraints(self):
        with tempfile.TemporaryDirectory() as temp:
            a=Path(temp)/"original";b=Path(temp)/"copy";a.mkdir();b.mkdir()
            good=lambda view,p: {
               "standing_space":("AIRBORNE_NO_FLOOR" if p[1]==223
                                 else "CONSERVATIVE_FULL_FLOOR_TWO_BLOCK_PASSABLE"),
               "ground":"minecraft:air" if p[1]==223 else "minecraft:light_gray_concrete",
               "feet":"minecraft:air","head":"minecraft:air"}
            with patch.object(p.old,"probe_preflight",return_value=b),patch.object(
                p.geo,"check_official_source",return_value=True),patch.object(
                p,"World",return_value=object()),patch.object(
                p.geo,"footprint",side_effect=good):
                world,detail=p.verify_sites(a,b)
                self.assertEqual(world,b)
                self.assertEqual(set(detail),{"airborne_legacy_test","conservative_full_floor"})
            with patch.object(p.old,"probe_preflight",return_value=b),patch.object(
                p.geo,"check_official_source",return_value=True),patch.object(
                p,"World",return_value=object()),patch.object(
                p.geo,"footprint",return_value={
                    "standing_space":"AIRBORNE_NO_FLOOR"}):
                with self.assertRaisesRegex(ValueError,"no longer matches"):
                    p.verify_sites(a,b)

    def test_targets_cannot_be_stated_as_game_start(self):
        self.assertEqual(p.SITES["DrehmalAliceCI"][1][1],223)
        self.assertEqual(p.SITES["DrehmalBobCI"][1][1],219)

if __name__=="__main__":unittest.main()
