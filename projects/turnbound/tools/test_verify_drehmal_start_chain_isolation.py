import unittest
from unittest.mock import patch
import verify_drehmal_start_chain_isolation as mod

class StartChainSourceTests(unittest.TestCase):
    def test_expected_33_nonoverlapping_chain_commands(self):
        roles=mod.coordinate_roles()
        self.assertEqual(len(roles),33)
        self.assertEqual(set(roles.values()),{
            ("tp","tp"),("effect","effect"),("setblock","setblock"),
            ("execute","scoreboard"),("execute","advancement"),
            ("spawnpoint","spawnpoint"),("spreadplayers","spreadplayers"),
            ("gamerule","gamerule")})
        self.assertNotIn((26457,209,-260),roles)
        self.assertNotIn((26458,214,-288),roles)
    def test_pinned_source_and_partial_refusal(self):
        with self.assertRaises(FileNotFoundError): mod.read_targets("/nonexistent/official-aux.json")
        with self.assertRaises(FileNotFoundError): mod.validate_copy("/nonexistent/copy")
    def test_seventh_stage_is_not_gameplay_authorization(self):
        self.assertEqual(mod.CHAIN_COUNT,57)
        self.assertEqual(mod.EXPECTED_COUNT,33)
        self.assertFalse("turnbound-world.marker" in mod.RECEIPT)

if __name__=="__main__":
    unittest.main()
