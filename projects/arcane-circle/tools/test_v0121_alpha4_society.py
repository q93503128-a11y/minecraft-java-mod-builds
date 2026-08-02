#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"

def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

properties = read("gradle.properties")
main = read("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java")
catalog = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java")
casting = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java")
fusion = read("src/main/java/kr/moonseungjun/arcanecircle/magic/FusionSpellEffects.java")
notice = read("src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneNoticeService.java")
mage = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java")
quests = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneQuestData.java")
economy = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java")
world_data = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneWorldData.java")
network = read("src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java")
state = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneClientState.java")
hud = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneHud.java")
screen = read("src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java")
index = json.loads(read("src/main/resources/data/arcanecircle/spell_catalog/index.json"))

def need(source: str, tokens: tuple[str, ...], label: str) -> None:
    missing = [token for token in tokens if token not in source]
    if missing:
        raise SystemExit(f"{label} missing: {missing}")

need(properties, ("mod_version=0.12.1-alpha.5",), "version")
need(main, ('VERSION = "0.12.1-alpha.5"', "ArcaneMageService::onInteract", "ArcaneMageService.tickNear"), "lifecycle")
need(catalog, (
    'addFusion("steam_burst"', 'addFusion("frost_step"', 'addFusion("thunder_cage"',
    'addFusion("solar_guard"', 'addFusion("void_lance"', 'addFusion("winter_domain"',
    'addFusion("astral_prison"', 'addFusion("phoenix_requiem"', 'addFusion("world_sunder"'
), "expanded fusion catalogue")
if catalog.count('\n        addFusion("') != 19:
    raise SystemExit(f"expected 19 fusion formulae, found {catalog.count(chr(10) + '        addFusion(\\\"')}")
need(casting, (
    "fusionCooldownBlock", "startFusionIngredientCooldowns", "FusionSpellEffects.supports",
    "ArcaneQuestData.get", "ArcaneNoticeService.push"
), "fusion cooldown and commission integration")
need(fusion, ("steamBurst", "frostStep", "thunderCage", "solarGuard", "voidLance",
              "winterDomain", "astralPrison", "phoenixRequiem", "worldSunder"), "fusion effects")
need(mage, ("MAGE_TAG", "CIRCLE_PREFIX", "level.isVillage", "onInteract",
            "ArcaneNetwork.openPage", "castResidentSpell", "castHostileSpell"), "mage residents")
need(quests, ("mage_commissions_v1", "recordCast", "claim", "ArcaneWorldData.get"), "persistent commissions")
need(economy, ("FIRST_TRADITION_COST = 750L", "TRADITION_CHANGE_COST = 5000L",
               "traditionCost", "world.chooseTradition(player, tradition, cost)"), "universal Arcana economy")
need(world_data, ("if (!spendMarks(player, attunementCost)) return false",), "paid faculty attunement")
need(network, ("notice_seq=", "notice_ttl=", "quest_id=", "quest_reward=", "openPage"), "snapshot additions")
need(state, ("noticeVisible", "noticeText", "cooldownRemainingTicks(String spellId)"), "client notice state")
need(hud, ("drawRaisedNotice", "ArcaneClientState.noticeVisible"), "raised spell notice")
need(screen, (
    "private static int activeSlot = -1", "activeSlot = -1", "selectedStaffId",
    "clickStaffs", "drawStaffRecipe", "recipeHint()", "questPanel"
), "grimoire interaction details")

if index.get("version") != "0.12.1-alpha.5":
    raise SystemExit("alpha.4 catalogue version missing")
if index.get("fusion_spells") != 19:
    raise SystemExit("catalogue fusion count mismatch")
if index.get("mage_residents") is not True or index.get("mage_currency") != "arcana":
    raise SystemExit("mage society metadata missing")
if index.get("commission_currency") != "arcana":
    raise SystemExit("commission currency metadata missing")

print("Arcane Circle v0.12.1-alpha.5 fusion, UI and mage society contract: PASS")
