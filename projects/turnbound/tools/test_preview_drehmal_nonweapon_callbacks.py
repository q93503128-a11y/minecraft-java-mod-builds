import io
import json
import unittest
import zipfile
import preview_drehmal_nonweapon_callbacks as mod

class OriginalNonweaponCallbackTests(unittest.TestCase):
    def source(self):
        stream=io.BytesIO()
        targets={}
        with zipfile.ZipFile(stream,"w") as out:
            for i,path in enumerate(sorted(mod.TARGET_NAMES)):
                callback=f"players:test_{i}"
                original=json.dumps({
                    "display":{"title":"preserve this"},
                    "criteria":{"one":{"trigger":"minecraft:consume_item"}},
                    "rewards":{"function":callback},
                }).encode()
                out.writestr(path,original)
                targets[path]=(mod.hash_bytes(original),callback)
            out.writestr("data/custom/functions/f.mcfunction","say never edit this original function")
        return stream.getvalue(),targets

    def test_nine_bounded_nonlocation_candidates(self):
        self.assertEqual(len(mod.TARGET_NAMES),9)
        self.assertFalse(any("/near_trial/" in p for p in mod.TARGET_NAMES))
        self.assertEqual(len(mod.SOURCE_SHA),64)

    def test_zip_isolation_preserves_all_other_entries_and_originals(self):
        original,targets=self.source()
        after=mod.repack(original,targets)
        self.assertNotEqual(mod.hash_bytes(original),mod.hash_bytes(after))
        self.assertTrue(mod.compare(original,after,targets))
        with zipfile.ZipFile(io.BytesIO(after)) as archive:
            for path in targets:
                doc=json.loads(archive.read(path))
                self.assertEqual(doc["rewards"],{})
                self.assertEqual(doc["criteria"]["one"]["trigger"],"minecraft:consume_item")
                self.assertEqual(doc["display"]["title"],"preserve this")
            self.assertEqual(archive.read("data/custom/functions/f.mcfunction"),
                             b"say never edit this original function")

    def test_corruption_and_wrong_callback_fail_closed(self):
        original,targets=self.source()
        bad=dict(targets)
        a=next(iter(bad))
        bad[a]=(bad[a][0],"players:wrong")
        with self.assertRaisesRegex(ValueError,"reward"):
            mod.repack(original,bad)
        bad=dict(targets)
        bad.pop(a)
        with self.assertRaises(ValueError):
            mod.repack(original,bad)
        with self.assertRaises((OSError,ValueError)):
            mod.load_targets("/file/does/not/exist")

if __name__=="__main__":
    unittest.main()
