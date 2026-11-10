#!/usr/bin/env python3
"""Synthetic tests of the 8+9 combined offline layer: fail closed and exact restore."""
import hashlib
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

import preview_drehmal_eight_nine_combined as combined


class CombinedExperimentTests(unittest.TestCase):
    def fixture(self):
        buf = io.BytesIO()
        targets = {}
        func = b"tp @p 1 2 3\n"
        with zipfile.ZipFile(buf, "w") as z:
            z.writestr(combined.nine.selective.PRIMORDIAL_ENTRY, json.dumps({
                "criteria": {"one": {"trigger": "minecraft:location"}},
                "display": {"title": "keep"},
                "rewards": {"function": combined.nine.selective.PRIMORDIAL_REWARD},
            }))
            z.writestr("data/players/functions/sethost.mcfunction", func)
            for i, name in enumerate(sorted(combined.eight.TARGET_NAMES)):
                cb = "players:test_" + str(i)
                raw = json.dumps({
                    "criteria": {"one": {"trigger": "minecraft:consume_item"}},
                    "display": {"title": "keep everything"},
                    "rewards": {"function": cb},
                }).encode()
                z.writestr(name, raw)
                targets[name] = (hashlib.sha256(raw).hexdigest(), cb)
            z.writestr("data/misc/functions/structure.mcfunction", b"say original untouched")
        return buf.getvalue(), func, targets

    def test_ten_exact_members_and_two_orders(self):
        before, func, targets = self.fixture()
        with patch.object(combined.nine.selective, "EXPECTED_FUNCTION_SHA", hashlib.sha256(func).hexdigest()):
            after = combined.combine_and_compare(before, targets)
            self.assertNotEqual(before, after)
            with zipfile.ZipFile(io.BytesIO(before)) as src, zipfile.ZipFile(io.BytesIO(after)) as dst:
                modified = {n for n in src.namelist() if src.read(n) != dst.read(n)}
                self.assertEqual(modified, combined.EXPECTED_MEMBERS)
                self.assertEqual(src.read("data/misc/functions/structure.mcfunction"),
                                 dst.read("data/misc/functions/structure.mcfunction"))
                self.assertEqual(src.read("data/players/functions/sethost.mcfunction"),
                                 dst.read("data/players/functions/sethost.mcfunction"))

    def test_apply_verify_restore_and_interrupted_overlay_rejection(self):
        with tempfile.TemporaryDirectory() as tmp:
            world = Path(tmp)
            pack = world / combined.PACK
            pack.parent.mkdir()
            original, func, targets = self.fixture()
            pack.write_bytes(original)
            source_sha = hashlib.sha256(original).hexdigest()
            with patch.object(combined, "source_guard", return_value=(world, pack)), \
                 patch.object(combined.six, "receipt", return_value={"patched_pack_sha256": source_sha}), \
                 patch.object(combined.seven, "verify", return_value={}), \
                 patch.object(combined.nine.selective, "EXPECTED_FUNCTION_SHA", hashlib.sha256(func).hexdigest()):
                result = combined.apply(world, targets)
                self.assertTrue(result["verified"])
                self.assertEqual(result["changed_original_zip_members"], 10)
                with self.assertRaisesRegex(ValueError, "overlay"):
                    combined.preflight(world, targets)
                self.assertEqual(combined.verify(world), result)
                restored = combined.restore(world)
                self.assertEqual(pack.read_bytes(), original)
                self.assertEqual(restored["status"], "EIGHT_NINE_EXPERIMENT_RESTORED_EXACTLY_TO_SEVEN")
                self.assertFalse((world / combined.RECEIPT).exists())
                self.assertFalse((world / combined.BACKUP).exists())

    def test_corruption_and_wrong_callback_rejected(self):
        original, func, targets = self.fixture()
        with patch.object(combined.nine.selective, "EXPECTED_FUNCTION_SHA", hashlib.sha256(func).hexdigest()):
            name = sorted(targets)[0]
            altered = dict(targets)
            altered[name] = (targets[name][0], "players:wrong")
            with self.assertRaises(ValueError):
                combined.combine_and_compare(original, altered)
            fewer = dict(targets)
            del fewer[name]
            with self.assertRaises(ValueError):
                combined.combine_and_compare(original, fewer)
            with tempfile.TemporaryDirectory() as tmp:
                world = Path(tmp)
                pack = world / combined.PACK
                pack.parent.mkdir()
                pack.write_bytes(original)
                source_sha = hashlib.sha256(original).hexdigest()
                with patch.object(combined, "source_guard", return_value=(world, pack)), \
                     patch.object(combined.six, "receipt", return_value={"patched_pack_sha256": source_sha}), \
                     patch.object(combined.seven, "verify", return_value={}):
                    combined.apply(world, targets)
                    pack.write_bytes(pack.read_bytes() + b"tampered")
                    with self.assertRaisesRegex(ValueError, "changed after application"):
                        combined.verify(world)

    def test_activated_world_and_other_overlay_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            world = Path(tmp)
            (world / "turnbound-world.marker").write_text("active")
            with self.assertRaisesRegex(ValueError, "Activated"):
                combined.source_guard(world)


if __name__ == "__main__":
    unittest.main()
