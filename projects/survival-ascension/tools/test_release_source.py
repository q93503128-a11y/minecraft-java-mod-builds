#!/usr/bin/env python3
from __future__ import annotations

import contextlib
import io
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
BASELINE_VERSION = "0.48.0-alpha.1"
REQUIRED_VERSION = "0.51.0-alpha.1"
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

# 0.49 physical frontline freight remains intact.
freight = read("src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java")
production_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(freight, [
    'FRONTLINE_KEY = "survivalascension_freight_frontline"',
    "FRONTLINE_FOOD = 176", "FRONTLINE_IRON = 56", "FRONTLINE_FUEL = 8",
    "FRONTLINE_LOGS = 32", "FRONTLINE_STONE_BRICKS = 128",
    "load(player, level, outpost, cart, player.isShiftKeyDown())",
    "checkFrontlineBundle(player, source)", "moveFrontlineBundleInto(source, cart)",
    "int rollback = moveBulkOut(cart, source)", "data.putInt(FRONTLINE_KEY, frontlineManifest ? 1 : 0)"
], "0.49 frontline freight manifest")
forbid(freight, ["SavedData", "setChunkForced", "addRegionTicket", "getChunk(", "teleportTo", "randomTeleport"], "0.49 freight physical-only policy")
need(production_ui, ["Shift=전선묶음", "레일6+·동력레일·호퍼·제어", "한도3→토목6→중추9"], "0.49/0.50 production UI")

# 0.50 regional logistics scale remains physical and limit-first.
depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
outpost_data = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostData.java")
field_service = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
outpost_service = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java")
need(depot_data, [
    "BASE_DEPOTS_PER_PLAYER = 3", "CIVIL_DEPOTS_PER_PLAYER = 6", "MAX_DEPOTS_PER_PLAYER = 9",
    "registrationLimit(ServerPlayer player)", "InfrastructureProject.CIVIL_WORKS", "InfrastructureProject.ASCENSION_NEXUS",
    '"field_depots_v1"'
], "0.50 regional depot scale")
need(outpost_data, ["MAX_OUTPOSTS_PER_PLAYER = FieldDepotData.MAX_DEPOTS_PER_PLAYER", '"outpost_v1"'], "0.50 regional outpost scale")
ordered(outpost_service, [
    "int outpostLimit = FieldDepotData.registrationLimit(player);",
    "if (outposts.count(player) >= outpostLimit)",
    "StructureCheck check = inspectStructure",
    "FieldDepotService.consume(player, Items.IRON_INGOT",
    "production.consumeSupplyCharges",
    "outposts.upgrade(player, dimension, anchor, outpostLimit)"
], "0.50 outpost limit before physical-cost mutation")
forbid(depot_data + outpost_data, ["field_depots_v2", "outpost_v2"], "0.50 saved-data migration")

# 0.51 armor ascension: standard humanoid armor tags join the existing affix system.
affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
equipment_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java")
main_mod = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(affix, [
    "Category.ARMOR", "ARMOR(\"armor\")",
    "ItemTags.HEAD_ARMOR", "ItemTags.CHEST_ARMOR", "ItemTags.LEG_ARMOR", "ItemTags.FOOT_ARMOR",
    "armorDamageMultiplier(ServerPlayer player, float incomingAmount, boolean environmental)",
    "armorXpMultiplier(ServerPlayer player)", "Math.min(0.35D, reduction)", "Math.min(1.32D, 1.0D + bonus)",
    "List<EquipmentSlot> ARMOR_SLOTS", "EquipmentSlot.HEAD", "EquipmentSlot.CHEST", "EquipmentSlot.LEGS", "EquipmentSlot.FEET",
    "player.getItemBySlot(slot)", "Items.IRON_HELMET", "Items.DIAMOND_CHESTPLATE", "Items.NETHERITE_LEGGINGS", "Items.NETHERITE_BOOTS",
    "case ARMOR -> \"방어구\"", "case ARMOR -> \"불굴\"", "case ARMOR -> \"완강\"", "case ARMOR -> \"보호\""
], "0.51 armor affix progression")
forbid(affix, ["player.getArmorSlots()"], "0.51 obsolete armor-slot API")
need(combat, [
    "event.getEntity() instanceof ServerPlayer defender",
    "AscensionAffixes.armorDamageMultiplier(defender, event.getAmount(), environmental)",
    "AscensionAffixes.armorXpMultiplier(player)"
], "0.51 worn armor runtime")
need(reforge, ["검/곡괭이/도끼/삽/괭이/방어구 태그 장비"], "0.51 armor imprint server flow")
need(equipment_ui, ["검/곡괭이/도끼/삽/괭이/방어구 표준 태그 장비 필요"], "0.51 armor imprint UI")
need(main_mod, ["VERSION = \"0.51.0-alpha.1\"", "armor affix progression"], "0.51 runtime banner")
forbid(affix + combat, ["setChunkForced", "addRegionTicket", "getChunk("], "0.51 armor runtime world-loading policy")

# User-facing docs are part of the release contract, not an uncommitted CI-side patch.
project_doc = read("PROJECT.md")
readme = read("README.md")
changelog = read("CHANGELOG.md")
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(project_doc, [
    "Mod version: `0.51.0-alpha.1`",
    "## 0.51 Armor Ascension / 방어구 승천 성장",
    "hard-capped at 35%",
    "hard-capped at +32% Combat XP"
], "0.51 PROJECT docs")
need(readme, [
    "## 0.51.0-alpha.1 — Armor Ascension / 방어구 승천 성장",
    "capped at 35%",
    "capped at +32%"
], "0.51 README docs")
need(changelog, [
    "## 0.51.0-alpha.1",
    "Armor Ascension / 방어구 승천 성장",
    "35%",
    "+32%"
], "0.51 CHANGELOG docs")
need(guide, [
    'h("방어구 affix")',
    "최대35%",
    "최대32%"
], "0.51 in-game guide")

if errors:
    print("RELEASE SOURCE AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

# Preserve the older regression contract, adapting only version and the intentionally superseded permanent max3 assertion.
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
print("- 0.49 frontline freight manifest and physical-only transport remain intact")
print("- 0.50 depot/outpost registration remains staged 3 -> 6 -> 9 and limit-first before resource mutation")
print("- 0.51 standard humanoid armor tags join imprint/reforge/awakening and elite affix drops")
print("- 0.51 worn armor uses 26.2 equipment-slot API and bounded effects: damage cap35%, mastery XP cap32%")
print("- README / PROJECT / CHANGELOG / in-game guide are committed and synchronized to 0.51")
