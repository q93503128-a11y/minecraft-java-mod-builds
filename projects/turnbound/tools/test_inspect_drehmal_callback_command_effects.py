import io
import tempfile
import unittest
from pathlib import Path

import inspect_drehmal_callback_command_effects as audit


class CallbackCommandEffectTests(unittest.TestCase):
    def test_structural_puzzle_and_player_effects_are_distinct(self):
        sample=(b"# no command\n"
                b"execute as @p at @s run setblock 1 2 3 minecraft:redstone_block\n"
                b"execute if score @s bowbladEffect matches 1 run tellraw @s {\"text\":\"hidden\"}\n"
                b"scoreboard players set @s bowbladEffect 1\n"
                b"give @s minecraft:bread\n")
        result=audit.summarize_function(sample)
        self.assertEqual(result["literal_command_line_count"],4)
        self.assertEqual(result["effective_operation_counts"]["setblock"],1)
        self.assertEqual(result["effect_family_counts"]["WORLD_BLOCK_OR_STORAGE"],1)
        self.assertEqual(result["effect_family_counts"]["PLAYER_REWARD_OR_STATE"],1)
        self.assertEqual(result["effect_family_counts"]["SCORE_OR_GLOBAL_STATE"],1)
        self.assertEqual(result["effect_family_counts"]["VISUAL_OR_FEEDBACK"],1)
        self.assertIn("score_write",result["bowblade_objective_role_counts"])
        self.assertIn("score_condition",result["bowblade_objective_role_counts"])
        self.assertNotIn("hidden",str(result))

    def test_unknown_macros_and_incomplete_execute_not_assumed_safe(self):
        self.assertEqual(audit.operation("$test macro"),"UNRESOLVED_DYNAMIC_COMMAND")
        self.assertEqual(audit.operation("execute as @s"),"EXECUTE_WITHOUT_LITERAL_RUN")
        self.assertEqual(audit.family("UNRECOGNIZED_OR_UNSUPPORTED_COMMAND"),"UNKNOWN_OR_EXECUTE_ONLY")
        self.assertIsNone(audit.operation("# ignored"))

    def test_static_schedule_closure_is_not_live_execution_claim(self):
        graph={
            "a":{"calls":["b"]},
            "b":{"calls":[]},
            "c":{"calls":[]},
        }
        sources={"b":{"c"}}
        found,missing=audit.closure(["a"],graph,sources)
        self.assertEqual(found,{"a","b","c"})
        self.assertEqual(missing,set())

    def test_invalid_original_report_fails_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/"graph.json"
            path.write_text("{}")
            with self.assertRaisesRegex(ValueError,"SHA"):
                audit.read_pinned(path,audit.GRAPH_SHA)

    def test_targets_are_only_nine_plus_three_reviewed_structures(self):
        from preview_drehmal_nonweapon_callbacks import TARGET_NAMES
        self.assertEqual(len(TARGET_NAMES|audit.EXTRA),12)
        self.assertNotIn("data/players/advancements/terms/avsal.json",TARGET_NAMES)
        self.assertEqual(len(audit.CROSS_SHA),64)


if __name__=="__main__":
    unittest.main()
