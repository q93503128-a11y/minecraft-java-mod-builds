import importlib.util
import tempfile
import unittest
from pathlib import Path

path=Path(__file__).with_name("inventory_drehmal_trigger_positions.py")
spec=importlib.util.spec_from_file_location("inventory_drehmal_trigger_positions",path)
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class InventoryTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root=Path(self.tmp.name)

    def test_position_dimension_and_safe_command_redaction(self):
        file=self.root / "dimensions" / "minecraft" / "lodahr" / "region" / "r.0.0.mca"
        file.parent.mkdir(parents=True)
        record={"id":"minecraft:command_block","command":"execute as @p run function terminus:teleport_to_tower/av_sal_teleport",
                "x":10,"y":63,"z":24,"file":str(file),"chunk_slot":3}
        x=mod.safe_trigger(record,self.root)
        self.assertEqual(x["source_dimension"],"minecraft:lodahr")
        self.assertEqual(x["coordinates"],[10,63,24])
        self.assertIn("TOWER_DEPARTURE_REPLACE",x["review_categories"])
        self.assertNotIn("command",x)
        self.assertNotIn("execute",str(x))
    def test_missing_coordinates_not_guessed(self):
        file=self.root/"region"/"r.1.1.mca"
        file.parent.mkdir()
        x=mod.safe_trigger({"id":"minecraft:commandblock_minecart","command":"function terminus:teleport_from_tower/test",
                            "x":None,"y":None,"z":None,"file":str(file),"chunk_slot":10},self.root)
        self.assertIsNone(x["coordinates"])
        self.assertEqual(x["position_source"],"UNKNOWN_REQUIRES_INSPECTION")
        self.assertEqual(x["source_dimension"],"minecraft:overworld")
    def test_nonfunction_command_is_not_exported(self):
        file=self.root/"region"/"r.1.1.mca"
        file.parent.mkdir()
        x=mod.safe_trigger({"id":"minecraft:command_block","command":"say hello",
                            "x":1,"y":2,"z":3,"file":str(file),"chunk_slot":10},self.root)
        self.assertIsNone(x)
    def test_outside_world_rejected(self):
        file=Path("/outside/region/r.1.1.mca")
        with self.assertRaisesRegex(ValueError,"outside"):
            mod.safe_trigger({"id":"minecraft:command_block","command":"function terminus:foo",
                              "x":1,"y":2,"z":3,"file":str(file),"chunk_slot":0},self.root)

if __name__=="__main__":
    unittest.main()
