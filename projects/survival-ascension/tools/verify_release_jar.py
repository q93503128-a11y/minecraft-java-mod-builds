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

    depot_data_name = "kr/moonseungjun/survivalascension/production/FieldDepotData.class"
    outpost_data_name = "kr/moonseungjun/survivalascension/production/OutpostData.class"
    field_service_name = "kr/moonseungjun/survivalascension/production/FieldDepotService.class"
    outpost_service_name = "kr/moonseungjun/survivalascension/production/OutpostService.class"
    for name in [depot_data_name, outpost_data_name, field_service_name, outpost_service_name]:
        if name not in zf.namelist():
            raise SystemExit(f"0.50 regional logistics runtime class missing: {name}")

    depot_data = zf.read(depot_data_name)
    outpost_data = zf.read(outpost_data_name)
    field_service = zf.read(field_service_name)
    outpost_service = zf.read(outpost_service_name)
    for token in [
        b"BASE_DEPOTS_PER_PLAYER",
        b"CIVIL_DEPOTS_PER_PLAYER",
        b"MAX_DEPOTS_PER_PLAYER",
        b"registrationLimit",
        b"CIVIL_WORKS",
        b"ASCENSION_NEXUS",
    ]:
        if token not in depot_data:
            raise SystemExit(f"0.50 compiled regional depot token missing: {token!r}")
    if b"registrationLimit" not in outpost_data:
        raise SystemExit("0.50 compiled outpost dynamic-limit routing missing")
    for token in [b"registrationLimit", b"LIMIT_REACHED", b"add"]:
        if token not in field_service:
            raise SystemExit(f"0.50 compiled field-depot admission token missing: {token!r}")
    for token in [b"registrationLimit", b"consumeSupplyCharges", b"upgrade"]:
        if token not in outpost_service:
            raise SystemExit(f"0.50 compiled outpost pre-admission token missing: {token!r}")

print("frontline_freight_manifest_runtime=present")
print("frontline_freight_release_verify=PASS")
print("regional_logistics_scale_runtime=present")
print("regional_logistics_scale_release_verify=PASS")
