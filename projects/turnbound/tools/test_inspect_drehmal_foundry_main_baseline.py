import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
import inspect_drehmal_foundry_main_baseline as m

class FakeWorld:
    def __init__(self,states):self.states=states
    def chunk(self,x,z):
        # Instead of mocking a whole 1.20.1 NBT palette, patch state directly.
        raise AssertionError("state should have been mocked")

class GateBaselineTests(unittest.TestCase):
    def test_90_cells_and_source_vs_dest_difference(self):
        def fake_state(view,at):
            if at[0]>=m.GATE_DST[0] and at[0]<m.GATE_DST[0]+5 and at[2]>=m.GATE_DST[2]:
                return ("minecraft:bedrock",())
            return ("minecraft:air",())
        with patch.object(m,"World",return_value=FakeWorld({})),patch.object(m,"state",side_effect=fake_state):
            data=m.source_destination_comparison("/unused")
        self.assertEqual(data["count"],90)
        self.assertEqual(data["same_full_blockstate_before_clone"],0)
        self.assertEqual(data["source_destination_fullstate_differences"],90)
        self.assertFalse(data["runtime_clone_executed"])
    def test_condition_classifier_does_not_expose_original_objectives(self):
        cmd="execute if score @s OriginalPrivateObjective matches 3..5 run function foundry:levers/door"
        data=m.classify_conditional(cmd)
        self.assertEqual(data["conditional_clauses"][0]["matches_integer_range"],["3","5"])
        self.assertEqual(data["function_reference_ids"],["foundry:levers/door"])
        self.assertNotIn("OriginalPrivateObjective",json.dumps(data))
    def test_redacted_main_score_gate_and_reverse_door_callers(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/"fixture.zip"
            with zipfile.ZipFile(p,"w") as z:
                z.writestr("data/foundry/functions/levers/main.mcfunction",
                    "\n".join("execute if score #private hidden_score matches "+str(i)+
                              " run scoreboard players add #private hidden_score 1" for i in (1,2,3)))
                z.writestr("data/foundry/functions/levers/1_check.mcfunction",
                           "advancement revoke @s only foundry:1")
                z.writestr("data/foundry/functions/levers/1.mcfunction",
                           "scoreboard players set #private hidden_score 1")
                z.writestr("data/foundry/functions/triggers.mcfunction",
                           "schedule function foundry:levers/door 2t")
            result=m.trace_foundry_door_callers(p)
            self.assertEqual(result["main_condition_distinct_score_keys"],1)
            self.assertEqual(result["door_literal_callers"]["foundry:levers/door"],["foundry:triggers"])
            self.assertEqual(result["check_advancement_actions"]["foundry:levers/1_check"],["revoke"])
            self.assertNotIn("hidden_score",json.dumps(result))
    def test_overworld_door_dispatch_score_key_is_redacted(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/"fixture.zip"
            with zipfile.ZipFile(p,"w") as z:
                z.writestr("data/players/functions/overworld_p.mcfunction",
                    "execute if score #private hidden_score matches 3 if dimension minecraft:overworld run function foundry:levers/door")
                z.writestr("data/foundry/functions/levers/main.mcfunction",
                    "execute if score #private hidden_score matches 3 run tellraw @a hello")
                z.writestr("data/players/functions/tick.mcfunction","function players:overworld_p")
                z.writestr("data/minecraft/tags/functions/tick.json",
                    '{"values":["players:tick"]}')
            result=m.trace_overworld_door_dispatch(p)
            self.assertTrue(result["door_call_sites"][0]["score_key_same_as_main"])
            self.assertTrue(result["door_call_sites"][0]["has_dimension_guard"])
            self.assertEqual(result["literal_function_callers_of_entry"],["players:tick"])
            self.assertFalse(result["minecraft_tick_and_load_tags"][0]["contains_overworld_controller"])
            self.assertNotIn("hidden_score",json.dumps(result))
    def test_foundry_overworld_door_entity_selector_metadata(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/"original.zip"
            with zipfile.ZipFile(path,"w") as z:
                z.writestr("data/players/functions/overworld_p.mcfunction",
                    "execute if score #private private_obj matches 3 if entity @a[x=-3276,y=160,z=1576,distance=..6,tag=Foundry] run function foundry:levers/door")
                z.writestr("data/players/functions/overworld.mcfunction",
                    "function players:overworld_p")
                z.writestr("data/players/functions/master.mcfunction",
                    "function players:overworld")
                z.writestr("data/minecraft/tags/functions/tick.json",
                    '{"values":["players:master"]}')
            r=m.trace_overworld_entity_guard(path)
            guard=r["door_entity_guards"][0]["selectors"][0]
            self.assertEqual(guard["selector_kind"],"@a")
            self.assertEqual(guard["literal_coordinates"],[-3276.0,160.0,1576.0])
            self.assertEqual(guard["distance_bounds"],"..6")
            self.assertTrue(guard["has_tag_filter"])
            self.assertEqual(r["reverse_function_callers"]["players:overworld"],["players:master"])
            self.assertNotIn("private_obj",json.dumps(r))
            self.assertNotIn("Foundry",json.dumps(r))
    def test_function_inventory_fails_closed_on_missing_pinned_member(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/"original.zip"
            with zipfile.ZipFile(path,"w") as z:z.writestr("data/foundry/functions/main.mcfunction","say original")
            with self.assertRaisesRegex(ValueError,"Missing original Foundry function"):
                m.function_inventory(path)
if __name__=="__main__":unittest.main()
