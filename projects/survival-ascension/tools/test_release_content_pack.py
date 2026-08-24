#!/usr/bin/env python3
from __future__ import annotations

import contextlib
import io
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
BASELINE_LOCK_VERSION = "0.48.0-alpha.1-content-preview.1"
REQUIRED_LOCK_VERSION = "0.54.0-alpha.1-content-preview.1"
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
production_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
equipment_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java")
combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")

need(freight, [
    "FRONTLINE_FOOD = 176", "FRONTLINE_IRON = 56", "FRONTLINE_FUEL = 8",
    "FRONTLINE_LOGS = 32", "FRONTLINE_STONE_BRICKS = 128",
    "player.isShiftKeyDown()", "moveFrontlineBundleInto", "FRONTLINE_KEY"
], "0.49 frontline freight content-pack bridge")
need(production_ui, ["Shift=전선묶음", "레일6+·동력레일·호퍼·제어"], "0.49 frontline freight player flow")
need(depot_data, [
    "BASE_DEPOTS_PER_PLAYER = 3", "CIVIL_DEPOTS_PER_PLAYER = 6", "MAX_DEPOTS_PER_PLAYER = 9",
    "InfrastructureProject.CIVIL_WORKS", "InfrastructureProject.ASCENSION_NEXUS", "registrationLimit(ServerPlayer player)"
], "0.50 regional logistics content-pack flow")
need(production_ui, ["한도3→토목6→중추9"], "0.50 regional logistics player flow")
need(affix, [
    "ItemTags.HEAD_ARMOR", "ItemTags.CHEST_ARMOR", "ItemTags.LEG_ARMOR", "ItemTags.FOOT_ARMOR",
    "Category.ARMOR", "armorDamageMultiplier", "armorXpMultiplier"
], "0.51 armor content-pack bridge")
need(equipment_ui, ["방어구/방패 표준 태그 장비 필요"], "0.51 armor player flow")
need(combat, ["armorDamageMultiplier", "armorXpMultiplier"], "0.51 armor runtime flow")
need(affix, ["Tags.Items.TOOLS_BOW", "Tags.Items.TOOLS_CROSSBOW", "Category.RANGED", "snapshotRangedProjectile", "projectileDamageMultiplier", "projectileXpMultiplier"], "0.52 ranged content-pack bridge")
need(equipment_ui, ["검/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 표준 태그 장비 필요"], "0.52 ranged imprint player flow")
need(combat, ["onEntityJoin(EntityJoinLevelEvent event)", "tryRangedBurst", "isPrecisionRangedProjectile", "fieldMastery ? 6.0D", "fieldMastery ? 10"], "0.52 ranged runtime flow")
matrix = read("MODPACK_COMPAT_MATRIX.md")
need(matrix, ["c:tools/bow", "c:tools/crossbow", "발사체 스냅샷"], "0.52 generic ranged compatibility docs")
need(affix, ["Tags.Items.TOOLS_SHIELD", "Category.SHIELD", "isShield(ItemStack stack)", "shieldWaveRadiusBonus", "shieldWaveTargetBonus", "shieldWaveKnockbackBonus", "shieldWaveCooldownReduction", "shieldWaveLiftBonus"], "0.53 shield content-pack bridge")
need(equipment_ui, ["방어구/방패 표준 태그 장비 필요"], "0.53 shield player flow")
need(combat, ["onShieldBlock(LivingShieldBlockEvent event)", "event.getBlocked()", "candidate.setDeltaMovement", "player.isShiftKeyDown()"], "0.53 shield runtime flow")
need(matrix, ["c:tools/shield", "성공차단 방어 파동"], "0.53 generic shield compatibility docs")
need(affix, ["Tags.Items.TOOLS_MACE", "Category.MACE", "isMace(ItemStack stack)", "maceImpactRadiusBonus", "maceImpactTargetBonus", "maceImpactKnockbackBonus", "maceImpactLiftBonus"], "0.54 mace content-pack bridge")
need(equipment_ui, ["검/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 표준 태그 장비 필요"], "0.54 mace player flow")
need(combat, ["DamageTypeTags.IS_MACE_SMASH", "tryMaceImpact", "VANILLA_MACE_KNOCKBACK_RADIUS_SQR", "Attributes.KNOCKBACK_RESISTANCE"], "0.54 mace runtime flow")
need(matrix, ["c:tools/mace", "외곽 충격권", "엘리트 기본드롭에서는 메이스를 생성하지 않는다"], "0.54 generic mace compatibility docs")

if errors:
    print("RELEASE CONTENT-PACK AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

baseline_path = ROOT / "tools/test_content_pack_source.py"
baseline = baseline_path.read_text(encoding="utf-8")
baseline = baseline.replace(BASELINE_LOCK_VERSION, REQUIRED_LOCK_VERSION)
baseline = baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.54.0-alpha.1`')
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
print("regional_logistics_scale=PASS")
print("armor_affix_content_bridge=PASS")
print("ranged_affix_projectile_bridge=PASS")
print("shield_affix_guard_wave_bridge=PASS")
print("mace_affix_outer_impact_bridge=PASS")
print("RELEASE CONTENT-PACK AUDIT PASS")
