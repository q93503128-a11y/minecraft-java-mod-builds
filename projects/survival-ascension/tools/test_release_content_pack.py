#!/usr/bin/env python3
from __future__ import annotations

import contextlib
import io
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
BASELINE_LOCK_VERSION = "0.48.0-alpha.1-content-preview.1"
REQUIRED_LOCK_VERSION = "0.49.0-alpha.1-content-preview.1"
errors: list[str] = []


def read(rel: str) -> str:
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing: {rel}")
        return ""
    return path.read_text(encoding="utf-8")


def need(text: str, needles: list[str], label: str) -> None:
    for needle in needles:
        if needle not in text:
            errors.append(f"{label} missing: {needle}")


lock_text = read("modpack/content-lock.json")
try:
    lock = json.loads(lock_text)
except json.JSONDecodeError as exc:
    errors.append(f"invalid content lock JSON: {exc}")
    lock = {}
if lock.get("version") != REQUIRED_LOCK_VERSION:
    errors.append(f"content-pack version drifted: expected {REQUIRED_LOCK_VERSION}, got {lock.get('version')!r}")

freight = read("src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java")
ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(freight, [
    "FRONTLINE_FOOD = 176", "FRONTLINE_IRON = 56", "FRONTLINE_FUEL = 8",
    "FRONTLINE_LOGS = 32", "FRONTLINE_STONE_BRICKS = 128",
    "player.isShiftKeyDown()", "moveFrontlineBundleInto", "FRONTLINE_KEY"
], "0.49 frontline freight content-pack bridge")
need(ui, ["Shift=전선묶음", "레일6+·동력레일·호퍼·제어"], "0.49 frontline freight player flow")

if errors:
    print("RELEASE CONTENT-PACK AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

baseline_path = ROOT / "tools/test_content_pack_source.py"
baseline = baseline_path.read_text(encoding="utf-8")
baseline = baseline.replace(BASELINE_LOCK_VERSION, REQUIRED_LOCK_VERSION)
namespace = {"__file__": str(baseline_path), "__name__": "__main__"}
buffer = io.StringIO()
exit_code = 0
try:
    with contextlib.redirect_stdout(buffer):
        exec(compile(baseline, str(baseline_path), "exec"), namespace)
except (SystemExit, AssertionError) as exc:
    if isinstance(exc, SystemExit):
        exit_code = int(exc.code or 0)
    else:
        exit_code = 1
        print(f"baseline content-pack assertion: {exc}", file=sys.stderr)

print(buffer.getvalue(), end="")
if exit_code != 0:
    print("RELEASE CONTENT-PACK AUDIT FAIL: baseline regression contract failed")
    sys.exit(exit_code)

print("frontline_freight_manifest=PASS")
print("RELEASE CONTENT-PACK AUDIT PASS")
