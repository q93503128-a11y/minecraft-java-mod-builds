#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def need(text: str, tokens: tuple[str, ...], label: str) -> None:
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"{label} missing: {missing}")


def forbid(text: str, tokens: tuple[str, ...], label: str) -> None:
    found = [token for token in tokens if token in text]
    if found:
        raise SystemExit(f"{label} forbidden tokens remain: {found}")

properties = read("gradle.properties")
main = read("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java")
client = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneClient.java")
casting = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java")
data = read("src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java")
gear = read("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java")
items = read("src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java")
economy = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java")
offers = read("src/main/java/kr/moonseungjun/arcanecircle/world/AcademyOfferCatalog.java")
screen = read("src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java")
network = read("src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java")
effects = read("src/main/java/kr/moonseungjun/arcanecircle/magic/ExpandedSpellEffects.java")
lang = json.loads(read("src/main/resources/assets/arcanecircle/lang/ko_kr.json"))
index = json.loads(read("src/main/resources/data/arcanecircle/spell_catalog/index.json"))

need(properties, ("mod_version=0.12.1-alpha.6",), "version")
need(main, ('VERSION = "0.12.1-alpha.6"', "MageGearService.tick", "누른 채 다른 숫자 주문"), "lifecycle")
need(client, (
    "primarySlot", "fusionChord", "FUSION_QUEUED", "new QueueFusionPayload(primarySlot)",
    "new CommitFusionPayload(0)", "new BeginCastPayload(slot)", "new ReleaseCastPayload(primarySlot)"
), "number-key fusion chord")
forbid(client, ("FUSION_MODIFIER_KEY", "InputConstants.KEY_X", "fusion_modifier"), "obsolete X fusion input")
need(casting, (
    "융합 불가 · ", "fusion.message()", "처음 누른 주문 키", "처음 누른 키를 놓아 시전"
), "immediate fusion rejection")
need(data, (
    "MageGearService.GearStats", "gear.manaCostMultiplier", "gear.cooldownMultiplier",
    "strongestIngredient", "ingredients.size() >= 3 ? 1.45 : 1.25", "power = Math.max(power, fusionFloor)"
), "gear stats and fusion floor")
need(gear, (
    "EquipmentSlot.CHEST", "EquipmentSlot.LEGS", "MAGE_ROBE_HEM", "MobEffects.SPEED",
    "MobEffects.JUMP_BOOST", "MobEffects.HEALTH_BOOST", "MobEffects.RESISTANCE"
), "two-slot robe runtime")
need(items, (
    "MAGE_HAT", "MAGE_ROBE", "MAGE_ROBE_HEM", "MAGE_BOOTS",
    "ArmorMaterials.LEATHER", "ArmorType.HELMET", "ArmorType.CHESTPLATE", "ArmorType.BOOTS"
), "wearable gear registration")
need(offers, ("Kind { PRIMER, SPELLBOOK, STAFF, GEAR }", "gear:mage_hat", "gear:mage_robe", "gear:mage_boots"), "gear offers")
need(economy, ("case GEAR", "ArcaneNoticeService.push"), "Arcana gear economy and screen-safe notices")
need(screen, (
    "ArcaneClientState.noticeText", "융합 불가 · 필요", "재료 주문 미습득",
    'ArcaneClientState.text("gear_hat"', 'ArcaneClientState.text("gear_robe"'
), "grimoire feedback")
need(network, ("MageGearService.GearStats", '";gear_hat="', '";gear_robe="', '";gear_boots="'), "gear snapshot")
need(effects, ('case "feather_fall" -> featherFall(player, 120);',), "brief feather fall")

if "key.arcanecircle.fusion_modifier" in lang:
    raise SystemExit("obsolete X key translation remains")
for key in ("item.arcanecircle.mage_hat", "item.arcanecircle.mage_robe", "item.arcanecircle.mage_boots"):
    if key not in lang:
        raise SystemExit(f"missing gear translation: {key}")
for item in ("mage_hat", "mage_robe", "mage_robe_hem", "mage_boots"):
    if not (ROOT / f"src/main/resources/assets/arcanecircle/items/{item}.json").is_file():
        raise SystemExit(f"missing item definition: {item}")
    if not (ROOT / f"src/main/resources/assets/arcanecircle/models/item/{item}.json").is_file():
        raise SystemExit(f"missing item model: {item}")

if index.get("version") != "0.12.1-alpha.6":
    raise SystemExit("alpha.6 catalogue version missing")
if index.get("fusion_input") != "number_key_chord":
    raise SystemExit("number-key fusion metadata missing")
if index.get("feather_fall_ticks") != 120:
    raise SystemExit("feather fall duration metadata mismatch")

print("Arcane Circle v0.12.1-alpha.6 fusion, notice, gear and duration contract: PASS")
