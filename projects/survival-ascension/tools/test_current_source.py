#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/survivalascension"


def text(path):
    return path.read_text(encoding="utf-8")


def require(condition, message):
    if not condition:
        raise AssertionError(message)


props = text(ROOT / "gradle.properties")
require("minecraft_version=26.2" in props, "Minecraft version drift")
require("neo_version=26.2.0.38-beta" in props, "NeoForge version drift")
require("mod_version=0.61.16-alpha.1" in props, "Survival Ascension version drift")

main = text(JAVA / "SurvivalAscension.java")
require('VERSION = "0.61.16-alpha.1"' in main, "source version drift")
for event in (
    "MiningProgression::onBlockBreak",
    "WoodcuttingProgression::onServerTick",
    "HarvestingProgression::onServerTick",
    "CombatProgression::onLivingDeath",
    "ConstructionProgression::onServerTick",
    "MobilityProgression::onPlayerTick",
    "WarbandDirector::onServerTick",
    "EliteMobSystem::onServerTick",
    "ApexHuntSystem::onServerTick",
    "OutpostSiegeSystem::onServerTick",
    "AscensionTrialSystem::onServerTick",
    "FinalAscensionBossSystem::onServerTick",
):
    require(event in main, f"core event registration missing: {event}")

tuning = text(JAVA / "progress/SkillTuning.java")
require("MAX_LEVEL = 100" in tuning, "mastery cap drift")
for curve in (
    "if (level < 20)",
    "if (level < 30) return 430L + 15L * (level - 20);",
    "if (level < 60)",
    "if (level < 90)",
    "return 5100L + 220L * (level - 90);",
):
    require(curve in tuning, f"mastery XP curve drift: {curve}")
for multiplier in (
    "case MINING -> { early = 1.25D; late = 1.10D; }",
    "case WOODCUTTING -> { early = 2.50D; late = 2.00D; }",
    "case HARVESTING -> { early = 3.00D; late = 2.50D; }",
    "case FISHING -> { early = 6.00D; late = 5.00D; }",
    "case COMBAT -> { early = 4.00D; late = 3.50D; }",
    "case CONSTRUCTION -> { early = 5.00D; late = 3.50D; }",
    "case MOBILITY -> { early = 4.00D; late = 3.00D; }",
):
    require(multiplier in tuning, f"skill pacing drift: {multiplier}")
for threshold in (
    "if (level >= 100)",
    "if (level >= 90)",
    "if (level >= 60)",
    "if (level >= 30)",
    "if (level >= 10)",
):
    require(threshold in tuning, f"mastery threshold missing: {threshold}")

infra = text(JAVA / "infrastructure/InfrastructureProject.java")
for requirement in (
    "COMBAT_ACADEMY",
    "전투 Lv.90",
    "QUARRY_NETWORK",
    "채굴 Lv.90",
    "BUILDER_FOUNDRY",
    "건축 Lv.90",
    "ASCENSION_NEXUS",
    "기동 Lv.90",
):
    require(requirement in infra, f"Lv90 infrastructure authority drift: {requirement}")

combat = text(JAVA / "combat/CombatProgression.java")
require("private static int xpForKill" in combat, "combat kill XP authority missing")
require("health * healthScale" in combat, "combat XP no longer scales from target health")

mining = text(JAVA / "mining/MiningProgression.java")
for unchanged in (
    "if (state.is(Blocks.COPPER_ORE)) return 7;",
    "if (state.is(Blocks.IRON_ORE)) return 9;",
    "if (state.is(Blocks.DIAMOND_ORE)) return 18;",
    "if (state.is(Blocks.ANCIENT_DEBRIS)) return 24;",
):
    require(unchanged in mining, f"mining pacing changed unexpectedly: {unchanged}")

mobility = text(JAVA / "mobility/MobilityProgression.java")
for optimization in (
    "APPLIED_ATTRIBUTE_LEVEL",
    "player.tickCount % 10 == 0",
    "refreshAttributesIfNeeded",
    "if (applied != null && applied == level) return;",
    "trackTraversal(player);",
):
    require(optimization in mobility, f"mobility optimization invariant missing: {optimization}")

client = text(JAVA / "client/SurvivalAscensionClient.java")
require("InputConstants.KEY_X" in client, "dash default key must be X")
require("mobility_action\", InputConstants.KEY_V" not in client, "old V dash default returned")

