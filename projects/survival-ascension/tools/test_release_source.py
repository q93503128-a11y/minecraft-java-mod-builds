#!/usr/bin/env python3
from __future__ import annotations

import contextlib
import io
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
BASELINE_VERSION = "0.48.0-alpha.1"
REQUIRED_VERSION = "0.55.0-alpha.1"
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
need(reforge, ["검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비"], "0.52 ranged/armor imprint server flow")
need(equipment_ui, ["검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 표준 태그 장비 필요"], "0.52 ranged/armor imprint UI")
need(main_mod, ["VERSION = \"0.55.0-alpha.1\"", "ranged projectile snapshots/impact bursts"], "0.52 runtime banner")
forbid(affix + combat, ["setChunkForced", "addRegionTicket", "getChunk("], "0.51 armor runtime world-loading policy")

# 0.52 ranged combat ascension: launch-time snapshots and bounded physical impact scale.
need(affix, [
    "Tags.Items.TOOLS_BOW", "Tags.Items.TOOLS_CROSSBOW", "Category.RANGED", 'RANGED("ranged")',
    'RANGED_PROJECTILE = "survivalascension_ranged_projectile"',
    "snapshotRangedProjectile(Projectile projectile, ItemStack weapon, boolean precision)",
    "projectileDamageMultiplier", "projectileXpMultiplier", "projectileBurstRadiusBonus", "projectileBurstTargetBonus", "projectileBurstFractionBonus",
    "Math.min(1.25D", "Math.min(1.50D", "Math.min(1.5D", "Math.min(4", "Math.min(0.15D"
], "0.52 ranged affix/projectile snapshot")
need(combat, [
    "onEntityJoin(EntityJoinLevelEvent event)", "AscensionAffixes.isRangedProjectile(projectile)",
    "snapshotRangedProjectile(projectile, weapon, player.isShiftKeyDown())", "tryRangedBurst",
    "AscensionAffixes.isPrecisionRangedProjectile(direct)", "fieldMastery ? 6.0D", "fieldMastery ? 10",
    "Math.min(0.65D", "projectileXpMultiplier(direct)",
    'RANGED_BURST_USED_KEY = "survivalascension_ranged_burst_used"',
    "getBooleanOr(RANGED_BURST_USED_KEY, false)", "putBoolean(RANGED_BURST_USED_KEY, true)"
], "0.52 ranged combat runtime")
ordered(combat, [
    "getBooleanOr(RANGED_BURST_USED_KEY, false)",
    "if (nearby.isEmpty()) return;",
    "putBoolean(RANGED_BURST_USED_KEY, true)",
    "candidate.hurtServer(level, event.getSource(), burstDamage)"
], "0.52 one physical burst per projectile")
need(main_mod, ["CombatProgression::onEntityJoin", "ranged projectile snapshots/impact bursts"], "0.52 ranged event wiring")
forbid(affix + combat, ["setChunkForced", "addRegionTicket", "getChunk("], "0.52 ranged world-loading policy")

# 0.53 shield ascension: successful real block -> bounded zero-damage physical guard wave.
need(affix, [
    "Tags.Items.TOOLS_SHIELD", "Category.SHIELD", 'SHIELD("shield")', "Items.SHIELD",
    "isShield(ItemStack stack)", "shieldWaveRadiusBonus", "shieldWaveTargetBonus",
    "shieldWaveKnockbackBonus", "shieldWaveCooldownReduction", "shieldWaveLiftBonus",
    'case SHIELD -> "방패"', 'case SHIELD -> "파동"', 'case SHIELD -> "진압"', 'case SHIELD -> "반동"',
    'category == Category.SHIELD ? "대응"', 'category == Category.SHIELD ? "압력"'
], "0.53 shield affix progression")
need(combat, [
    "onShieldBlock(LivingShieldBlockEvent event)", "event.getBlocked()", "event.getBlockedDamage()",
    "player.getUseItem()", "AscensionAffixes.isShield(shield)", "player.isShiftKeyDown()",
    'SHIELD_WAVE_READY_KEY = "survivalascension_shield_wave_ready"',
    "fieldMastery ? 6.5D", "fieldMastery ? 10", "Math.min(8.0D", "Math.min(14",
    "Math.min(1.30D", "Math.min(0.28D", "Math.max(6, baseCooldown",
    "ContentPackCompatibility.isCombatTarget(candidate)", "candidate.setDeltaMovement", "candidate.hurtMarked = true"
], "0.53 shield guard-wave runtime")
shield_start = combat.find("public static void onShieldBlock")
shield_end = combat.find("private static void tryRangedBurst", shield_start)
if shield_start < 0 or shield_end < 0:
    errors.append("0.53 shield runtime body missing")
else:
    shield_body = combat[shield_start:shield_end]
    forbid(shield_body, ["hurtServer(", "SkillProgressionService.award", "event.setBlocked(", "event.setBlockedDamage("], "0.53 shield no-damage/no-block-force policy")
