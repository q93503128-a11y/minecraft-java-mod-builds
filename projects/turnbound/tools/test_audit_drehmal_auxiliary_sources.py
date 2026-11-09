import importlib.util
import tempfile
import unittest
from pathlib import Path
p=Path(__file__).with_name("audit_drehmal_auxiliary_sources.py")
s=importlib.util.spec_from_file_location("aux_source",p)
m=importlib.util.module_from_spec(s)
s.loader.exec_module(m)

class AuxiliaryTests(unittest.TestCase):
    def test_nonfunction_execute_is_not_misclassified_as_safe(self):
        a=m.command_shape("execute as @p at @s run scoreboard players add @s hp 1")
        self.assertEqual(a["root"],"execute")
        self.assertEqual(a["effective"],"scoreboard")
        self.assertEqual(a["function_refs"],[])
    def test_execute_function_reference(self):
        a=m.command_shape("execute if score @p test matches 1 run function core:scene/door")
        self.assertEqual(a["effective"],"function")
        self.assertEqual(a["function_refs"],["core:scene/door"])
    def test_schedule_function_is_detectable(self):
        a=m.command_shape("schedule function core:scene/door 10t replace")
        self.assertEqual(a["effective"],"schedule")
        self.assertEqual(a["function_refs"],["core:scene/door"])
    def test_empty_command_is_explicit(self):
        self.assertEqual(m.command_shape(" ")["root"],"EMPTY")
    def test_region_dimension(self):
        self.assertEqual(m.region_dimension("dimensions/minecraft/space/region/r.-1.0.mca"),"minecraft:space")
        self.assertEqual(m.region_dimension("region/r.0.0.mca"),"minecraft:overworld")
    def test_no_original_command_is_exported(self):
        with tempfile.TemporaryDirectory() as td:
            f=Path(td)/"region"/"r.0.0.mca";f.parent.mkdir()
            c="execute as @p run scoreboard players add @p secret_data 1234"
            raw={"files":1,"warnings":[],"command_blocks":[{
                "id":"minecraft:command_block","command":c,"file":str(f),"chunk_slot":0,
                "x":1,"y":2,"z":3}]}
            result=m.command_inventory(raw,Path(td))
            assert "secret_data" not in str(result)
            self.assertEqual(result["effective_operation_counts"]["scoreboard"],1)
if __name__=="__main__":unittest.main()
