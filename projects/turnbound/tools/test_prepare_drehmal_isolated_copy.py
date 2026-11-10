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
        with self.assertRaisesRegex(ValueError,"Only staged world copies are allowed"):
            m.verify(self.world)
    def test_restore_always_reverses_schedule_before_root_and_weapon(self):
        # A fake implementation captures order; no original content is edited.
        order=[]
        prior=m.verify
        rs=(m.saved.restore,m.towers.restore,m.roots.restore,m.weapons.restore)
        base=m.baseline
        no=m.no_other_stage
        old_file_sha=m.saved.file_sha
        old_pack_sha=m.weapons.file_hash
        try:
            m.verify=lambda world: {"status":"VERIFIED"}
            m.saved.restore=lambda world:order.append("saved")
            m.towers.restore=lambda world:order.append("tower")
            m.roots.restore=lambda world:order.append("root")
            m.weapons.restore=lambda world:order.append("weapon")
            m.baseline=lambda world:world
            m.no_other_stage=lambda world:None
            m.saved.file_sha=lambda path:m.LEVEL_SHA
            m.weapons.file_hash=lambda path:m.ORIGINAL_PACK_SHA
            m.restore(self.world)
            self.assertEqual(order,["saved","tower","root","weapon"])
        finally:
            m.verify=prior
            m.saved.restore,m.towers.restore,m.roots.restore,m.weapons.restore=rs
            m.baseline=base
            m.no_other_stage=no
            m.saved.file_sha=old_file_sha
            m.weapons.file_hash=old_pack_sha
    def test_verify_cannot_claim_gameplay_success(self):
        self.assertFalse(m.inspect.__doc__=="PLAYTESTED")
        self.assertEqual(m.LEVEL_SHA,m.saved.LEVEL_SHA)
    def test_starter_preview_receipt_rejected_by_clean_baseline(self):
        (self.world/m.STARTER_RECEIPT).write_text("{}")
        with self.assertRaisesRegex(ValueError,"pre-existing"):
            m.no_other_stage(self.world)
    def test_sixth_stage_restored_before_gamemode_and_tower(self):
        order=[]
        verify,base,no=m.verify,m.baseline,m.no_other_stage
        funcs=(m.saved.restore,m.starters.restore,m.modes.restore,
               m.towers.restore,m.roots.restore,m.weapons.restore)
        sh,ph=m.saved.file_sha,m.weapons.file_hash
        (self.world/m.STARTER_RECEIPT).write_text("{}")
        (self.world/m.GAMEMODE_RECEIPT).write_text("{}")
        try:
            m.verify=lambda world: {"status":"VERIFIED"}
            m.saved.restore=lambda world:order.append("saved")
            m.starters.restore=lambda world,composite=False:order.append("starter") if composite else None
            m.modes.restore=lambda world:order.append("gamemode")
            m.towers.restore=lambda world:order.append("tower")
            m.roots.restore=lambda world:order.append("root")
            m.weapons.restore=lambda world:order.append("weapon")
            m.baseline=lambda world:world
            m.no_other_stage=lambda world:None
            m.saved.file_sha=lambda path:m.LEVEL_SHA
            m.weapons.file_hash=lambda path:m.ORIGINAL_PACK_SHA
            m.restore(self.world)
            self.assertEqual(order,["saved","starter","gamemode","tower","root","weapon"])
        finally:
            m.verify,m.baseline,m.no_other_stage=verify,base,no
            (m.saved.restore,m.starters.restore,m.modes.restore,
             m.towers.restore,m.roots.restore,m.weapons.restore)=funcs
            m.saved.file_sha,m.weapons.file_hash=sh,ph
    def test_no_activated_world_marker_in_tool_output_contract(self):
        source=path.read_text(encoding="utf-8")
        self.assertNotIn('write_text("TURNBOUND_DREHMAL_WORLD_V1"',source)
        self.assertIn('"map_separation_complete":False',source)

if __name__=="__main__":
    unittest.main()
