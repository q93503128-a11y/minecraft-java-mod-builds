#!/usr/bin/env python3
"""Fail-closed read-only facility hint scanner tests (not a playtest)."""
import unittest
import preview_drehmal_facility_surface_inventory as probe


class FakeWorld:
    def __init__(self, blocks): self.blocks=blocks
    def state(self, pos): return {"id":self.blocks.get(pos,"minecraft:air")}


class FacilitySurfaceTests(unittest.TestCase):
    def test_wiki_anchor_is_hint_not_approved_click(self):
        site=probe.inventory(
            FakeWorld({
                (1,0,1):"minecraft:lever",
                (3,0,2):"minecraft:lever",
                (0,0,0):"minecraft:redstone_wire",
            }), "demo",(0,0,0),3,2,"lever")
        self.assertEqual(site["candidate_total"],2)
        self.assertEqual(site["nearby_candidates"][0]["position"],[1,0,1])
        self.assertEqual(site["nearby_candidates"][0]["block_id"],"minecraft:lever")
        self.assertFalse(site["exact_click_target_confirmed"])
        self.assertFalse(site["real_physical_interaction_tested"])

    def test_no_control_must_remain_unverified(self):
        output=probe.inventory(FakeWorld({}),"absent",(0,0,0),1,1,"button")
        self.assertEqual(output["candidate_total"],0)
        self.assertEqual(output["nearby_candidates"],[])
        self.assertFalse(output["exact_click_target_confirmed"])

    def test_wrong_feature_and_scope_bounded(self):
        output=probe.inventory(
            FakeWorld({(1,0,0):"minecraft:iron_door",
                       (0,0,1):"minecraft:lever"}),
            "door",(0,0,0),2,1,"door_and_signal")
        self.assertEqual(output["candidate_total"],2)
        self.assertEqual(output["control_counts"]["door"],1)
        with self.assertRaises(ValueError):
            probe.inventory(FakeWorld({}),"too_large",(0,0,0),99,3,"button")

    def test_pinned_source_hints_and_classification(self):
        self.assertEqual(len(probe.SITES),6)
        self.assertEqual(probe.category("minecraft:oak_button"),"button")
        self.assertEqual(probe.category("minecraft:lever"),"lever")
        self.assertIsNone(probe.category("minecraft:stone"))


if __name__=="__main__":
    unittest.main()
