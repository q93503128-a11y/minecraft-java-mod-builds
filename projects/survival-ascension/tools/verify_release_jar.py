#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
if len(sys.argv) != 2:
    raise SystemExit("usage: verify_release_jar.py <jar>")
jar = Path(sys.argv[1]).resolve()

subprocess.run([sys.executable, str(ROOT / "tools/verify_release_jar_pre_korean.py"), str(jar)], check=True)
subprocess.run([sys.executable, str(ROOT / "tools/verify_korean_runtime_pack.py"), str(jar)], check=True)

with zipfile.ZipFile(jar) as zf:
    final_gate = "kr/moonseungjun/survivalascension/endgame/FinalAscensionProgression.class"
    if final_gate not in zf.namelist():
        raise SystemExit(f"final ascension canonical gate missing: {final_gate}")
    compiled = zf.read(final_gate)
    for token in [b"REQUIRED_EXPEDITIONS", b"REQUIRED_APEX", b"missingRequirements", b"isReady"]:
        if token not in compiled:
            raise SystemExit(f"final ascension compiled gate token missing: {token!r}")

print("final_ascension_canonical_gate_runtime=present")
print("survival_ascension_release_verify=PASS")
