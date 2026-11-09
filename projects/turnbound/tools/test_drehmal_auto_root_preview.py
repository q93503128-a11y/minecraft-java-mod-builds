import hashlib
import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

SOURCE = Path(__file__).with_name("drehmal_auto_root_preview.py")
spec = importlib.util.spec_from_file_location("auto_root_preview", SOURCE)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class AutoRootPreviewTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.world = self.root / "copy"
        (self.world / "datapacks").mkdir(parents=True)
        self.level = b"fixture map original level"
        (self.world / "level.dat").write_bytes(self.level)
        self.level_hash = hashlib.sha256(self.level).hexdigest()
        (self.world / "turnbound-stage.json").write_text(json.dumps({
            "schema": 1, "source_level_sha256": self.level_hash
        }), encoding="utf-8")
        self.source_tags = {
            "data/minecraft/tags/functions/load.json": b'{"values":["core:init"]}',
            "data/minecraft/tags/functions/tick.json": b'{"values":["core:main","entities:main"]}'
        }
        self.expected = {
            k: (hashlib.sha256(v).hexdigest(), list(json.loads(v)["values"]))
            for k,v in self.source_tags.items()
        }
        self.pack = self.world / "datapacks" / module.PACK_NAME
        with zipfile.ZipFile(self.pack, "w", zipfile.ZIP_DEFLATED) as z:
            for name, raw in self.source_tags.items():
                z.writestr(name, raw)
            z.writestr("data/terminus/functions/teleport_from_tower/demo.mcfunction",
                       "tp @p 10 80 10\n")
            z.writestr("data/core/functions/main.mcfunction",
                       "execute as @a run function terminus:teleport_from_tower/demo\n")
        self.source_sha = module.file_hash(self.pack)

    def test_only_two_tags_disabled_and_exact_restoration(self):
        result = module.apply(self.world, self.level_hash, self.expected)
        self.assertEqual(result["status"], "OFFLINE_PREVIEW_ONLY")
        self.assertNotEqual(self.source_sha, module.file_hash(self.pack))
        with zipfile.ZipFile(self.pack) as z:
            for name in self.expected:
                self.assertEqual(json.loads(z.read(name))["values"], [])
            self.assertIn(b"tp @p", z.read(
                "data/terminus/functions/teleport_from_tower/demo.mcfunction"))
        self.assertEqual(module.restore(self.world, self.level_hash)["status"], "RESTORED")
        self.assertEqual(self.source_sha, module.file_hash(self.pack))
        self.assertFalse((self.world / module.RECEIPT_FILE).exists())

    def test_rejects_unstaged_and_activated_world(self):
        (self.world / "turnbound-stage.json").unlink()
        with self.assertRaisesRegex(ValueError, "staged"):
            module.apply(self.world, self.level_hash, self.expected)
        (self.world / "turnbound-stage.json").write_text(json.dumps({
            "schema": 1, "source_level_sha256": self.level_hash
        }), encoding="utf-8")
        (self.world / "turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
        with self.assertRaisesRegex(ValueError, "activated"):
            module.apply(self.world, self.level_hash, self.expected)

    def test_rejects_wrong_hash_and_prevents_double_patch(self):
        corrupt = dict(self.expected)
        first = next(iter(corrupt))
        corrupt[first] = ("0"*64, corrupt[first][1])
        with self.assertRaisesRegex(ValueError, "hash"):
            module.apply(self.world, self.level_hash, corrupt)
        self.assertEqual(self.source_sha, module.file_hash(self.pack))
        module.apply(self.world, self.level_hash, self.expected)
        with self.assertRaisesRegex(ValueError, "Preview already exists"):
            module.apply(self.world, self.level_hash, self.expected)

    def test_refuses_restore_if_edited_after_preview(self):
        module.apply(self.world, self.level_hash, self.expected)
        with zipfile.ZipFile(self.pack, "a") as z:
            z.writestr("new.txt", "user data")
        with self.assertRaisesRegex(ValueError, "changed after preview"):
            module.restore(self.world, self.level_hash)

if __name__ == "__main__":
    unittest.main()