need(main_mod, ["VERSION = \"0.55.0-alpha.1\"", "CombatProgression::onShieldBlock", "shield guard waves"], "0.53 shield event wiring")
need(reforge, ["검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비"], "0.53 shield imprint server flow")
need(equipment_ui, ["검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 표준 태그 장비 필요"], "0.53 shield imprint UI")
forbid(affix + combat, ["setChunkForced", "addRegionTicket", "getChunk("], "0.53 shield world-loading policy")

# 0.54 mace impact ascension: actual mace-smash only; vanilla inner radius preserved, zero-damage outer ring.
need(affix, [
    "Tags.Items.TOOLS_MACE", "Category.MACE", 'MACE("mace")', "Items.MACE", "isMace(ItemStack stack)",
    "maceImpactRadiusBonus", "maceImpactTargetBonus", "maceImpactKnockbackBonus", "maceImpactLiftBonus",
    'case MACE -> "메이스"', 'category == Category.MACE ? "충각"', 'case MACE -> "진동"', 'case MACE -> "분쇄"', 'case MACE -> "격퇴"'
], "0.54 mace affix progression")
need(affix, ["GEAR_CATEGORIES = List.of(Category.WEAPON, Category.RANGED, Category.PICKAXE, Category.AXE, Category.SHOVEL, Category.HOE, Category.SHIELD, Category.ARMOR)"], "0.54 mace acquisition preservation")
need(combat, [
    "DamageTypeTags.IS_MACE_SMASH", "AscensionAffixes.isMace(weapon)", "tryMaceImpact(player, serverLevel, primary, weapon, level)",
    "VANILLA_MACE_KNOCKBACK_RADIUS_SQR = 12.25D", "fieldMastery ? 9.0D", "fieldMastery ? 20",
    "Math.min(10.5D", "Math.min(26", "Math.min(1.30D", "Math.min(0.28D",
    "primary.distanceToSqr(candidate) > VANILLA_MACE_KNOCKBACK_RADIUS_SQR", "Attributes.KNOCKBACK_RESISTANCE",
    "ContentPackCompatibility.isCombatTarget(candidate)", "candidate.setDeltaMovement", "candidate.hurtMarked = true"
], "0.54 mace outer-impact runtime")
ordered(combat, [
    "event.getSource().is(DamageTypeTags.IS_MACE_SMASH) && AscensionAffixes.isMace(weapon)",
    "tryMaceImpact(player, serverLevel, primary, weapon, level)", "return;", "if (tryShockwave"
], "0.54 mace smash replaces generic cleave/shockwave")
mace_start = combat.find("private static void tryMaceImpact")
mace_end = combat.find("private static void tryRangedBurst", mace_start)
if mace_start < 0 or mace_end < 0:
    errors.append("0.54 mace runtime body missing")
else:
    mace_body = combat[mace_start:mace_end]
    forbid(mace_body, ["hurtServer(", "SkillProgressionService.award"], "0.54 mace zero-damage/zero-XP outer ring")
need(main_mod, ["VERSION = \"0.55.0-alpha.1\"", "mace outer impact rings"], "0.54 runtime banner")
need(reforge, ["검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비"], "0.54 mace imprint server flow")
need(equipment_ui, ["검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 표준 태그 장비 필요"], "0.54 mace imprint UI")
forbid(affix + combat, ["setChunkForced", "addRegionTicket", "getChunk("], "0.54 mace world-loading policy")

# 0.55 native 26.2 spear + Sulfur Caves integration.
deep55 = read("src/main/resources/data/survivalascension/tags/worldgen/biome/expedition/deep.json")
need(deep55, ['"minecraft:sulfur_caves"'], "0.55 Sulfur Caves Deep expedition bridge")
need(affix, ["ItemTags.SPEARS", "Category.SPEAR", 'SPEAR("spear")', "isSpear(ItemStack stack)",
             "spearLineReachBonus", "spearLineTargetBonus", "spearLineKnockbackBonus",
             'case SPEAR -> "스피어"', 'category == Category.SPEAR ? "관통"', 'case SPEAR -> "돌파"',
             'case SPEAR -> "대열"', 'case SPEAR -> "충압"'], "0.55 spear affix progression")
need(affix, ["GEAR_CATEGORIES = List.of(Category.WEAPON, Category.RANGED, Category.PICKAXE, Category.AXE, Category.SHOVEL, Category.HOE, Category.SHIELD, Category.ARMOR)"], "0.55 spear acquisition preservation")
need(combat, ["AscensionAffixes.isSpear(weapon)", "trySpearDrive(player, serverLevel, primary, weapon, level)",
              "forwardSpeed < 0.08D", "fieldMastery ? 7.5D", "Math.min(9.0D", "Math.min(8",
              "Math.min(1.10D", "Attributes.KNOCKBACK_RESISTANCE", "candidate.setDeltaMovement"], "0.55 spear drive-line runtime")
