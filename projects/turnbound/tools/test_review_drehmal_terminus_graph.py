import importlib.util
import unittest
from pathlib import Path

s = importlib.util.spec_from_file_location("terminus", Path(__file__).with_name("review_drehmal_terminus_graph.py"))
m = importlib.util.module_from_spec(s)
s.loader.exec_module(m)

def fixture():
    def f(i, calls):
        return {"id": i, "pack": "hi_drehmal.zip", "source_sha256": "a"*64,
                "calls": calls, "reachable_from_load_or_tick": False}
    fns = [
        f("terminus:teleport_to_tower/demo_teleport", ["players:gamemode/set_survival"]),
        f("players:gamemode/set_survival", []),
        f("terminus:teleport_from_tower/demo_terminus", ["terminus:unlock_tower/demo_unlock"]),
        f("terminus:unlock_tower/demo_unlock", ["terminus:count_towers"]),
        f("terminus:count_towers", ["terminus:gifts/menu/main"]),
        f("terminus:gifts/menu/main", [])
    ]
    return {"map_release": "Drehmal: APOTHEOSIS v2.2.2f", "world_level_dat_sha256":m.LEVEL_SHA,
            "root_tags":[{"id":"#minecraft:load","values":["core:init"]},
                         {"id":"#minecraft:tick","values":["core:main","entities:main"]}],
            "functions": fns,"function_count":len(fns),
            "region_command_block_scan":{"command_block_count":20,"warnings":["short header"],
                "warnings_truncated":2, "function_references":{
                    "terminus:teleport_to_tower/demo_teleport": 1,
                    "terminus:teleport_from_tower/demo_terminus":1
                }}}

class TerminusAnalysisTest(unittest.TestCase):
    def test_survival_and_gift_indirect_dependencies(self):
        x=m.analyze(fixture())
        items={v["id"]:v for v in x["review"]}
        self.assertEqual(items["terminus:teleport_to_tower/demo_teleport"]["risk_paths"]["players:gamemode/set_survival"][-1],"players:gamemode/set_survival")
        self.assertEqual(items["terminus:teleport_from_tower/demo_terminus"]["risk_paths"]["terminus:gifts/"][-1],"terminus:gifts/menu/main")
        self.assertEqual(x["region_warnings_lower_bound"],3)
        self.assertEqual(x["review"][0]["decision"],"NOT_APPROVED_FOR_REMOVAL")
    def test_malformed_or_mismatched_source_fails_closed(self):
        x=fixture()
        x["world_level_dat_sha256"]="no"
        with self.assertRaisesRegex(ValueError,"Unsupported"):m.analyze(x)
        x=fixture()
        x["functions"].append(x["functions"][0])
        x["function_count"]+=1
        with self.assertRaisesRegex(ValueError,"Duplicate"):m.analyze(x)
    def test_missing_refs_reported(self):
        x=fixture()
        x["region_command_block_scan"]["function_references"]["nonexistent:foo"]=3
        self.assertEqual(m.analyze(x)["unresolved"],["nonexistent:foo"])

if __name__=="__main__":
    unittest.main()
