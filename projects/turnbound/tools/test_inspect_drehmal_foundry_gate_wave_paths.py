import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
import inspect_drehmal_foundry_gate_wave_paths as src
class FoundryOriginalSourceInspectionTest(unittest.TestCase):
 def test_parses_only_literal_coordinates_not_unsupported_relative_contexts(self):
  line="execute as @a run fill -3300 150 1480 -3299 155 1481 minecraft:air"
  rows=src.edit_coordinates("foundry:gate",line)
  self.assertEqual(len(rows),1)
  self.assertTrue(rows[0]["execute_position_context_unresolved"])
  self.assertTrue(src.near_foundry(rows[0]))
  self.assertEqual(src.edit_coordinates("foundry:other","setblock ~ ~ ~ minecraft:air"),[])
 def test_wave_callers_and_advancements_are_separate_from_execution(self):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d)/"source.zip"
   with zipfile.ZipFile(p,"w") as z:
    z.writestr("data/foundry/functions/arena_start.mcfunction",
      "function foundry:wave_1\nexecute run fill -3276 151 1500 -3273 154 1500 minecraft:air")
    z.writestr("data/foundry/functions/wave_1.mcfunction","summon minecraft:zombie")
    z.writestr("data/foundry/advancements/arena.json",
      json.dumps({"criteria":{"a":{"trigger":"minecraft:location"}},
                  "rewards":{"function":"foundry:arena_start"}}))
   got=src.inspect_datapack(p)
   self.assertEqual(got["named_wave_entry_reverse_callers"]["foundry:wave_1"],
       ["foundry:arena_start"])
   self.assertIn("advancement:foundry:arena",
       got["named_wave_entry_reverse_callers"]["foundry:arena_start"])
   self.assertTrue(got["foundry_area_literal_edit_candidate_count"])
   self.assertFalse(got["wave_path_causally_verified"])
 def test_no_full_map_or_wave_safety_from_static_analysis(self):
  self.assertTrue(src.LEVERS)
  self.assertEqual(src.GATE_HINT,(-3276,157,1500))
if __name__=="__main__":unittest.main()
