#!/usr/bin/env python3
"""Strict one-original-advancement-only copied-world patch tests."""
import hashlib
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
import probe_drehmal_primordial_reward_runtime as p

class PrimordialRewardIsolationTest(unittest.TestCase):
    def fixture_pack(self):
        pth=p.PRIMORDIAL_ENTRY
        function_path="data/players/functions/sethost.mcfunction"
        function=b"tp @p 26520 177 -84\n"
        advance={
            "criteria":{"location":{"trigger":"minecraft:location"}},
            "rewards":{"function":"players:sethost","experience":12},
            "display":{"title":"unchanged","description":"still here"}
        }
        data=io.BytesIO()
        with zipfile.ZipFile(data,"w") as z:
            z.writestr(pth,json.dumps(advance))
            z.writestr(function_path,function)
            z.writestr("data/other/functions/structure.mcfunction","say unchanged")
        return data.getvalue(),function,advance

    def test_advancement_reward_is_only_removed_field(self):
        original={"criteria":{"location":{"trigger":"minecraft:location"}},
                  "rewards":{"function":"players:sethost","experience":12},
                  "display":{"title":"untouched"}}
        changed=json.loads(p.filtered_advancement(json.dumps(original).encode()))
        self.assertEqual(changed["rewards"],{"experience":12})
        self.assertEqual(changed["criteria"],original["criteria"])
        self.assertEqual(changed["display"],original["display"])
        for invalid in (
            {"criteria":{},"rewards":{"function":"players:sethost"}},
            {"criteria":{"a":{"trigger":"minecraft:location"}},
             "rewards":{"function":"players:other"}},
        ):
            with self.assertRaisesRegex(ValueError,"location|function"):
                p.filtered_advancement(json.dumps(invalid).encode())

    def test_repack_preserves_every_original_member_except_one(self):
        before,function,original_adv=self.fixture_pack()
        with patch.object(p,"EXPECTED_FUNCTION_SHA",hashlib.sha256(function).hexdigest()):
            after=p.repack_without_primordial_reward(before)
        self.assertNotEqual(hashlib.sha256(before).digest(),hashlib.sha256(after).digest())
        with zipfile.ZipFile(io.BytesIO(before)) as a,zipfile.ZipFile(io.BytesIO(after)) as b:
            self.assertEqual(a.namelist(),b.namelist())
            modified=[name for name in a.namelist() if a.read(name)!=b.read(name)]
            self.assertEqual([p.PRIMORDIAL_ENTRY],modified)
            self.assertEqual(a.read("data/other/functions/structure.mcfunction"),
                             b.read("data/other/functions/structure.mcfunction"))

    def test_missing_or_mismatched_original_function_fails_closed(self):
        before,_,_=self.fixture_pack()
        with self.assertRaisesRegex(ValueError,"function bytes changed"):
            p.repack_without_primordial_reward(before)

    def test_copy_only_backup_and_no_activation_allowed(self):
        with tempfile.TemporaryDirectory() as temp:
            world=Path(temp)
            pack=world/"datapacks"/"hi_drehmal.zip"
            pack.parent.mkdir()
            raw,function,_=self.fixture_pack()
            pack.write_bytes(raw)
            (world/"turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
            with self.assertRaisesRegex(ValueError,"untrusted or activated"):
                p.isolate_copy_datapack(world)
            (world/"turnbound-world.marker").unlink()
            with patch.object(p,"EXPECTED_FUNCTION_SHA",hashlib.sha256(function).hexdigest()):
                result=p.isolate_copy_datapack(world)
            self.assertEqual(1,result["modified_pack_members"])
            self.assertTrue(result["function_code_unchanged"])
            self.assertTrue((world/"turnbound-primordial-root-ci-backup.zip").is_file())
            with self.assertRaisesRegex(ValueError,"existing private CI backup"):
                p.isolate_copy_datapack(world)

if __name__=="__main__":
    unittest.main()
