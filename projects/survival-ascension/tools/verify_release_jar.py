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
    final_system = "kr/moonseungjun/survivalascension/endgame/FinalAscensionSystem.class"
    infrastructure = "kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.class"
    menu = "kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.class"
    for name in [final_gate, final_system, infrastructure, menu]:
        if name not in zf.namelist():
            raise SystemExit(f"final ascension runtime class missing: {name}")

    compiled_gate = zf.read(final_gate)
    for token in [b"REQUIRED_EXPEDITIONS", b"REQUIRED_APEX", b"missingRequirements", b"isReady"]:
        if token not in compiled_gate:
            raise SystemExit(f"final ascension compiled gate token missing: {token!r}")

    compiled_system = zf.read(final_system)
    for token in [
        b"ACT1_MINING", b"ACT1_BUILD", b"ACT1_COMBAT", b"ACT1_MOVE",
        b"ACT2_SET_ONE", b"ACT2_SET_TWO", b"ACT2_SET_THREE",
        b"ACT3_SEAL_ONE", b"ACT3_SEAL_TWO", b"ACT3_SEAL_THREE",
        b"survivalascension_final_ascension_owner", b"SEAL_CHANNEL_TICKS"
    ]:
        if token not in compiled_system:
            raise SystemExit(f"final ascension compiled encounter token missing: {token!r}")

    compiled_infrastructure = zf.read(infrastructure)
    if b"final_ascension" not in compiled_infrastructure:
        raise SystemExit("final ascension server action missing from packaged infrastructure service")

    compiled_menu = zf.read(menu)
    if "최후의 승천".encode("utf-8") not in compiled_menu:
        raise SystemExit("final ascension menu label missing from packaged client runtime")

print("final_ascension_canonical_gate_runtime=present")
print("final_ascension_acts_1_3_runtime=present")
print("final_ascension_menu_action_runtime=present")
print("survival_ascension_release_verify=PASS")
