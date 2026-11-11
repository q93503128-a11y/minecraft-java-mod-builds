#!/usr/bin/env python3
import unittest
from unittest.mock import patch
import probe_drehmal_foundry_natural_vs_door as m
class FoundryLiveDifferentialTests(unittest.TestCase):
 def test_gate_target_remains_same_pinned_destination(self):
  self.assertEqual(m.GATE_HINT,(-3276,160,1576))
  self.assertEqual(len(m.LEVERS),3)
  self.assertEqual(m.GATE_XZ_RADIUS,8)
 def test_natural_vs_explicit_delta_is_not_a_traversal_proof(self):
  first={(-3278,156,1576):"minecraft:bedrock"}
  second={}
  x=m.gate_diff(first,second)
  self.assertEqual(x["original_clone_destination_changed_blocks"],1)
  self.assertFalse(x["actual_foundry_door_traversal_verified"])
if __name__=="__main__":unittest.main()
