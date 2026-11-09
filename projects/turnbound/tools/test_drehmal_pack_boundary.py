import hashlib
import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

MODULE = Path(__file__).with_name("drehmal_pack_boundary.py")
spec = importlib.util.spec_from_file_location("drehmal_pack_boundary", MODULE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class DrehmalBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name)
        self.world = self.base / "world"
        self.world.mkdir()
        (self.world / "level.dat").write_bytes(b"sample nbt fixture")
        (self.world / "datapacks").mkdir()

    def test_tag_graph_classification_and_source_remains_unchanged(self):
        pack = self.world / "datapacks" / "original.zip"
        with zipfile.ZipFile(pack, "w") as z:
            z.writestr("data/minecraft/tags/functions/tick.json",
                       json.dumps({"values": ["drehmal:loop"]}))
            z.writestr("data/drehmal/functions/loop.mcfunction",
                       "execute as @a run function drehmal:story/reward\n")
            z.writestr("data/drehmal/functions/story/reward.mcfunction",
                       "give @p diamond\nscoreboard players add @p progress 1\n")
            z.writestr("data/drehmal/functions/door/open.mcfunction", "setblock ~ ~ ~ redstone_block\n")
        before = hashlib.sha256(pack.read_bytes()).hexdigest()
        report = mod.inspect_world(self.world)
        funcs = {f["id"]: f for f in report["functions"]}
        self.assertEqual(report["function_count"], 3)
        self.assertEqual(report["reachable_function_count"], 2)
        self.assertIn("rewards", funcs["drehmal:story/reward"]["categories"])
        self.assertTrue(funcs["drehmal:loop"]["reachable_from_load_or_tick"])
        self.assertEqual(funcs["drehmal:story/reward"]["called_by"], ["drehmal:loop"])
        self.assertIn("world_edit", funcs["drehmal:door/open"]["categories"])
        self.assertEqual(report["command_blocks"]["status"], "NOT_SCANNED")
        self.assertEqual(hashlib.sha256(pack.read_bytes()).hexdigest(), before)

    def test_reject_zip_escape(self):
        pack = self.world / "datapacks" / "danger.zip"
        with zipfile.ZipFile(pack, "w") as z:
            z.writestr("../unsafe.mcfunction", "say danger")
        with self.assertRaisesRegex(ValueError, "Unsafe ZIP"):
            mod.inspect_world(self.world)

    def test_stage_never_overwrites_original_or_existing_destination(self):
        (self.world / "datapacks" / "empty").mkdir()
        (self.world / "region").mkdir()
        (self.world / "region" / "r.0.0.mca").write_bytes(b"regional data")
        copied = self.base / "copy"
        mod.stage_copy(self.world, copied)
        self.assertEqual((copied / "level.dat").read_bytes(), (self.world / "level.dat").read_bytes())
        self.assertTrue((copied / "turnbound-stage.json").is_file())
        self.assertFalse((self.world / "turnbound-stage.json").exists())
        with self.assertRaisesRegex(ValueError, "already exists"):
            mod.stage_copy(self.world, copied)

if __name__ == "__main__":
    unittest.main()
