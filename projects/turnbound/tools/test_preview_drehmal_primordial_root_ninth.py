#!/usr/bin/env python3
"""Fail-closed, single-member, exact-restore contract for ninth map boundary."""
import hashlib
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
import preview_drehmal_primordial_root_ninth as nine

class NinthBoundaryTest(unittest.TestCase):
    def source_fixture(self):
        src=io.BytesIO()
        function=b"tp @p 26520 177 -84\n"
        with zipfile.ZipFile(src,"w") as z:
            z.writestr(nine.selective.PRIMORDIAL_ENTRY,json.dumps({
                "criteria":{"a":{"trigger":"minecraft:location"}},
                "rewards":{"function":nine.selective.PRIMORDIAL_REWARD},
                "display":{"title":"keep"}
            }))
            z.writestr("data/players/functions/sethost.mcfunction",function)
            z.writestr("data/my/functions/buildings.mcfunction","say unchanged")
        return src.getvalue(),function

    def test_exact_apply_verify_restore_without_modified_other_members(self):
        with tempfile.TemporaryDirectory() as d:
            world=Path(d)
            pack=world/"datapacks"/"hi_drehmal.zip"
            pack.parent.mkdir()
            before,function=self.source_fixture()
            pack.write_bytes(before)
            digest=hashlib.sha256(before).hexdigest()
            with patch.object(nine,"stage_gate",return_value=(world,pack)),patch.object(
                    nine.six,"receipt",return_value={"patched_pack_sha256":digest}),patch.object(
                    nine.seven,"verify",return_value={"seven_stage_offline_experiment_verified":True}),patch.object(
                    nine.selective,"EXPECTED_FUNCTION_SHA",hashlib.sha256(function).hexdigest()):
                data=nine.apply(world)
                self.assertTrue(data["verified"])
                self.assertEqual(1,data["modified_datapack_members"])
                self.assertNotEqual(before,pack.read_bytes())
                self.assertTrue((world/nine.BACKUP).exists())
                self.assertTrue((world/nine.RECEIPT).exists())
                restored=nine.restore(world)
                self.assertEqual(before,pack.read_bytes())
                self.assertEqual("NINTH_PRIMORDIAL_ROOT_PREVIEW_RESTORED_EXACTLY_TO_SEVENTH",
                                 restored["status"])
                self.assertFalse((world/nine.RECEIPT).exists())
                self.assertFalse((world/nine.BACKUP).exists())

    def test_any_extra_pack_mutation_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            world=Path(d)
            pack=world/"datapacks"/"hi_drehmal.zip"
            pack.parent.mkdir()
            before,function=self.source_fixture()
            pack.write_bytes(before)
            base=hashlib.sha256(before).hexdigest()
            with patch.object(nine,"stage_gate",return_value=(world,pack)),patch.object(
                    nine.six,"receipt",return_value={"patched_pack_sha256":base}),patch.object(
                    nine.seven,"verify",return_value={}),patch.object(
                    nine.selective,"EXPECTED_FUNCTION_SHA",hashlib.sha256(function).hexdigest()):
                nine.apply(world)
                pack.write_bytes(pack.read_bytes()+b"bad")
                with self.assertRaisesRegex(ValueError,"drifted"):
                    nine.verify(world)

    def test_refuses_activated_copy_before_other_mutations(self):
        with tempfile.TemporaryDirectory() as d:
            w=Path(d)
            (w/"turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
            with self.assertRaisesRegex(ValueError,"Activated"):
                nine.stage_gate(w)

if __name__=="__main__":
    unittest.main()
