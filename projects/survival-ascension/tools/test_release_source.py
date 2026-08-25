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


def forbid(text: str, needles: list[str], label: str) -> None:
    for needle in needles:
        if needle in text:
            errors.append(f"{label} forbidden: {needle}")


# Preserve the complete 0.58 release regression contract and adapt only the current release identity.
legacy_path = ROOT / "tools/test_release_source_058.py"
legacy = legacy_path.read_text(encoding="utf-8")
legacy = legacy.replace('REQUIRED_VERSION = "0.58.0-alpha.1"', 'REQUIRED_VERSION = "0.59.0-alpha.1"')
legacy = legacy.replace(r'VERSION = \"0.58.0-alpha.1\"', r'VERSION = \"0.59.0-alpha.1\"')
legacy = legacy.replace('Mod version: `0.58.0-alpha.1`', 'Mod version: `0.59.0-alpha.1`')
namespace = {"__file__": str(legacy_path), "__name__": "__main__"}
buffer = io.StringIO()
exit_code = 0
try:
    with contextlib.redirect_stdout(buffer):
        exec(compile(legacy, str(legacy_path), "exec"), namespace)
except (SystemExit, AssertionError) as exc:
    exit_code = int(exc.code or 0) if isinstance(exc, SystemExit) else 1
    if not isinstance(exc, SystemExit):
        print(f"0.58 regression assertion: {exc}", file=sys.stderr)
print(buffer.getvalue(), end="")
if exit_code != 0:
    print("RELEASE SOURCE AUDIT FAIL: 0.58 regression contract failed under 0.59 identity")
    sys.exit(exit_code)

props = read("gradle.properties")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
bridge = read("src/main/java/kr/moonseungjun/survivalascension/compat/ApexContentPackBridge.java")
project = read("PROJECT.md")
readme = read("README.md")
changelog = read("CHANGELOG.md")
testing = read("TESTING.md")

need(props, ["mod_version=0.59.0-alpha.1"], "0.59 release identity")
need(main, [
    'VERSION = "0.59.0-alpha.1"', "ApexContentPackBridge::onServerStarted",
    "data-driven Apex content escorts"
], "0.59 runtime wiring")
need(bridge, [
    "APEX_ESCORTS_TIER_0", "APEX_ESCORTS_TIER_1", "APEX_ESCORTS_TIER_2",
    "randomEscortId(RandomSource random, int worldStage)", "escortIds(int worldStage)",
    "type.getCategory() != MobCategory.MONSTER", "Tags.EntityTypes.BOSSES", "apex_escort_tier_"
], "0.59 optional Apex escort compatibility")
need(apex, [
    "ApexContentPackBridge.randomEscortId", "archetype.aquatic() ? null", "packSlot",
    "if (escort == null && packEscort)", "escort.setGlowingTag(true)", "hunt.packEscortCount++",
    "이변 호위 1체 포함"
], "0.59 Apex escort replacement")
forbid(bridge, ["tbos:", "com.nightbeam", "setChunkForced", "addRegionTicket", "getChunk("],
       "0.59 optional-content/force-load policy")
need(project, ["Mod version: `0.59.0-alpha.1`", "## 0.59 Apex Content Escort Integration"], "0.59 PROJECT docs")
need(readme, ["## 0.59.0-alpha.1", "정점 사냥", "호위 수 자체를 늘리지"], "0.59 README docs")
need(changelog, ["## 0.59.0-alpha.1", "apex_escorts_tier_0", "0.59.0-alpha.1-content-preview.1"], "0.59 CHANGELOG docs")
need(testing, ["## 0.59 focused checks", "이변 호위", "Ocean"], "0.59 manual test matrix")

if errors:
    print("RELEASE SOURCE AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("apex_optional_escort_replacement=PASS")
print("apex_optional_java_dependency=ABSENT")
print("apex_escort_count_inflation=ABSENT")
print("RELEASE SOURCE AUDIT PASS")
