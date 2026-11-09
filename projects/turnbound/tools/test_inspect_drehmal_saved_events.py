import gzip
import hashlib
import importlib.util
import json
import struct
import tempfile
import unittest
from pathlib import Path

p=Path(__file__).with_name("inspect_drehmal_saved_events.py")
spec=importlib.util.spec_from_file_location("saved_events",p)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def text(value):
    raw=value.encode("utf-8")
    return struct.pack(">H",len(raw))+raw

def tag(tagtype,name,payload):
    return bytes([tagtype])+text(name)+payload

def compound(*children):
    return b"".join(children)+b"\x00"

def named_compound(name,*children):
    return tag(10,name,compound(*children))

def level_fixture(pending=True):
    callback=named_compound("Callback",tag(8,"Type",text("minecraft:function")),
                            tag(8,"Name",text("core:scene/door")))
    event=compound(tag(8,"Name",text("core:scene/door")),
                   tag(4,"TriggerTime",struct.pack(">q",210)),callback)
    sched=tag(9,"ScheduledEvents",b"\x0a"+struct.pack(">i",1 if pending else 0)+(event if pending else b""))
    data=named_compound("Data",tag(4,"Time",struct.pack(">q",200)),sched)
    return gzip.compress(b"\x0a\x00\x00"+compound(data))

class SavedStateTests(unittest.TestCase):
    def setUp(self):
        tmp=tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root=Path(tmp.name)
        (self.root/"level.dat").write_bytes(level_fixture())
    def test_reads_exact_saved_event_not_static_schedule(self):
        level=m.read_gzip_nbt(self.root/"level.dat")
        out=m.saved_schedule(level)
        self.assertEqual(out["count"],1)
        self.assertEqual(out["entries"][0]["callback_id"],"core:scene/door")
        self.assertEqual(out["entries"][0]["trigger_tick"],210)
        self.assertEqual(out["save_game_time"],200)
        self.assertEqual(out["tag_state"],"HAS_SAVED_ENTRIES")
    def test_empty_and_absent_are_distinct(self):
        (self.root/"level.dat").write_bytes(level_fixture(False))
        self.assertEqual(m.saved_schedule(m.read_gzip_nbt(self.root/"level.dat"))["tag_state"],"EMPTY")
        self.assertEqual(m.saved_schedule({"Data":{}})["tag_state"],"ABSENT")
    def test_unknown_callback_type_not_auto_approved(self):
        obj={"Data":{"ScheduledEvents":[{"Name":"x","TriggerTime":1,
            "Callback":{"Type":"unrecognized:custom","Name":"test:foo"}}]}}
        x=m.saved_schedule(obj)
        self.assertEqual(x["entries"][0]["type_review"],"UNRECOGNIZED_REVIEW")
    def test_unexpected_event_rejected(self):
        with self.assertRaisesRegex(ValueError,"Missing scheduled callback compound"):
            m.saved_schedule({"Data":{"ScheduledEvents":[{"Name":"x"}]}})
    def test_world_hash_check_and_zero_writes(self):
        actual=m.file_sha(self.root/"level.dat")
        self.assertEqual(len(actual),64)
        with self.assertRaisesRegex(ValueError,"does not match"):
            m.inspect(self.root,self.root/"missing.json",expected_sha="0"*64)
        self.assertEqual(m.file_sha(self.root/"level.dat"),actual)
    def test_scoreboard_absent_no_fabrication(self):
        self.assertEqual(m.scoreboard_summary(self.root)["state"],"ABSENT")
    def test_command_storage_metadata_only(self):
        data=self.root/"data"
        data.mkdir()
        secret=data/"command_storage_core.dat"
        secret.write_bytes(b"very private source data")
        result=m.command_storage_index(self.root)
        self.assertEqual(len(result),1)
        self.assertEqual(result[0]["file_name"],"command_storage_core.dat")
        self.assertNotIn("very private",str(result))
    def test_refuses_symlinked_save_file(self):
        alias=self.root/"fake.dat"
        alias.symlink_to(self.root/"level.dat")
        with self.assertRaisesRegex(ValueError,"symlink"):
            m.read_gzip_nbt(alias)

if __name__=="__main__":
    unittest.main()
