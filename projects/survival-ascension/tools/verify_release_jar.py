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

    affix_name = "kr/moonseungjun/survivalascension/equipment/AscensionAffixes.class"
    combat_name = "kr/moonseungjun/survivalascension/combat/CombatProgression.class"
    equipment_ui_name = "kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.class"
    for name in [affix_name, combat_name, equipment_ui_name]:
        if name not in zf.namelist():
            raise SystemExit(f"0.51 armor ascension runtime class missing: {name}")
    affix = zf.read(affix_name)
    combat = zf.read(combat_name)
    equipment_ui = zf.read(equipment_ui_name)
    for token in [
        b"HEAD_ARMOR", b"CHEST_ARMOR", b"LEG_ARMOR", b"FOOT_ARMOR",
        b"armorDamageMultiplier", b"armorXpMultiplier", b"ARMOR_SLOTS", b"getItemBySlot",
        b"IRON_HELMET", b"DIAMOND_CHESTPLATE", b"NETHERITE_LEGGINGS", b"NETHERITE_BOOTS",
    ]:
        if token not in affix:
            raise SystemExit(f"0.51 compiled armor-affix token missing: {token!r}")
    if b"getArmorSlots" in affix:
        raise SystemExit("0.51 compiled armor-affix still contains obsolete getArmorSlots API")
    for token in [b"armorDamageMultiplier", b"armorXpMultiplier"]:
        if token not in combat:
            raise SystemExit(f"0.51 compiled worn-armor routing missing: {token!r}")
    # ACTION_IMPRINT is a compile-time constant and may be inlined away. Verify the actual
    # compiled UI routing through the surviving action enum, affix admission call, payload type,
    # and packet-dispatch path instead of relying on the source constant's field name.
    for token in [b"IMPRINT", b"canImprint", b"EquipmentActionPayload", b"sendToServer"]:
        if token not in equipment_ui:
            raise SystemExit(f"0.51 compiled armor imprint UI routing token missing: {token!r}")

    for token in [b"TOOLS_BOW", b"TOOLS_CROSSBOW", b"snapshotRangedProjectile", b"survivalascension_ranged_projectile", b"projectileDamageMultiplier", b"projectileXpMultiplier"]:
        if token not in affix:
            raise SystemExit(f"0.52 compiled ranged-affix token missing: {token!r}")
    for token in [
        b"onEntityJoin", b"tryRangedBurst", b"isPrecisionRangedProjectile",
        b"projectileBurstRadiusBonus", b"projectileBurstTargetBonus",
        b"survivalascension_ranged_burst_used", b"getBooleanOr", b"putBoolean",
    ]:
        if token not in combat:
            raise SystemExit(f"0.52 compiled ranged-combat token missing: {token!r}")
    main_name = "kr/moonseungjun/survivalascension/SurvivalAscension.class"
    if main_name not in zf.namelist():
        raise SystemExit("0.53 main runtime class missing")
    main_class = zf.read(main_name)
    for token in [b"TOOLS_SHIELD", b"shieldWaveRadiusBonus", b"shieldWaveTargetBonus", b"shieldWaveKnockbackBonus", b"shieldWaveCooldownReduction", b"shieldWaveLiftBonus"]:
        if token not in affix:
            raise SystemExit(f"0.53 compiled shield-affix token missing: {token!r}")
    for token in [b"onShieldBlock", b"survivalascension_shield_wave_ready", b"isShield", b"setDeltaMovement"]:
        if token not in combat:
            raise SystemExit(f"0.53 compiled shield-wave token missing: {token!r}")
    if b"shield guard waves" not in main_class:
        raise SystemExit("0.53 runtime banner missing shield guard waves")

print("frontline_freight_manifest_runtime=present")
print("frontline_freight_release_verify=PASS")
print("regional_logistics_scale_runtime=present")
print("regional_logistics_scale_release_verify=PASS")
print("armor_affix_runtime=present")
print("armor_affix_release_verify=PASS")
print("ranged_combat_runtime=present")
print("ranged_combat_one_burst_per_projectile=present")
print("ranged_combat_release_verify=PASS")
print("shield_guard_wave_runtime=present")
print("shield_guard_wave_release_verify=PASS")
