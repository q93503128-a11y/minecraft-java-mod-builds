import unittest
import probe_drehmal_foundry_held_item as mod
class HeldItemDiffUnitTests(unittest.TestCase):
 def test_score_diff_exposes_count_not_original_names_or_values(self):
  before={("private","objective"):1,("old","objective"):2}
  after={("private","objective"):3,("new","score"):5}
  self.assertEqual(mod.changed_score_entries(before,after),3)
 def test_gate_target_is_original_source_clone_target(self):
  self.assertEqual(mod.GATE_HINT,(-3276,160,1576))
  self.assertLessEqual(mod.GATE_XZ_RADIUS,8)
if __name__=="__main__":unittest.main()
