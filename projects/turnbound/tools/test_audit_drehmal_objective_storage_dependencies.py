import importlib.util
import collections
import unittest
from pathlib import Path

p=Path(__file__).with_name("audit_drehmal_objective_storage_dependencies.py")
spec=importlib.util.spec_from_file_location("score_deps",p)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

class SourceReferenceTest(unittest.TestCase):
    def test_score_read_write_and_operation_both_operands(self):
        cases=[
            ("scoreboard players set @s ai_state 1", ("ai_state","score_write")),
            ("scoreboard players operation @s room = #global stage", ("room","score_operation")),
            ("scoreboard players operation @s room = #global stage", ("stage","score_operation")),
            ("execute if score @s door = #const stage run setblock ~ ~ ~ air",("door","score_condition")),
            ("execute if score @s door = #const stage run setblock ~ ~ ~ air",("stage","score_condition")),
            ("execute store result score @s health run data get entity @s Health",("health","score_write")),
            ("execute as @e[scores={laser=1..,timer=3}] run playsound minecraft:test ambient @a",("laser","selector_condition")),
            ("execute as @e[scores={laser=1..,timer=3}] run playsound minecraft:test ambient @a",("timer","selector_condition")),
        ]
        for command,expected in cases:
            with self.subTest(command=command,expected=expected):
                self.assertIn(expected,m.parse_command(command)[0])

    def test_storage_reads_and_writes_identifiers_only(self):
        refs=m.parse_command("execute store result storage drehmal:door value int 1 run scoreboard players get @s stage")
        self.assertIn("drehmal:door",refs[1])
        self.assertIn(("stage","score_read"),refs[0])

    def test_unknown_and_comment_are_not_fabricated(self):
        self.assertEqual(m.parse_command("# scoreboard players set @a Foo 1"),([],[]))
        self.assertEqual(m.parse_command("say hello"),([],[]))

    def test_report_never_exports_commands_or_player_scores(self):
        saved={"scoreboard_objective_inventory":{"objectives":[{"objective_id":"door","criteria":"dummy"}]},
               "command_storage_files":[{"file_name":"command_storage_drehmal.dat","sha256":"a"*64,"bytes":10}]}
        e=collections.defaultdict(lambda:{"roles":collections.Counter(),"functions":set(),"blocks":set(),"function_structure":set(),"function_visual":set(),"function_player":set()})
        e["door"]["roles"]["score_write"]=1
        e["door"]["functions"].add("core:scene/door")
        e["door"]["function_structure"].add("core:scene/door")
        s=collections.defaultdict(lambda:{"functions":set(),"blocks":set(),"read_sites":0,"write_sites":0})
        s["drehmal:door"]["functions"].add("core:scene/door")
        report=m.summarize(saved,e,s,{"command_blocks":[],"warnings":[]},1)
        self.assertEqual(report["objectives"][0]["review"],"PRESERVE_UNTIL_MANUAL_MECHANISM_REVIEW")
        self.assertIn("drehmal:door",report["command_storage_files"][0]["referenced_storage_ids"])
        self.assertNotIn("scoreboard players set",str(report))
        self.assertFalse(report["world_modified"])

    def test_saved_catalog_unknown_names_not_safe_delete(self):
        saved={"scoreboard_objective_inventory":{"objectives":[{"objective_id":"unseen","criteria":"dummy"}]},
               "command_storage_files":[]}
        report=m.summarize(saved,{}, {},{"command_blocks":[],"warnings":["short"]},0)
        self.assertEqual(report["objectives"][0]["review"],"NO_SAFE_DELETE_INFERENCE")
        self.assertEqual(report["region_scan_warnings"],1)

if __name__=="__main__":
    unittest.main()
