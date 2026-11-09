import copy
import importlib.util
import json
import unittest
from pathlib import Path
p=Path(__file__).with_name("preview_drehmal_weapon_rewards.py")
spec=importlib.util.spec_from_file_location("weapon_preview",p)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def fixture():
    file="data/weapons/advancements/example.json"
    callback="weapons:example"
    graph={"world_level_dat_sha256":m.LEVEL_SHA,"functions":[
        {"id":callback,"pack":m.PACK,"called_by":[],"calls":[],
         "reachable_from_load_or_tick":False}]}
    aux={"level_sha256":m.LEVEL_SHA,"world_bytes_modified":False,
         "datapack_json_and_schedule":{"advancement_reward_records":[{
         "file":file,"reward_function_id":callback,"pack":m.PACK,
         "reward_field_names":["function","recipes"],
         "criteria_triggers":["minecraft:inventory_changed"],"file_sha256":"a"*64}]}}
    return aux,graph

class PreviewTests(unittest.TestCase):
    def test_manifest_accepts_only_original_weapon_reward(self):
        aux,graph=fixture()
        self.assertEqual(len(m.manifest(aux,graph,count=1)),1)
    def test_no_other_paths_accidentally_selected(self):
        aux,graph=fixture()
        aux["datapack_json_and_schedule"]["advancement_reward_records"][0]["file"]="data/core/advancements/example.json"
        with self.assertRaisesRegex(ValueError,"Expected 1"):
            m.manifest(aux,graph,count=1)
    def test_rejects_functions_with_other_callers(self):
        aux,graph=fixture()
        graph["functions"][0]["called_by"]=["core:init"]
        with self.assertRaisesRegex(ValueError,"manual review"):
            m.manifest(aux,graph,count=1)
    def test_rejects_callback_drift(self):
        aux,graph=fixture()
        aux["datapack_json_and_schedule"]["advancement_reward_records"][0]["reward_function_id"]="core:other"
        with self.assertRaisesRegex(ValueError,"unexpected"):
            m.manifest(aux,graph,count=1)
    def test_rejects_untrusted_world_report(self):
        aux,graph=fixture()
        aux["world_bytes_modified"]=True
        with self.assertRaisesRegex(ValueError,"Unverified"):
            m.manifest(aux,graph,count=1)
if __name__=="__main__":unittest.main()
