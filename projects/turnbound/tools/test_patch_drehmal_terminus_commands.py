import hashlib
import importlib.util
import json
import struct
import tempfile
import unittest
import zlib
from pathlib import Path

source = Path(__file__).with_name("patch_drehmal_terminus_commands.py")
spec = importlib.util.spec_from_file_location("terminus_copy", source)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def text(s):
    b = s.encode()
    return struct.pack(">H", len(b)) + b

def tag(kind, name, body):
    return bytes([kind]) + text(name) + body

def block(x, command):
    return (tag(8, "id", text("minecraft:command_block"))
            + tag(3, "x", struct.pack(">i", x))
            + tag(3, "y", struct.pack(">i", 75))
            + tag(3, "z", struct.pack(">i", 20))
            + tag(8, "Command", text(command)) + b"\x00")

class TerminusCopyTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.world = Path(tmp.name) / "copy"
        (self.world / "region").mkdir(parents=True)
        self.level = b"fixture"
        self.level_sha = hashlib.sha256(self.level).hexdigest()
        (self.world / "level.dat").write_bytes(self.level)
        (self.world / "turnbound-stage.json").write_text(json.dumps({
            "schema": 1, "source_level_sha256": self.level_sha
        }))
        self.command = "function terminus:teleport_to_tower/demo_teleport"
        self.other = "function core:scene/door"
        nbt = (b"\x0a\x00\x00" + tag(9, "block_entities",
                b"\x0a" + struct.pack(">i", 2) + block(8, self.command)
                + block(10, self.other)) + b"\x00")
        compressed = b"\x02" + zlib.compress(nbt)
        sector = (struct.pack(">I", len(compressed)) + compressed).ljust(4096, b"\x00")
        self.region = self.world / "region" / "r.0.0.mca"
        self.original = ((2).to_bytes(3,"big") + b"\x01"
                         + bytes(4096 - 4) + bytes(4096) + sector)
        self.region.write_bytes(self.original)
        self.target = ("region/r.0.0.mca", 0, (8,75,20),
                       m.sha(self.command.encode()), "minecraft:command_block")

    def test_targeted_patch_other_commands_unmodified(self):
        result, count = m.patch_region(self.original, {0:[self.target[2:]]})
        self.assertEqual(count, 1)
        offset = int.from_bytes(result[:3],"big") * 4096
        length = struct.unpack(">I", result[offset:offset+4])[0]
        _, raw = m.decompress(result[offset+4:offset+4+length])
        values = [row[2] for row in m.SpanReader(raw).scan()]
        self.assertEqual(values, ["", self.other])
        self.assertEqual(self.region.read_bytes(), self.original)
        self.assertEqual(result[4096:8192],self.original[4096:8192])

    def test_bad_hash_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "absent"):
            m.patch_region(self.original, {0:[((8,75,20), "0"*64, "minecraft:command_block")]})
        self.assertEqual(self.region.read_bytes(), self.original)

    def test_exact_restoration_and_tamper_refusal(self):
        prepared = [self.target]
        # World validation substitutes fixture level SHA in this isolated test.
        with self.assertRaisesRegex(ValueError, "official release"):
            m.apply(self.world, prepared)
        changed, _ = m.patch_region(self.original, {0:[self.target[2:]]})
        backup = self.world / m.BACKUP / "region"
        backup.mkdir(parents=True)
        (backup / self.region.name).write_bytes(self.original)
        (self.world / m.RECEIPT).write_text(json.dumps({
            "mode": "OFFLINE_TERMINUS_ONLY",
            "original_level_sha256": self.level_sha,
            "target_count": 1,
            "regions": {"region/r.0.0.mca": {
                "before":m.sha(self.original),"after":m.sha(changed)}}
        }))
        self.region.write_bytes(changed)
        result = m.restore(self.world,self.level_sha)
        self.assertEqual(result["target_count"],1)
        self.assertEqual(self.region.read_bytes(),self.original)

    def test_active_world_refused(self):
        (self.world / "turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
        with self.assertRaisesRegex(ValueError, "activated"):
            m.validate_world(self.world,self.level_sha)

if __name__ == "__main__":
    unittest.main()
