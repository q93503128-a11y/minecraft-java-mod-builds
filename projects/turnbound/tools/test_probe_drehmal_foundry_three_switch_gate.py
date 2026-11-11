import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
import probe_drehmal_foundry_three_switch_gate as test

class FoundryThreeSwitchTests(unittest.TestCase):
    def test_barrier_category_differential_does_not_claim_door_traversal(self):
        before={(1,2,3):"minecraft:basalt",(2,2,3):"minecraft:bedrock",
                (3,2,3):"minecraft:stone"}
        after={(3,2,3):"minecraft:dirt"}
        result=test.gate_diff(before,after)
        self.assertEqual(result["changed_block_ids"],3)
        self.assertEqual(result["basalt_or_bedrock_removed_to_air"],2)
        self.assertFalse(result["actual_foundry_door_traversal_verified"])

    def test_unchanged_door_region_does_not_prove_failure(self):
        old={(1,2,3):"minecraft:stone"}
        result=test.gate_diff(old,old)
        self.assertEqual(result["changed_block_ids"],0)
        self.assertFalse(result["causality_without_independent_control_proven"])

    def test_foundry_inventory_only_reports_derived_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/"demo.zip"
            with zipfile.ZipFile(path,"w") as z:
                z.writestr("data/foundry/functions/arena/wave_1.mcfunction",
                           "summon minecraft:zombie\nscoreboard players add @a wave 1")
                z.writestr("data/other/functions/f.mcfunction","say test")
            result=test.foundry_function_inventory(path)
            self.assertEqual(result["original_foundry_named_script_count"],1)
            self.assertEqual(result["wave_or_arena_named_function_ids_total"],1)
            self.assertFalse(result["identified_actual_wave_activation"])
            self.assertFalse(result["original_function_content_exported"])

    def test_three_lever_manifest_and_search_boundary(self):
        self.assertEqual(len(test.LEVERS),3)
        self.assertEqual(test.GATE_HINT,(-3276,157,1500))
        self.assertLessEqual(test.GATE_XZ_RADIUS,48)

if __name__=="__main__":
    unittest.main()
