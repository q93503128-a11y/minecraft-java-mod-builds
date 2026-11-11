import tempfile,unittest,zipfile,json
from pathlib import Path
import inspect_drehmal_foundry_lever_predicates as mod
class FoundryPredicateTests(unittest.TestCase):
 def test_structured_predicate_paths_only(self):
  condition={"location":{"block":{"blocks":["minecraft:lever"],"state":{"powered":"true"}}},
             "item":{"items":["minecraft:stick"]}}
  result=mod.predicate_summary(condition)
  self.assertIn("location.block.blocks",result["condition_keypaths"])
  self.assertIn("minecraft:lever",result["literal_resource_ids"])
  self.assertNotIn("powered=true",str(result))
 def test_exact_three_advancements_and_function_independence(self):
  self.assertEqual(len(mod.IDS),3)
  self.assertIn("foundry:levers/door",mod.FUNCTION_IDS)
if __name__=="__main__":unittest.main()
