#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")

def need(source: str, tokens: tuple[str, ...], label: str) -> None:
    missing = [token for token in tokens if token not in source]
    if missing:
        raise SystemExit(f"{label} missing: {missing}")

properties = read("gradle.properties")
main = read("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java")
catalog = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java")
casting = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java")
player_data = read("src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java")
combat = read("src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java")
gear = read("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java")
tradition = read("src/main/java/kr/moonseungjun/arcanecircle/world/MagicTradition.java")
quests = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneQuestData.java")
economy = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java")
mages = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java")
network = read("src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java")
quest_payload = read("src/main/java/kr/moonseungjun/arcanecircle/network/QuestActionPayload.java")
screen = read("src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java")
items = read("src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java")
offers = read("src/main/java/kr/moonseungjun/arcanecircle/world/AcademyOfferCatalog.java")
lang = json.loads(read("src/main/resources/assets/arcanecircle/lang/ko_kr.json"))
index = json.loads(read("src/main/resources/data/arcanecircle/spell_catalog/index.json"))

need(properties, ("mod_version=0.12.1-alpha.7",), "version")
need(main, ('VERSION = "0.12.1-alpha.7"', "시전시간 0초", "마력 소모·시전시간·재사용 대기시간"), "lifecycle")
if index.get("version") != "0.12.1-alpha.7":
    raise SystemExit("alpha.7 spell index version missing")

need(catalog, (
    "DAMAGING_SPELLS", "isDamaging", "damageTierMultiplier",
    "case 2 -> 1.18", "case 5 -> 2.15", "case 8 -> 4.00", "case 9 -> 5.00"
), "circle damage progression")

need(player_data, (
    "Math.max(0.06, Math.pow(0.72, masteryGap))",
    "rawCooldown < 0.75 ? 0",
    "SpellCatalog.damageTierMultiplier",
    "chosen.powerFor", "chosen.manaMultiplier", "chosen.cooldownMultiplier",
    "if (totalTicks <= 0)"
), "mana, cooldown and faction scaling")
if "int cooldown = Math.max(8" in player_data:
    raise SystemExit("legacy minimum 8-tick cooldown remains")
if "int total = Math.max(1, totalTicks)" in player_data:
    raise SystemExit("legacy forced cooldown remains")

need(casting, (
    "new ChargeState(slot, cast.spell().id(), serverClock(player), 0)",
    "if (charge.requiredTicks <= 0) return;",
    "instantGap", "return 0;",
    "chosen.castTimeMultiplier",
    "cast.cooldownTicks() <= 0 ? \"없음\"",
    "recordCast(player, impact, spell.circle(), cast.fusion())"
), "instant casting and zero cooldown flow")

need(quests, (
    "MAX_ACTIVE = 3", '"mage_commissions_v1"', "offerStatus",
    "acceptOffer", "rejectOffer", "claim(ServerPlayer player, int index)",
    '"damage"', '"threat"', '"fusion"',
    "case 1 -> 1_200L", "case 5 -> 36_000L", "case 9 -> 820_000L",
    "questRewardMultiplier"
), "three-slot quest board")
need(quest_payload, ('"quest_action"', "QuestActionPayload(String action)"), "quest action payload")
need(network, (
    '"quests"', "QuestActionPayload.TYPE", "handleQuest",
    '"accept"', '"reject"', '"claim:"',
    '"quest_count="', '"quest_offer"', '"quest_0"'
), "quest network snapshot")
need(screen, (
    'new Tab("quests", "의뢰")', "clickQuests", "QuestActionPayload",
    '"의뢰 게시판"', '"수락"', '"거절"', '"보상 수령"',
    "t.strength()", "t.weakness()", "traditionInfo"
), "quest and faction UI")

need(combat, (
    "Attributes.ATTACK_DAMAGE", "Attributes.ARMOR", "Attributes.MOVEMENT_SPEED",
    "equipment", "getActiveEffects", "ArcaneMageService.circle",
    "Math.pow(threat, 1.25)", "Math.pow(threat, 1.75)",
    "combatValue", "peakThreat", "threatPoints"
), "nonlinear enemy threat")
need(economy, ("impact.combatValue()", "combatRewardMultiplier", "impact.threatPoints()"), "nonlinear Arcana economy")

need(mages, (
    "recentAttacker", "getLastHurtByMob", "quests.offer(player, mage.circle(), mage.affiliation())",
    'ArcaneNetwork.openPage(player, "quests")',
    "retaliating", "mage.affiliation().powerMultiplier()", "cooldownMultiplier()"
), "mage retaliation and faction combat")

need(tradition, (
    "strength", "weakness", "combatRewardMultiplier", "questRewardMultiplier",
    "castTimeMultiplier", "fusionMultiplier", "powerFor",
    "사거리 +18%", "마력 회복 +28%", "융합 위력 +18%", "전투 아르카나 +30%"
), "faction doctrines")

need(items, (
    "SAGE_HAT", "SAGE_ROBE", "SAGE_ROBE_HEM", "SKYWALKER_BOOTS",
    "ARCHMAGE_CROWN", "ARCHMAGE_ROBE", "ARCHMAGE_ROBE_HEM", "FROSTSTEP_BOOTS"
), "high-tier gear registration")
need(gear, (
    "hatTier", "robeTier", "bootsTier", "SLOW_FALLING", "FROSTED_ICE",
    "case 3 -> new Piece(1200", "case 3 -> new Piece(900", "case 3 -> new Piece(300"
), "tiered gear runtime")
need(offers, (
    '"gear:sage_hat"', '"gear:skywalker_boots"', '"gear:sage_robe"',
    '"gear:archmage_crown"', '"gear:froststep_boots"', '"gear:archmage_robe"',
    "3_200_000L"
), "high-tier gear economy")

resource_ids = (
    "sage_hat", "sage_robe", "sage_robe_hem", "skywalker_boots",
    "archmage_crown", "archmage_robe", "archmage_robe_hem", "froststep_boots"
)
for item_id in resource_ids:
    for relative in (
        f"src/main/resources/assets/arcanecircle/items/{item_id}.json",
        f"src/main/resources/assets/arcanecircle/models/item/{item_id}.json",
    ):
        if not (ROOT / relative).is_file():
            raise SystemExit(f"missing gear resource: {relative}")
    if f"item.arcanecircle.{item_id}" not in lang:
        raise SystemExit(f"missing Korean gear name: {item_id}")

print("Arcane Circle v0.12.1-alpha.7 progression, three-quest board, factions, gear and threat economy contract: PASS")
