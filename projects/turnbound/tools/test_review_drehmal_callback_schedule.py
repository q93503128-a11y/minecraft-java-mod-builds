import importlib.util
import unittest
from pathlib import Path
p=Path(__file__).with_name("review_drehmal_callback_schedule.py")
spec=importlib.util.spec_from_file_location("triage",p)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
class CallbackScheduleTests(unittest.TestCase):
    def test_graph_cycle_and_unresolved_references(self):
        g={"a":{"calls":["b"]},"b":{"calls":["a","missing"]}}
        self.assertEqual(m.reach(g,["a"]),{"a","b"})
    def test_schedule_loop_components(self):
        self.assertEqual(m.cycle_components({"a":{"b"},"b":{"a","c"},"c":{"c"},"d":{"e"}}),[["a","b"],["c"]])
    def test_structure_advancement_guard(self):
        self.assertEqual(m.group({"file":"data/foundry/advancements/2.json","reward_function_id":"foundry:levers/2_check"},{"categories":["progression"]}),"STRUCTURE_OR_PUZZLE_PRESERVE_REVIEW")
    def test_world_edit_is_never_declared_safe(self):
        self.assertEqual(m.group({"file":"data/players/advancements/test.json","reward_function_id":"players:misc/test"},{"categories":["world_edit"]}),"WORLD_STATE_OR_TRAVEL_REVIEW")
    def test_missing_callback_is_explicit(self):
        self.assertEqual(m.group({"file":"data/players/advancements/test.json","reward_function_id":"players:null"},None),"MISSING_FUNCTION_REVIEW")
    def test_source_identity_fail_closed(self):
        with self.assertRaisesRegex(ValueError,"Unverified"):
            m.analyze({"map_release":"wrong"},{"map_release":m.RELEASE})
if __name__=="__main__": unittest.main()
