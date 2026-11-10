import importlib.util
import tempfile
import unittest
from pathlib import Path
path=Path(__file__).with_name("preview_drehmal_starter_discovery_commands.py")
spec=importlib.util.spec_from_file_location("starter_disc",path)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def record(op,resource,pos,region="region/r.51.-1.mca"):
    return {
        "operation":op,"dimension":"minecraft:overworld",
        "region_file":region,"coordinates":pos,"chunk_slot":0,
        "resource_id":resource,"target_class":"NEAREST_PLAYER" if op=="give" else "ALL_PLAYERS",
        "advancement_action":"grant" if op=="advancement" else None,
        "command_sha256":"a"*64,"actor":"minecraft:command_block",
        "approved_for_removal":False,"physical_coordinate_review":"KNOWN"
    }

def complete_fixture():
    source=[]
    for i,resource in enumerate(("minecraft:white_bed","minecraft:bread","minecraft:stone_pickaxe","minecraft:leather_boots")):
        number=3 if i==0 else 6
        for j in range(number):
            r=record("give",resource,[26457+j,207+i,-260])
            r["chunk_slot"]=i*16+j
            r["command_sha256"]=f"{i*16+j+1:064x}"
            source.append(r)
    for i,(resource,region) in enumerate(m.EXPECTED_ADVANCEMENTS.items()):
        r=record("advancement",resource,[100+i,50,200+i],region)
        r["command_sha256"]=f"{i+110:064x}"
        source.append(r)
    return {"physical_candidates":source}

class StarterDiscoveryTests(unittest.TestCase):
    def test_exact_reviewed_source_count(self):
        rows=m.select_candidates(complete_fixture())
        self.assertEqual(len(rows),24)
        self.assertEqual(len({x[0] for x in rows}),4)
    def test_missing_starter_item_fails_closed(self):
        data=complete_fixture()
        data["physical_candidates"].pop(0)
        with self.assertRaisesRegex(ValueError,"drifted"):
            m.select_candidates(data)
    def test_different_item_fails_closed(self):
        data=complete_fixture()
        data["physical_candidates"][0]["resource_id"]="minecraft:ender_eye"
        with self.assertRaisesRegex(ValueError,"drifted"):
            m.select_candidates(data)
    def test_discovery_wrong_target_fails_closed(self):
        data=complete_fixture()
        data["physical_candidates"][-1]["target_class"]="NEAREST_PLAYER"
        with self.assertRaisesRegex(ValueError,"Unexpected original discovery"):
            m.select_candidates(data)
    def test_unverified_coordinates_refused(self):
        data=complete_fixture()
        data["physical_candidates"][0]["physical_coordinate_review"]="MISSING_NBT_COORDS_NO_PATCH"
        with self.assertRaisesRegex(ValueError,"not safe"):
            m.select_candidates(data)
    def test_unstaged_world_refused(self):
        with tempfile.TemporaryDirectory() as root:
            with self.assertRaisesRegex(ValueError,"non-activated"):
                m.validate_copy(Path(root))
    def test_no_approved_safe_to_remove_claim(self):
        self.assertIn("NOT_PLAYTESTED",m.REVIEW_STATUS)
        self.assertFalse(m.REVIEW_STATUS.startswith("SAFE"))
if __name__=="__main__":
    unittest.main()
