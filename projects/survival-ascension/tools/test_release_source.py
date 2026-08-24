#!/usr/bin/env python3
from __future__ import annotations

import contextlib
import io
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
BASELINE_VERSION = "0.48.0-alpha.1"
REQUIRED_VERSION = "0.50.0-alpha.1"
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


def ordered(text: str, needles: list[str], label: str) -> None:
    pos = -1
    for needle in needles:
        next_pos = text.find(needle, pos + 1)
        if next_pos < 0:
            errors.append(f"{label} missing/order: {needle}")
            return
        pos = next_pos


props = read("gradle.properties")
version = next((line.split("=", 1)[1].strip() for line in props.splitlines() if line.startswith("mod_version=")), "")
if version != REQUIRED_VERSION:
    errors.append(f"release version drifted: expected {REQUIRED_VERSION}, got {version or '<missing>'}")

freight = read("src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java")
production_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(freight, [
    'FRONTLINE_KEY = "survivalascension_freight_frontline"',
    "FRONTLINE_FOOD = 176", "FRONTLINE_IRON = 56", "FRONTLINE_FUEL = 8",
    "FRONTLINE_LOGS = 32", "FRONTLINE_STONE_BRICKS = 128",
    "load(player, level, outpost, cart, player.isShiftKeyDown())",
    "checkFrontlineBundle(player, source)", "moveFrontlineBundleInto(source, cart)",
    "moveMatchingInto(List<Container> sources, Container target", "candidates.sort(Comparator.comparingInt",
    "int rollback = moveBulkOut(cart, source)", "if (!isEmpty(cart))",
    "data.putInt(FRONTLINE_KEY, frontlineManifest ? 1 : 0)", "data.remove(FRONTLINE_KEY)",
    "원정1 + 전초방어1 + 요새방어1회분"
], "0.49 frontline freight manifest")
forbid(freight, [
    "SavedData", "setChunkForced", "addRegionTicket", "getChunk(", "addFreshEntity", "teleportTo", "randomTeleport",
    "consumeSupplyCharge", "giveOrDrop", "award(", "biomesoplenty:", "tbos:", "amethyst_resonance:"
], "0.49 freight physical-only policy")
need(production_ui, [
    "일반=대량 · Shift=전선묶음(원정/방어/요새 각1)", "레일6+·동력레일·호퍼·제어",
    "일반/전선 화물", "한도3→토목6→중추9"
], "0.49/0.50 production UI")
need(guide, [
    'h("전선 화물 묶음")', 'h("전선 화물 적재")',
    "식량176+철56+석탄/목탄8+통나무32+석재벽돌128",
    "Shift 없이 선택하면 기존 일반 대량화물 적재입니다.",
    "전체 묶음이 출발 전초에 없으면 아무것도 적재하지 않습니다.",
    "전선 묶음 표식도 수레 자체 NBT에만",
    "산업 단계 3개", "토목 공사소 완공 후 6개", "승천 중추 완공 후 9개"
], "0.49/0.50 in-game guide")

