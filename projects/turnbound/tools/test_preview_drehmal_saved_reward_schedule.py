import gzip
import hashlib
import importlib.util
import json
import struct
import tempfile
import unittest
from pathlib import Path

p=Path(__file__).with_name("preview_drehmal_saved_reward_schedule.py")
spec=importlib.util.spec_from_file_location("schedule_patch",p)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def txt(v):
    raw=v.encode()
    return struct.pack(">H",len(raw))+raw

def tag(tagtype,name,raw):
    return bytes([tagtype])+txt(name)+raw

def comp(*fields):
    return b"".join(fields)+b"\x00"

def event(name="terminus:gifts/respawn_gift",tick=1135083594):
    return comp(tag(8,"Name",txt(name)),tag(4,"TriggerTime",struct.pack(">q",tick)),
                tag(10,"Callback",comp(tag(8,"Type",txt("minecraft:function")),
                                       tag(8,"Name",txt(name)))))

def save(time=1135083463,single=True):
    content=tag(9,"ScheduledEvents",b"\x0a"+struct.pack(">i",1 if single else 0)+(event() if single else b""))
    return gzip.compress(b"\x0a\x00\x00"+comp(tag(10,"Data",comp(
        tag(4,"Time",struct.pack(">q",time)),content,
        tag(8,"OtherUnrelatedValue",txt("STAY"))))),mtime=0)

class ScheduleCopyTests(unittest.TestCase):
    def setUp(self):
        t=tempfile.TemporaryDirectory()
        self.addCleanup(t.cleanup)
        self.world=Path(t.name)/"copy"
        self.world.mkdir()
        self.original=save()
        self.sha=hashlib.sha256(self.original).hexdigest()
        (self.world/"level.dat").write_bytes(self.original)
        (self.world/"turnbound-stage.json").write_text(json.dumps({
            "schema":1,"source_level_sha256":self.sha
        }),encoding="utf-8")

    def test_removes_one_event_and_preserves_other_level_nbt(self):
        patched=m.preview_bytes(self.original,self.sha)
        _,root=m.load_nbt(patched)
        self.assertEqual(root["Data"]["ScheduledEvents"],[])
        self.assertEqual(root["Data"]["OtherUnrelatedValue"],"STAY")
        self.assertEqual(root["Data"]["Time"],1135083463)
        self.assertEqual(patched,m.preview_bytes(self.original,self.sha))
    def test_invalid_source_fails_without_altering_file(self):
        with self.assertRaisesRegex(ValueError,"hash mismatch"):
            m.preview_bytes(self.original,"0"*64)
        self.assertEqual((self.world/"level.dat").read_bytes(),self.original)
    def test_empty_schedule_not_accepted(self):
        original=save(single=False)
        with self.assertRaisesRegex(ValueError,"Unexpected saved scheduled event count"):
            m.preview_bytes(original,m.digest(original))
    def test_other_saved_event_not_accidentally_suppressed(self):
        with self.assertRaisesRegex(ValueError,"Saved reward event differs"):
            m.preview_bytes(self.original,self.sha,expected_name="terminus:other")
    def test_exact_restore_after_apply(self):
        result=m.apply(self.world,source_sha=self.sha)
        self.assertEqual(result["event_count"],1)
        self.assertNotEqual((self.world/"level.dat").read_bytes(),self.original)
        self.assertEqual(m.restore(self.world,self.sha)["status"],"ORIGINAL_LEVEL_EXACTLY_RESTORED")
        self.assertEqual((self.world/"level.dat").read_bytes(),self.original)
    def test_modified_copy_refuses_restore(self):
        m.apply(self.world,source_sha=self.sha)
        (self.world/"level.dat").write_bytes(b"external edit")
        with self.assertRaisesRegex(ValueError,"modified after preview"):
            m.restore(self.world,self.sha)
        self.assertTrue((self.world/m.BACKUP).exists())
    def test_live_world_gate_fails_closed(self):
        (self.world/"turnbound-world.marker").write_text("TURNBOUND_DREHMAL_WORLD_V1")
        with self.assertRaisesRegex(ValueError,"activated"):
            m.apply(self.world,source_sha=self.sha)
if __name__=="__main__":
    unittest.main()
