import importlib.util
import tempfile
import unittest
from pathlib import Path

p=Path(__file__).with_name("suppress_drehmal_gamemode_commands.py")
spec=importlib.util.spec_from_file_location("orig_gamemode",p)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def fixture(root="gamemode",dimension="minecraft:overworld",path="region/r.0.0.mca"):
    return {"level_sha256":m.LEVEL_SHA,"world_bytes_modified":False,
        "command_blocks":{"scanned_command_blocks":2232,
          "all_block_metadata":[{
            "root_operation":root,"effective_operation":"UNRECOGNIZED_gamemode",
            "region_file":path,"dimension":dimension,"chunk_slot":0,
            "coordinates":[8,65,9],"command_sha256":"a"*64,
            "command_actor":"minecraft:command_block","literal_function_ids":[]
          }]+[{"root_operation":"give"}]*2231}}

class GamemodeScopeTests(unittest.TestCase):
    def test_one_synthetic_pinned_direct_gamemode(self):
        a=m.parse_targets(fixture(),expected_count=1)
        self.assertEqual(len(a),1)
        self.assertEqual(a[0][:3],("region/r.0.0.mca",0,(8,65,9)))
    def test_structure_dimension_path_is_supported(self):
        a=m.parse_targets(fixture(dimension="minecraft:space",
            path="dimensions/minecraft/space/region/r.0.0.mca"),expected_count=1)
        self.assertEqual(len(a),1)
    def test_refuses_other_direct_command(self):
        with self.assertRaisesRegex(ValueError,"count"):
            m.parse_targets(fixture(root="tp"),expected_count=1)
    def test_refuses_untrusted_region_or_symbolic_parent(self):
        with self.assertRaisesRegex(ValueError,"dimension/region"):
            m.parse_targets(fixture(path="../region/r.0.0.mca"),expected_count=1)
    def test_original_count_gate_refuses_partial_source(self):
        with self.assertRaisesRegex(ValueError,"count"):
            m.parse_targets(fixture())
    def test_world_gate_refuses_unstaged_copy(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError,"non-activated"):
                m.validate_copy(Path(tmp))
    def test_report_never_contains_original_command_text(self):
        source=p.read_text(encoding="utf-8")
        self.assertNotIn("gamemode adventure @a",source)
        self.assertNotIn("write_text('TURNBOUND_DREHMAL_WORLD_V1'",source)
if __name__=="__main__":unittest.main()
