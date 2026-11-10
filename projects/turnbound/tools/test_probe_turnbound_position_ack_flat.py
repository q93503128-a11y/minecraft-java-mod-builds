#!/usr/bin/env python3
"""Minimal safe control gates for disposable flat-world protocol experiment."""
import tempfile
import unittest
from pathlib import Path
import probe_turnbound_position_ack_flat as p

class FlatProtocolControlTest(unittest.TestCase):
    def test_horizontal_not_vertical(self):
        self.assertEqual(0,p.summary_delta((80.5,25,80.5),(80.5,120,80.5)))
        self.assertEqual(100,p.summary_delta((80.5,120,180.5),(80.5,120,80.5)))

    def test_refuses_overwriting_any_unknown_run(self):
        with tempfile.TemporaryDirectory() as d:
            project=Path(d)
            (project/"run").mkdir()
            with self.assertRaisesRegex(ValueError,"existing"):
                p.create_run(project)

    def test_two_modes_must_have_different_ci_names(self):
        self.assertEqual(2,len(p.TARGETS))
        self.assertNotEqual(*p.TARGETS.values())

if __name__=="__main__":unittest.main()
