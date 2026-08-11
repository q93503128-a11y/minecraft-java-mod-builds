#!/usr/bin/env python3
"""Current non-mutating Arcane Circle contract entry point.

Historical version-specific validators remain archival references; this entry point always
validates the committed current runtime without rewriting source.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]

subprocess.run(
    [sys.executable, str(ROOT / "tools/test_v0121_alpha21_presentation_phase1.py")],
    cwd=ROOT,
    check=True,
)

print("Arcane Circle current magic contract: PASS (alpha.21 presentation phase 1, non-mutating)")