spear_start = combat.find("private static void trySpearDrive")
spear_end = combat.find("private static void tryMaceImpact", spear_start)
if spear_start < 0 or spear_end < 0:
    errors.append("0.55 spear runtime body missing")
else:
    forbid(combat[spear_start:spear_end], ["hurtServer(", "SkillProgressionService.award"], "0.55 spear zero-damage/zero-XP line")
ordered(combat, ["AscensionAffixes.isSpear(weapon)", "trySpearDrive(player, serverLevel, primary, weapon, level)", "return;", "if (tryShockwave"], "0.55 spear replaces generic cleave/shockwave")

# User-facing docs are part of the release contract, not an uncommitted CI-side patch.
project_doc = read("PROJECT.md")
readme = read("README.md")
changelog = read("CHANGELOG.md")
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(project_doc, [
    "Mod version: `0.55.0-alpha.1`",
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
need(project_doc, [
    "## 0.52 Ranged Combat Ascension / 원거리 전투 승천", "6.0 blocks / 10 targets", "hard-capped at 65%"
], "0.52 PROJECT docs")
need(readme, [
    "## 0.52.0-alpha.1 — Ranged Combat Ascension / 원거리 전투 승천", "Lv100 + Field Mastery 6/10", "capped at 65%"
], "0.52 README docs")
need(changelog, ["## 0.52.0-alpha.1", "Ranged Combat Ascension / 원거리 전투 승천", "6/10", "65%"], "0.52 CHANGELOG docs")
need(guide, ['h("원거리 전투 파급")', "현장 숙련=6블록/10체", "Shift 발사는 파급 없는 단일 정밀 타격"], "0.52 in-game guide")
need(project_doc, ["## 0.53 Shield Ascension / 방패 승천", "radius8.0", "minimum cooldown6 ticks"], "0.53 PROJECT docs")
need(readme, ["## 0.53.0-alpha.1 — Shield Ascension / 방패 승천", "6.5 blocks / 10 targets", "cooldown minimum6 ticks"], "0.53 README docs")
need(changelog, ["## 0.53.0-alpha.1", "Shield Ascension / 방패 승천", "zero damage", "min6t"], "0.53 CHANGELOG docs")
need(guide, ['h("방패 방어 파동")', "현장 숙련=6.5블록/10체", "파동 없는 정밀 방어", 'h("방패 정밀/파동")'], "0.53 in-game guide")
need(project_doc, ["## 0.54 Mace Impact Ascension / 메이스 충격권 승천", "9.0/20", "radius10.5"], "0.54 PROJECT docs")
need(readme, ["## 0.54.0-alpha.1 — Mace Impact Ascension / 메이스 충격권 승천", "9.0 blocks/20 targets", "zero damage"], "0.54 README docs")
need(changelog, ["## 0.54.0-alpha.1", "Mace Impact Ascension / 메이스 충격권 승천", "IS_MACE_SMASH", "9.0/20"], "0.54 CHANGELOG docs")
need(guide, ['h("메이스 충격권")', "현장 숙련=9블록/20체", "바닐라 메이스 스매시는 그대로", 'h("메이스 스매시")'], "0.54 in-game guide")
need(project_doc, ["## 0.55 Native 26.2 Spear + Sulfur Integration", "reach9.0 / targets8 / push1.10"], "0.55 PROJECT docs")
need(readme, ["## 0.55.0-alpha.1 — Native 26.2 Spear + Sulfur Integration", "minecraft:sulfur_caves", "Field Mastery 7.5/5"], "0.55 README docs")
need(changelog, ["## 0.55.0-alpha.1", "ItemTags.SPEARS", "0피해/0XP", "0.55.0-alpha.1-content-preview.1"], "0.55 CHANGELOG docs")
need(guide, ['h("스피어 돌파선")', "minecraft:spears", "피해·숙련 XP 없이", "바닐라 Jab/Charge"], "0.55 in-game guide")

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
baseline = baseline.replace("표준 검/곡괭이/도끼/삽/괭이 태그 장비", "표준 검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비")
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
print("- 0.52 ranged launch snapshots prevent post-shot gear swapping; precision/burst scale and persisted modifiers are bounded")
print("- 0.52 each physical projectile can produce at most one area burst, including piercing shots")
print("- 0.53 successful standard-shield blocks create bounded zero-damage guard waves; Shift keeps precision blocking")
print("- 0.54 real mace-smash hits use bounded hostile-only outer impact rings while vanilla 3.5-block knockback remains authoritative")
print("- 0.55 Sulfur Caves and dedicated spear momentum drive-line integration are bounded and regression-checked")
print("- README / PROJECT / CHANGELOG / in-game guide are committed and synchronized to 0.55")
