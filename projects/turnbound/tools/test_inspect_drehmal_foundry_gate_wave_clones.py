import json
import tempfile
import unittest
import zipfile
from pathlib import Path
import inspect_drehmal_foundry_gate_wave_paths as mod

class NarrowFoundryCallerAndCloneTest(unittest.TestCase):
 def test_clone_exact_destination_is_not_a_door_approval(self):
  line="execute as @a run clone -3278 150 1490 -3276 154 1492 -3260 151 1495"
  r=mod.clone_coordinates("foundry:open/3",line)[0]
  self.assertEqual(r["dest_min"],[-3260,151,1495])
  self.assertEqual(r["dest_max"],[-3258,155,1497])
  self.assertTrue(r["execute_position_context_unresolved"])
  self.assertEqual(mod.clone_coordinates("foundry:open/3","clone ~ ~ ~ ~ ~ ~ ~ ~ ~"),[])
 def test_traces_direct_callers_from_other_namespaces(self):
  with tempfile.TemporaryDirectory() as d:
   path=Path(d)/"test.zip"
   with zipfile.ZipFile(path,"w") as z:
    z.writestr("data/foundry/functions/open/3.mcfunction","function foundry:arena_start")
    z.writestr("data/foundry/functions/arena_start.mcfunction",
      "execute run clone 1 2 3 2 3 4 -3275 151 1500")
    z.writestr("data/core/functions/main.mcfunction","function foundry:wave_logic")
    z.writestr("data/foundry/functions/wave_logic.mcfunction","function foundry:wave_1")
   out=mod.focused_path_inventory(path)
   self.assertEqual(out["all_namespaces_literal_callers_to_focused_functions"]["foundry:wave_logic"],["core:main"])
   self.assertEqual(out["all_namespaces_literal_callers_to_focused_functions"]["foundry:arena_start"],["foundry:open/3"])
   self.assertEqual(out["literal_clone_operations_found"],1)
   self.assertFalse(out["runtime_invocation_verified"])
if __name__=="__main__":unittest.main()