require("MobilityProgression::onPlayerRespawn" in main and "MobilityProgression::onPlayerChangedDimension" in main, "mobility transient attributes are not restored across lifecycle boundaries")
require("return 1.0D + 0.0020D * clamped + 0.000010D * clamped * clamped;" in tuning, "mobility per-level speed scaling drift")
require("if (level >= 30) return 1.25D;" in tuning and "if (level >= 60) return 1.50D;" in tuning, "mobility step progression drift")
require("fishingBonusCatchChance" in tuning, "fishing bonus-yield progression missing")
fishing = text(JAVA / "fishing/FishingProgression.java")
require("applyBonusCatch" in fishing and "fish.grow(extra)" in fishing, "fishing deterministic bonus catch is not applied to real fish drops")
require("player.getRandom().nextDouble() < chance" not in fishing, "fishing mastery returned to streaky RNG")
require("ANGLER_HARBOR" in infra and "어업 부두" in infra, "fishing infrastructure project missing")
require("HARBOR_BONUS_CATCH_MILLI = 350" in fishing and "HARBOR_PRESERVATION_MILLI = 150" in fishing,
        "angler harbor fishing bonus drift")
require("HARBOR_XP_MULTIPLIER = 1.25D" in fishing, "angler harbor XP acceleration drift")
progress_data = text(JAVA / "progress/SkillProgressData.java")
require("fishing_bonus_milli" in progress_data and "fishing_preserve_milli" in progress_data,
        "persistent deterministic fishing meters missing")
site = text(JAVA / "infrastructure/InfrastructureSiteService.java")
require("ANGLER_SITE" in site and "Blocks.WATER" in site and "Blocks.SMOKER" in site,
        "physical waterside angler harbor commissioning site missing")
infra_ui = text(JAVA / "client/InfrastructureRadialMenuScreen.java")
require("어업 부두" in infra_ui and "Items.FISHING_ROD" in infra_ui, "angler harbor missing from infrastructure menu")

network = text(JAVA / "network/SkillNetwork.java")
require('PROTOCOL = "15"' in network, "TBOS shrine locator packet protocol must be 15")
expedition_payload = text(JAVA / "network/ExpeditionSnapshotPayload.java")
require("String currentRegionId" in expedition_payload and "ExpeditionProgression.currentRegion(player)" in expedition_payload, "server-authoritative current expedition region missing from snapshot")
expedition_state = text(JAVA / "client/ClientExpeditionState.java")
require("currentRegionId()" in expedition_state, "client expedition state does not retain current region")
expedition_screen = text(JAVA / "client/ExpeditionScreen.java")
require("현재 원정권" in expedition_screen and "isCurrent" in expedition_screen and "requestSnapshot();" in expedition_screen, "expedition current-region visibility/highlight missing")

hud = text(JAVA / "client/SkillHudOverlay.java")
require("graphics.guiWidth() - width - rightMargin" in hud, "Mythic tracker is not right-edge anchored")
require("graphics.guiWidth() >= 420" in hud, "Mythic tracker desktop boss-bar separation missing")
require("top = 78" in hud, "Mythic tracker narrow-screen boss-bar fallback missing")
locator = text(JAVA / "compat/TbosFractureShrineLocator.java")
require("TbosFractureShrineLocator::onPlayerTick" in main, "TBOS shrine locator event missing")
require("AdventureWorldManager" in locator and "TemporalSiteManager" in locator and "fractureShrines" in locator,
        "TBOS planned/exact shrine bridge missing")
require("setChunkForced" not in locator and "addRegionTicket" not in locator, "TBOS locator may force-load chunks")
require("FractureShrineTargetPayload.TYPE" in network and "installFractureShrineReceiver" in network, "TBOS locator packet missing")
require("ClientFractureShrineState" in hud and "균열 성소" in hud and "예상" in hud, "TBOS locator HUD missing")

warband = text(JAVA / "elite/WarbandDirector.java")
require("BEHAVIOR_INTERVAL = 20" in warband, "warband broad scan cadence regressed")
require("FORMATION_INTERVAL = 200" in warband, "warband formation cadence drift")

freight = text(JAVA / "production/FreightService.java")
require("level.hasChunkAt" in freight, "freight loaded-chunk safety missing")
for forbidden in ("setChunkForced", "addRegionTicket", "teleportTo", "randomTeleport"):
    require(forbidden not in freight, f"physical freight policy regressed: {forbidden}")

field = text(JAVA / "production/FieldDepotService.java")
for forbidden in ("setChunkForced", "addRegionTicket"):
    require(forbidden not in field, f"physical depot policy regressed: {forbidden}")

print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.16 soft TBOS shrine locator + protocol15 + prior runtime invariants")
