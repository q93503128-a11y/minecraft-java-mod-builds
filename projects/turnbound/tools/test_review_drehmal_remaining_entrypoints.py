import importlib.util
import unittest
from pathlib import Path
s=importlib.util.spec_from_file_location("r",Path(__file__).with_name("review_drehmal_remaining_entrypoints.py"))
m=importlib.util.module_from_spec(s)
s.loader.exec_module(m)
class ReviewTest(unittest.TestCase):
    def test_cycle_and_unknown(self):
        g={"a":{"calls":["b","bad"]},"b":{"calls":["a"]}}
        self.assertEqual(m.closure(g,["a"]),({"a","b"},{"bad"}))
    def test_missing_root_does_not_count_as_reachable(self):
        self.assertEqual(m.closure({},["missing"]),(set(),{"missing"}))
    def test_failed_source_identity(self):
        with self.assertRaisesRegex(ValueError,"mismatch"):
            m.classify({"map_release":"unknown"},{"level_dat_sha256":"bad"})
    def test_source_sha_stable_format(self):
        self.assertEqual(m.sha(b"abc"),"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
if __name__=="__main__":unittest.main()