# 0.50 regional logistics scale. Persisted IDs remain v1; runtime admission grows with existing infrastructure.
depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
outpost_data = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostData.java")
field_service = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
outpost_service = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java")
need(depot_data, [
    "BASE_DEPOTS_PER_PLAYER = 3", "CIVIL_DEPOTS_PER_PLAYER = 6", "MAX_DEPOTS_PER_PLAYER = 9",
    "registrationLimit(ServerPlayer player)", "InfrastructureProject.CIVIL_WORKS", "InfrastructureProject.ASCENSION_NEXUS",
    "return add(player, dimension, pos, registrationLimit(player));", "int maxAllowed", "own.size() >= limit",
    '"field_depots_v1"'
], "0.50 regional depot scale")
need(outpost_data, [
    "MAX_OUTPOSTS_PER_PLAYER = FieldDepotData.MAX_DEPOTS_PER_PLAYER",
    "return upgrade(player, dimension, pos, FieldDepotData.registrationLimit(player));",
    "int maxAllowed", "state(player).size() >= limit", '"outpost_v1"'
], "0.50 regional outpost scale")
need(field_service, [
    "int depotLimit = FieldDepotData.registrationLimit(player);",
    "depots.count(player) >= depotLimit",
    "depots.add(player, dimension, barrel, depotLimit)",
    "등록 거점 §e\" + depots.size() + \"/\" + depotLimit",
    "지역 한도: 산업 3 · 토목 6 · 승천 중추 9"
], "0.50 field-depot admission flow")
need(outpost_service, [
    "int outpostLimit = FieldDepotData.registrationLimit(player);",
    "outposts.count(player) >= outpostLimit",
    "outposts.upgrade(player, dimension, anchor, outpostLimit)",
    "승격 §e\" + data.count(player) + \"/\" + outpostLimit",
    "지역 한도: 산업 3 · 토목 6 · 승천 중추 9"
], "0.50 outpost admission flow")
ordered(outpost_service, [
    "int outpostLimit = FieldDepotData.registrationLimit(player);",
    "if (outposts.count(player) >= outpostLimit)",
    "StructureCheck check = inspectStructure",
    "FieldDepotService.consume(player, Items.IRON_INGOT",
    "production.consumeSupplyCharges",
    "outposts.upgrade(player, dimension, anchor, outpostLimit)"
], "0.50 outpost limit before physical-cost mutation")
forbid(depot_data + outpost_data, ["field_depots_v2", "outpost_v2"], "0.50 saved-data migration")

readme = read("README.md")
project = read("PROJECT.md")
changelog = read("CHANGELOG.md")
need(readme, ["0.50.0-alpha.1", "Regional Logistics Scale", "3", "6", "9", "0.49.0-alpha.1", "Frontline Freight Manifest"], "0.50 README")
need(project, ["Mod version: `0.50.0-alpha.1`", "0.50 Regional Logistics Scale", "0.49 Frontline Freight Manifest", "field_depots_v1", "outpost_v1"], "0.50 PROJECT")
need(changelog, ["## 0.50.0-alpha.1", "## 0.49.0-alpha.1", "3 -> 6 -> 9", "no new SavedData"], "0.50 CHANGELOG")

if errors:
    print("RELEASE SOURCE AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

# Preserve the full older regression contract, changing only the intentionally superseded permanent max3 assertion.
baseline_path = ROOT / "tools/test_current_source.py"
baseline = baseline_path.read_text(encoding="utf-8")
baseline = baseline.replace(BASELINE_VERSION, REQUIRED_VERSION)
baseline = baseline.replace("MAX_DEPOTS_PER_PLAYER = 3", "MAX_DEPOTS_PER_PLAYER = 9")
namespace = {"__file__": str(baseline_path), "__name__": "__main__"}
buffer = io.StringIO()
exit_code = 0
try:
    with contextlib.redirect_stdout(buffer):
        exec(compile(baseline, str(baseline_path), "exec"), namespace)
except SystemExit as exc:
    exit_code = int(exc.code or 0)

print(buffer.getvalue(), end="")
if exit_code != 0:
    print("RELEASE SOURCE AUDIT FAIL: baseline regression contract failed")
    sys.exit(exit_code)

print("RELEASE SOURCE AUDIT PASS")
print("- 0.49 Shift-select freight loads exactly one expedition + one outpost defense + one bastion local-supply manifest from the exact physical departure warehouse")
print("- normal bulk freight remains unchanged; frontline manifest remains physical-cart NBT only and rolls back on slot-layout failure")
print("- 0.50 depot/outpost persistence cap is nine while runtime registration/promotion is gated 3 -> 6 -> 9 by Industrial/Civil/Nexus progression")
print("- 0.50 outpost cap is validated before structure-cost, physical-material and supply-charge mutation, preventing limit-reached resource loss")
print("- README / PROJECT / CHANGELOG / in-game guide are synchronized to the 0.49 and 0.50 player flow")
