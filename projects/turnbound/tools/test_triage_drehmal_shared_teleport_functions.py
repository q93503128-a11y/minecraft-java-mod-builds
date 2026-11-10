#!/usr/bin/env python3
"""No original-command extraction and bounding tests for movement path triage."""
import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
import triage_drehmal_shared_teleport_functions as m

class StaticTeleportTriageTest(unittest.TestCase):
    def test_comments_not_counted_and_function_commands_not_published(self):
        name="data/example/functions/spawn/entry.mcfunction"
        content=b"# teleport @p 26520 177 -84\nexecute as @a run tp @s 26520.5 177 -84.5\nfunction example:warp/link\n"
        row=m.examine_function(name,content)
        self.assertEqual("example:spawn/entry",row["function_id"])
        self.assertEqual(1,row["direct_teleport_lines"])
        self.assertEqual(["example:warp/link"],row["calls"])
        self.assertTrue(row["location_numeric_hints"]["reentry_26520"])
        self.assertTrue(row["location_numeric_hints"]["reentry_negative84"])
        self.assertNotIn("execute",str(row))

    def test_candidates_are_bounded_and_cannot_be_marked_proven(self):
        fns={
          "example:spawn/entry":m.examine_function(
              "data/example/functions/spawn/entry.mcfunction",
              b"tp @p 26520 177 -84\n"),
          "example:harmless":m.examine_function(
              "data/example/functions/harmless.mcfunction",
              b"say hi\n")
        }
        adv={
          "example:join":{
            "advancement_id":"example:join","reward_function_id":"example:spawn/entry",
            "triggers":["minecraft:location"],"name_hint":False
          }
        }
        out=m.candidate_report(fns,adv)
        self.assertEqual(1,out["candidate_total"])
        self.assertEqual(1,out["functions_with_direct_teleport_lines"])
        self.assertEqual(["example:join"],
                         out["candidate_function_records"][0]["reward_advancement_references"])
        self.assertFalse(out["specific_function_causing_relocation_proven"])
        self.assertFalse(out["map_separation_complete"])
        self.assertFalse(out["raw_original_commands_or_advancement_json_published"])

    def test_zip_entry_validation_and_only_expected_resources(self):
        with tempfile.TemporaryDirectory() as temp:
            p=Path(temp)/"original.zip"
            with zipfile.ZipFile(p,"w") as z:
                z.writestr("data/test/functions/tp_test.mcfunction","tp @p 0 70 0")
                z.writestr("data/test/advancements/tick.json",json.dumps({
                  "criteria":{"hello":{"trigger":"minecraft:tick"}},
                  "rewards":{"function":"test:tp_test"}
                }))
                z.writestr("data/test/advancements/no_reward.json",
                          '{"criteria":{}}')
            f,a=m.inspect_pack(p)
            self.assertEqual(1,len(f))
            self.assertEqual(2,len(a))
            self.assertEqual("test:tp_test",a["test:tick"]["reward_function_id"])
            self.assertIn("minecraft:tick",a["test:tick"]["triggers"])
            i=zipfile.ZipInfo("../secret")
            with self.assertRaisesRegex(ValueError,"Unsafe"):
                m.safety(i)

    def test_sha_identity_gate_rejects_untested_source(self):
        with tempfile.TemporaryDirectory() as temp:
            world=Path(temp)
            (world/"datapacks").mkdir()
            (world/"level.dat").write_bytes(b"fake")
            (world/"datapacks"/"hi_drehmal.zip").write_bytes(b"fake")
            with self.assertRaisesRegex(ValueError,"SHA"):
                m.checked_zip(world)

if __name__=="__main__":
    unittest.main()
