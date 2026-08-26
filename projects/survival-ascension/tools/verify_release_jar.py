#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
if len(sys.argv) != 2:
    raise SystemExit("usage: verify_release_jar.py <jar>")
jar = Path(sys.argv[1]).resolve()

subprocess.run([sys.executable, str(ROOT / "tools/verify_release_jar_pre_korean.py"), str(jar)], check=True)
subprocess.run([sys.executable, str(ROOT / "tools/verify_korean_runtime_pack.py"), str(jar)], check=True)
print("survival_ascension_release_verify=PASS")
