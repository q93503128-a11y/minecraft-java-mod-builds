#!/usr/bin/env python3
"""Fail-closed tests of original-Drehmal datapack toggle (disposable copies only)."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import probe_drehmal_pack_toggle_runtime as p

class PackBoundaryControls(unittest.TestCase):
    def test_validates_original_pack_before_and_after(self):
        original="There are 2 data packs enabled: [vanilla], [file/hi_drehmal.zip]"
        vanilla="There is 1 data pack enabled: [vanilla]"
        self.assertTrue(p.validate_pack_change(original,original,"kept"))
        self.assertTrue(p.validate_pack_change(original,vanilla,"disabled"))
        for before,after,mode in (
            (vanilla,vanilla,"disabled"),
            (original,original,"disabled"),
            (original,vanilla,"kept"),
            (original,original,"random"),
        ):
            with self.subTest(before=before,after=after,mode=mode):
                with self.assertRaisesRegex(ValueError,"pack|mode"):
                    p.validate_pack_change(before,after,mode)

    def test_live_server_switch_uses_explicit_rcon_on_disposable_copy_only(self):
        class Fake:
            def __init__(self): self.commands=[]
            def command(self,cmd):
                self.commands.append(cmd)
                if cmd=="datapack list available":
                    return "There is 1 data pack available: [file/hi_drehmal.zip]"
                if cmd=='datapack disable "file/hi_drehmal.zip"':
                    return "Disabled data pack [file/hi_drehmal.zip]"
                if cmd=="datapack list enabled":
                    return ("There are 2 data packs enabled: [vanilla], [file/hi_drehmal.zip]"
                             if len(self.commands)==1 else
                             "There is 1 data pack enabled: [vanilla]")
                raise AssertionError(cmd)
        f=Fake()
        with patch.object(p.time,"sleep"):
            data=p.pack_mode(f,"disabled")
        self.assertTrue(data["pack_list_verified_after_reload"])
        self.assertTrue(data["pack_disabled_on_disposable_copy_only"])
        self.assertFalse(data["pack_removed_from_copy"])
        self.assertIn('datapack disable "file/hi_drehmal.zip"',f.commands)

    def test_refuses_to_open_activated_or_unverified_map(self):
        with tempfile.TemporaryDirectory() as root:
            original=Path(root)/"official"
            copied=Path(root)/"copy"
            original.mkdir();copied.mkdir()
            with patch.object(p.old,"probe_preflight",return_value=copied),patch.object(
                    p.geo,"check_official_source",return_value=True),patch.object(
                    p,"World",return_value=object()),patch.object(
                    p.geo,"footprint",return_value={
                        "standing_space":"AIRBORNE_NO_FLOOR","ground":"minecraft:air",
                        "feet":"minecraft:air","head":"minecraft:air"}):
                with self.assertRaisesRegex(ValueError,"no longer matches"):
                    p.verify_sites(original,copied)
            with self.assertRaises(ValueError):
                p.probe(Path(root),original,copied,"invalid")

    def test_rcon_denial_blocks_test(self):
        class Rejected:
            def __init__(self):self.i=0
            def command(self,cmd):
                self.i+=1
                if self.i==1:return "[file/hi_drehmal.zip]"
                if cmd.startswith("datapack disable"):return "Failed to disable data pack"
                raise AssertionError(cmd)
        with self.assertRaisesRegex(ValueError,"refused"):
            p.pack_mode(Rejected(),"disabled")

if __name__=="__main__":
    unittest.main()
