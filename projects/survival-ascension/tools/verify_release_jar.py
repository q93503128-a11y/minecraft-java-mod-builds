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

# Keep every existing packaged-JAR regression check and SHA generation.
subprocess.run([sys.executable, str(ROOT / "tools/verify_jar.py"), str(jar)], check=True)

with zipfile.ZipFile(jar) as zf:
    freight_name = "kr/moonseungjun/survivalascension/production/FreightService.class"
    ui_name = "kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.class"
    if freight_name not in zf.namelist() or ui_name not in zf.namelist():
        raise SystemExit("0.49 freight/UI runtime classes missing")
    freight = zf.read(freight_name)
    ui = zf.read(ui_name)
    for token in [
        b"survivalascension_freight_frontline",
        b"moveFrontlineBundleInto",
        b"checkFrontlineBundle",
        b"moveMatchingInto",
        b"FrontlineLoadResult",
    ]:
        if token not in freight:
            raise SystemExit(f"0.49 compiled frontline freight token missing: {token!r}")
    if b"CHEST_MINECART" not in ui or b"physical_freight" not in ui:
        raise SystemExit("0.49 compiled freight UI routing missing")

print("frontline_freight_manifest_runtime=present")
print("frontline_freight_release_verify=PASS")
