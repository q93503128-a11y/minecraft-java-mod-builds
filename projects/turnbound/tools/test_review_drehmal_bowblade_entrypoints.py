import unittest
import review_drehmal_bowblade_entrypoints as logic

class OriginalBowbladeEntrypointTests(unittest.TestCase):
    def test_schedule_and_function_edges_distinguished(self):
        functions={"auto":{"calls":["target"]},"target":{"calls":[]},"reward":{"calls":[]}}
        incoming=logic.incoming_graph(functions,[
            {"function_id":"reward","scheduled_target_ids":["target"]}])
        self.assertEqual(incoming["target"],{
            ("auto","function_call"),("reward","schedule_function")})
        roots={"function_ids":set(functions),"automatic":{"auto":["original_tag"]},
               "advancement":{"reward":["data/x/advancements/event.json"]},"command_block":{}}
        found=logic.evidence_paths("target",incoming,roots)
        self.assertEqual(found["possible_root_counts"],
                         {"automatic":1,"advancement":1,"command_block":0})
        self.assertEqual(found["literal_known_roots"]["advancement"][0]["edge_kinds"],
                         ["schedule_function"])
        self.assertFalse(found["original_runtime_execution_proven"])

    def test_command_block_and_absence_not_mistaken_for_runtime_proof(self):
        functions={"root":{"calls":["target"]},"target":{"calls":[]}}
        incoming=logic.incoming_graph(functions,[])
        roots={"function_ids":set(functions),"automatic":{},"advancement":{},
               "command_block":{"root":["original_block"]}}
        got=logic.evidence_paths("target",incoming,roots)
        self.assertEqual(got["possible_root_counts"]["command_block"],1)
        roots["command_block"]={}
        empty=logic.evidence_paths("target",incoming,roots)
        self.assertEqual(empty["possible_root_counts"],
                         {"automatic":0,"advancement":0,"command_block":0})
        self.assertTrue(empty["unknown_dynamic_execution_excluded"])
        self.assertFalse(empty["original_runtime_execution_proven"])

    def test_cycle_does_not_inflate_entrypoint_count(self):
        functions={"a":{"calls":["b"]},"b":{"calls":["a","goal"]},"goal":{"calls":[]}}
        roots={"function_ids":set(functions),"automatic":{"a":["tag"]},
               "advancement":{},"command_block":{}}
        got=logic.evidence_paths("goal",logic.incoming_graph(functions,[]),roots)
        self.assertEqual(got["reverse_reachable_function_count"],3)
        self.assertEqual(got["literal_known_roots"]["automatic"][0]["static_function_path"],
                         ["a","b","goal"])

    def test_unknown_sources_are_rejected(self):
        with self.assertRaisesRegex(ValueError,"absent"):
            logic.evidence_paths("not_found",{},{
                "function_ids":set(),"automatic":{},"advancement":{},"command_block":{}})
        with self.assertRaisesRegex(ValueError,"provenance"):
            logic.review({}, {}, {})
        self.assertEqual(len(logic.BOW_FUNCTIONS),5)
        self.assertEqual(len(logic.FACILITY_ADVANCEMENTS),3)

if __name__=="__main__":
    unittest.main()
