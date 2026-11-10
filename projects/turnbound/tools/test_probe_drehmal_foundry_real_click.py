import unittest
from unittest.mock import patch
import probe_drehmal_foundry_real_click as mod
class FoundryGeometryTest(unittest.TestCase):
 def test_exact_three_source_pinned_facility_levers(self):
  self.assertEqual(len(mod.LEVERS),3)
  self.assertEqual(len(set(mod.LEVERS.values())),3)
 def test_refuse_missing_solid_stand_position(self):
  class Void:
   def state(self,pos):return {"id":"minecraft:air"}
  with patch.object(mod,"World",return_value=Void()):
   with self.assertRaisesRegex(ValueError,"No nearby safe"):
    mod.stand("not-needed",(0,0,0))
if __name__=="__main__":unittest.main()
