#!/usr/bin/env python3
"""Current non-mutating Arcane Circle contract entry point.

Historical version-specific validators remain as archival references, but the canonical
contract must follow the committed runtime instead of rewriting source or pinning an old alpha.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]

subprocess.run(
    [sys.executable, str(ROOT / "tools/test_v0121_alpha19_runtime.py")],
    cwd=ROOT,
    check=True,
)

print("Arcane Circle current magic contract: PASS (alpha.19, non-mutating)")
