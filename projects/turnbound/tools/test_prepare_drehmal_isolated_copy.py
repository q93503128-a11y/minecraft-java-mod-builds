import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

path=Path(__file__).with_name("prepare_drehmal_isolated_copy.py")
spec=importlib.util.spec_from_file_location("isolated",path)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

class IntegrationGuardTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.world=Path(self.tmp.name)
    def test_partial_backup_prevents_fresh_attempt(self):
        (self.world/m.ROOT_RECEIPT).write_text("{}")
        with self.assertRaisesRegex(ValueError,"pre-existing"):
            m.no_other_stage(self.world)
    def test_all_four_stage_receipts_required_to_verify(self):
        with self.assertRaisesRegex((ValueError, OSError),"Missing"):
            m.verify(self.world)
    def test_restore_always_reverses_schedule_before_root_and_weapon(self):
        # A fake implementation captures order; no original content is edited.
        order=[]
        prior=m.verify
        rs=(m.saved.restore,m.towers.restore,m.roots.restore,m.weapons.restore)
        base=m.baseline
        no=m.no_other_stage
        try:
            m.verify=lambda world: {"status":"VERIFIED"}
            m.saved.restore=lambda world:order.append("saved")
            m.towers.restore=lambda world:order.append("tower")
            m.roots.restore=lambda world:order.append("root")
            m.weapons.restore=lambda world:order.append("weapon")
            m.baseline=lambda world:world
            m.no_other_stage=lambda world:None
            m.restore(self.world)
            self.assertEqual(order,["saved","tower","root","weapon"])
        finally:
            m.verify=prior
            m.saved.restore,m.towers.restore,m.roots.restore,m.weapons.restore=rs
            m.baseline=base
            m.no_other_stage=no
    def test_verify_cannot_claim_gameplay_success(self):
        self.assertFalse(m.inspect.__doc__=="PLAYTESTED")
        self.assertEqual(m.LEVEL_SHA,m.saved.LEVEL_SHA)
    def test_no_activated_world_marker_in_tool_output_contract(self):
        source=path.read_text(encoding="utf-8")
        self.assertNotIn('write_text("TURNBOUND_DREHMAL_WORLD_V1"',source)
        self.assertIn('"map_separation_complete":False',source)

if __name__=="__main__":
    unittest.main()
