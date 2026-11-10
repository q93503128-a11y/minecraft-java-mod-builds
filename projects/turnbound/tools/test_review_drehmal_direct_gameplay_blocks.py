import importlib.util
import unittest
from pathlib import Path
path=Path(__file__).with_name("review_drehmal_direct_gameplay_blocks.py")
spec=importlib.util.spec_from_file_location("review_direct",path)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
class ReviewDirectTests(unittest.TestCase):
    def test_give_item_id_excludes_nbt(self):
        x=m.parse_direct_effect('give @p minecraft:diamond_sword{display:{Name:"secret"}} 1',"give")
        self.assertEqual(x["resource_id"],"minecraft:diamond_sword")
        self.assertEqual(x["target_class"],"NEAREST_PLAYER")
        self.assertNotIn("secret",str(x))
    def test_advancement_scope(self):
        x=m.parse_direct_effect("advancement grant @a only story:step_one","advancement")
        self.assertEqual(x["resource_id"],"story:step_one")
        self.assertEqual(x["parameter_shape"],"ADVANCEMENT_ONLY")
    def test_effect_clear(self):
        x=m.parse_direct_effect("effect clear @a minecraft:poison","effect")
        self.assertEqual(x["effect_action"],"CLEAR")
        self.assertEqual(x["target_class"],"ALL_PLAYERS")
    def test_kill_preserves_uncertainty(self):
        x=m.parse_direct_effect("kill @e[type=minecraft:armor_stand]","kill")
        self.assertEqual(x["target_class"],"ENTITY_QUERY")
        self.assertFalse(x["review"].startswith("SAFE"))
    def test_summon_no_raw_tags(self):
        x=m.parse_direct_effect("summon minecraft:armor_stand ~ ~ ~ {CustomName:'secret'}","summon")
        self.assertEqual(x["resource_id"],"minecraft:armor_stand")
        self.assertNotIn("secret",str(x))
    def test_target_scope_never_exports_literal(self):
        self.assertEqual(m.selector_class("privatePlayerName"),"LITERAL_OR_UNPARSED_REVIEW")
    def test_record_key_integrity(self):
        data={"region_file":"region/r.0.0.mca","chunk_slot":9,"coordinates":[1,2,3],
              "command_actor":"minecraft:command_block","command_sha256":"a"*64}
        self.assertEqual(m.key(data)[2],(1,2,3))
if __name__=="__main__":
    unittest.main()
