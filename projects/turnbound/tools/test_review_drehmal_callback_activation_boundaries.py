import unittest
import review_drehmal_callback_activation_boundaries as matrix

class OriginalCallbackActivationMatrixTests(unittest.TestCase):
    def test_combined_scheduler_reachability_and_cycles(self):
        funcs={"a","b","c","d","e"}
        calls={"a":{"b"},"b":{"a"},"c":set(),"d":{"unknown"},"e":set()}
        r,u=matrix.traverse(["a"],calls,funcs)
        self.assertEqual(r,{"a","b"})
        self.assertFalse(u)
        s={"a":{"b"},"b":{"a","c"},"c":{"d"},"d":{"unknown"},"e":set()}
        r,u=matrix.traverse(["a"],s,funcs)
        self.assertEqual(r,{"a","b","c","d"})
        self.assertEqual(u,{"unknown"})

    def test_schedule_sources_not_inflated_by_graph_merge(self):
        from collections import defaultdict
        schedule=defaultdict(set)
        schedule["a"].add("b")
        combined=matrix.merge_schedule_edges({"a":set(),"b":set(),"c":set()},schedule)
        self.assertEqual(set(schedule),{"a"})
        self.assertEqual(combined["a"],{"b"})
        self.assertEqual(combined["b"],set())
        self.assertEqual(combined["c"],set())

    def test_location_impossible_and_structure_preservation(self):
        row={"trigger_types":["minecraft:location"],"review_group":"PLAYER_REWARD_OR_STATE_REVIEW",
             "direct_function_callers":[],"reachable_from_original_auto_roots":False,
             "has_schedule_in_static_callgraph":False,"callback_exists":True,
             "advancement_file":"data/test/advancements/example.json"}
        priority,reasons=matrix.tag(row,set(),0,0)
        self.assertEqual(priority,"PRESERVE_UNTIL_MECHANISM_REVIEW")
        self.assertIn("LOCATION_OR_DIMENSION_ACTIVATION",reasons)
        row["trigger_types"]=["minecraft:impossible"]
        _,reasons=matrix.tag(row,set(),0,0)
        self.assertIn("EXPLICIT_GRANT_OR_OTHER_UNOBSERVED_ACTIVATION",reasons)
        row["trigger_types"]=["minecraft:consume_item"]
        row["review_group"]="STRUCTURE_OR_PUZZLE_PRESERVE_REVIEW"
        _,reasons=matrix.tag(row,set(),0,0)
        self.assertIn("STRUCTURE_OR_TRAVEL_CLASSIFIED",reasons)

    def test_eighth_preview_is_not_playable_approval(self):
        row={"trigger_types":["minecraft:consume_item"],"review_group":"PLAYER_REWARD_OR_STATE_REVIEW",
             "direct_function_callers":[],"reachable_from_original_auto_roots":False,
             "has_schedule_in_static_callgraph":False,"callback_exists":True,
             "advancement_file":next(iter(matrix.eight.TARGET_NAMES))}
        priority,reasons=matrix.tag(row,set(),0,0)
        self.assertEqual(priority,"ALREADY_EIGHTH_OFFLINE_PREVIEW_NOT_PLAYABLE")
        self.assertFalse(reasons)
        priority,reasons=matrix.tag(row,{"teleport"},0,0)
        self.assertEqual(priority,"PRESERVE_UNTIL_MECHANISM_REVIEW")
        self.assertIn("FUNCTION_TELEPORT",reasons)

    def test_invalid_source_rejected_before_classification(self):
        with self.assertRaisesRegex(ValueError,"evidence identity"):
            matrix.analyze({}, {}, {}, {})

    def test_invalid_source_hash_refused(self):
        with self.assertRaises((OSError,ValueError)):
            matrix.read_pinned("/not/a/verified/drehmal/report.json","0"*64)

if __name__=="__main__":
    unittest.main()
