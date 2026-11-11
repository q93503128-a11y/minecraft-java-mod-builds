#!/usr/bin/env python3
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
import json
import probe_drehmal_foundry_advancement_score as m

class FoundryAdvancementScoreTraceUnitTests(unittest.TestCase):
    def test_parse_three_conditions_without_exposing_identifiers(self):
        text="\n".join("execute if score #private private_objective matches "+str(n)+" run function foundry:levers/door" for n in (1,2,3))
        clauses=m.parse_main_clauses(text)
        self.assertEqual([x["lower"] for x in clauses],[1,2,3])
        observed=m.evaluate_main_scores({("#private","private_objective"):2},clauses)
        self.assertEqual([x["would_match_in_assumed_context"] for x in observed],[False,True,False])
        self.assertNotIn("private_objective",json.dumps(observed))
    def test_gate_proximity_is_a_separate_natural_event(self):
        self.assertEqual(m.GATE_PRESENCE_CENTER,(-3276,159,1570))
        self.assertEqual(m.GATE_PRESENCE_MAX_RADIUS,12)
    def test_wrong_conditions_refused(self):
        with self.assertRaises(ValueError):
            m.parse_main_clauses("execute if score #a private matches 3 run function foundry:levers/door")
    def test_offline_advancement_reader(self):
        with TemporaryDirectory() as d:
            world=Path(d)
            target=m.advancement_file(world)
            self.assertIn("advancements",str(target))
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(json.dumps({"foundry:1":{"done":True,"criteria":{"criterion":"2026-01-01 00:00:00 +0000"}}}))
            state=m.advancement_progress(world)
            self.assertTrue(state["progress"]["foundry:1"]["completed"])
            self.assertFalse(state["progress"]["foundry:2"]["completed"])
            self.assertTrue(state["advancement_file_observed"])
    def test_score_selector_context_is_not_claimed_proven(self):
        clauses=m.parse_main_clauses("\n".join("execute if score @s private matches "+str(n)+" run function foundry:levers/door" for n in (1,2,3)))
        result=m.evaluate_main_scores({("DrehmalAliceCI","private"):3},clauses)
        self.assertEqual(result[2]["resolution"],"player_execution_context_hypothesis")
        self.assertTrue(result[2]["would_match_in_assumed_context"])

if __name__=="__main__":
    unittest.main()
