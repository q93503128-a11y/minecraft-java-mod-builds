import importlib.util
import struct
import tempfile
import unittest
import zlib
from pathlib import Path

MODULE = Path(__file__).with_name("scan_drehmal_command_blocks.py")
spec = importlib.util.spec_from_file_location("drehmal_region_scanner", MODULE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

def txt(s):
    data = s.encode("utf-8")
    return struct.pack(">H", len(data)) + data

def tag(kind, name, payload):
    return bytes([kind]) + txt(name) + payload

def compound(name, children):
    return tag(10, name, b"".join(children) + b"\x00")

def string_tag(name, value):
    return tag(8, name, txt(value))

class RegionScanTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.world = Path(self.tmp.name)
        (self.world / "level.dat").write_bytes(b"minimal fixture")
        (self.world / "region").mkdir()

    def make_region(self, nbt):
        comp = zlib.compress(nbt)
        length = len(comp) + 1
        payload = struct.pack(">I", length) + b"\x02" + comp
        self.assertLess(len(payload), 4096)
        loc = (2).to_bytes(3, "big") + b"\x01" + b"\x00" * (4096 - 4)
        header = loc + b"\x00" * 4096
        (self.world / "region" / "r.0.0.mca").write_bytes(header + payload.ljust(4096, b"\x00"))

    def test_command_block_detected_in_region_nbt(self):
        block = compound("", [string_tag("id", "minecraft:command_block"),
                              string_tag("Command", "function drehmal:story/intro")])
        entities = tag(9, "block_entities", b"\x0a" + struct.pack(">i", 1) + block[3:])
        self.make_region(compound("", [entities]))
        report = mod.scan_world(self.world)
        self.assertEqual(report["status"], "SCANNED")
        self.assertEqual(len(report["command_blocks"]), 1)
        self.assertIn("story/intro", report["command_blocks"][0]["command"])

    def test_corrupt_region_is_not_silently_cleared(self):
        (self.world / "region" / "r.0.0.mca").write_bytes(b"broken")
        report = mod.scan_world(self.world)
        self.assertEqual(report["status"], "SCANNED_WITH_WARNINGS")
        self.assertTrue(report["warnings"])

if __name__ == "__main__":
    unittest.main()
