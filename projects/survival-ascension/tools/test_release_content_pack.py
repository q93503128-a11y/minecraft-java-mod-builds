#!/usr/bin/env python3
from __future__ import annotations

import contextlib
import io
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
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


# Preserve the complete 0.58 content-pack regression contract while advancing only release identity.
legacy_path = ROOT / "tools/test_release_content_pack_058.py"
legacy = legacy_path.read_text(encoding="utf-8")
legacy = legacy.replace('REQUIRED_LOCK_VERSION = "0.58.0-alpha.1-content-preview.1"',
                        'REQUIRED_LOCK_VERSION = "0.59.0-alpha.1-content-preview.1"')
legacy = legacy.replace("'Mod version: `0.48.0-alpha.1`', 'Mod version: `0.58.0-alpha.1`'",
                        "'Mod version: `0.48.0-alpha.1`', 'Mod version: `0.59.0-alpha.1`'")
namespace = {"__file__": str(legacy_path), "__name__": "__main__"}
buffer = io.StringIO()
exit_code = 0
try:
    with contextlib.redirect_stdout(buffer):
        exec(compile(legacy, str(legacy_path), "exec"), namespace)
except (SystemExit, AssertionError) as exc:
    exit_code = int(exc.code or 0) if isinstance(exc, SystemExit) else 1
    if not isinstance(exc, SystemExit):
        print(f"0.58 content-pack regression assertion: {exc}", file=sys.stderr)
print(buffer.getvalue(), end="")
if exit_code != 0:
    print("RELEASE CONTENT-PACK AUDIT FAIL: 0.58 regression contract failed under 0.59 identity")
    sys.exit(exit_code)

bridge = read("src/main/java/kr/moonseungjun/survivalascension/compat/ApexContentPackBridge.java")
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
apex0 = read("src/main/resources/data/survivalascension/tags/entity_type/apex_escorts_tier_0.json")
apex1 = read("src/main/resources/data/survivalascension/tags/entity_type/apex_escorts_tier_1.json")
apex2 = read("src/main/resources/data/survivalascension/tags/entity_type/apex_escorts_tier_2.json")

need(bridge, [
    "APEX_ESCORTS_TIER_0", "APEX_ESCORTS_TIER_1", "APEX_ESCORTS_TIER_2",
    "randomEscortId", "escortIds", "apex_escort_tier_", "Tags.EntityTypes.BOSSES"
], "0.59 data-driven Apex escort bridge")
need(apex, [
    "ApexContentPackBridge.randomEscortId", "archetype.aquatic() ? null", "packSlot",
    "if (escort == null && packEscort)", "escort.setGlowingTag(true)", "hunt.packEscortCount++",
    "이변 호위 1체 포함"
], "0.59 bounded Apex escort replacement runtime")
need(apex0, ["tbos:armillary_scout", '"required": false'], "0.59 stage-0 Apex escort allowlist")
need(apex1, ["tbos:armillary_scout", "tbos:blank_chronist", '"required": false'], "0.59 stage-1 Apex escort allowlist")
need(apex2, ["tbos:blank_chronist", "tbos:gnomon_knight", '"required": false'], "0.59 stage-2 Apex escort allowlist")
if "tbos:minotaur" in apex0 + apex1 + apex2:
    errors.append("0.59 Apex escort safety: tbos:minotaur must stay out of mixed Apex allowlists")
for forbidden in ("tbos:hour_cantor", "tbos:phoenix_guardian"):
    if forbidden in apex0 + apex1 + apex2:
        errors.append(f"0.59 Apex escort safety: boss {forbidden} must stay out of escort allowlists")

if errors:
    print("RELEASE CONTENT-PACK AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("apex_optional_escort_replacement_bridge=PASS")
print("RELEASE CONTENT-PACK AUDIT PASS")
