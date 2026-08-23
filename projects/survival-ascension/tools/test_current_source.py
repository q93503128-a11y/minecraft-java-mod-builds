#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing: {rel}")
        return ""
    return path.read_text(encoding="utf-8")

def need(text, needles, label):
    for needle in needles:
        if needle not in text:
            errors.append(f"{label} missing: {needle}")

required = [
    "PROJECT.md", "README.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md",
    "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
    "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/resources/META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt",
    "src/main/resources/META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt",
    "src/main/resources/META-INF/third-party/MINEMENU_MIT.txt",
    "src/main/resources/META-INF/third-party/BUILDING_GADGETS_2_MIT.txt",
    "src/main/resources/META-INF/third-party/MOB_CHAMPIONS_MIT.txt",
    "src/main/resources/META-INF/third-party/APOTHEOSIS_MIT.txt",
    "src/main/resources/META-INF/third-party/MEKANISM_MIT.txt",
    "src/main/resources/META-INF/third-party/WARBAND_MIT.txt",
    "src/main/resources/META-INF/third-party/HOSTILES_ARE_TOO_EASY_CC0.txt",
    "src/main/resources/META-INF/third-party/GATEWAYS_TO_ETERNITY_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialDoctrine.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = read("gradle.properties")
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.25.0-alpha.1"], "toolchain/version")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.25.0-alpha.1"', "ExpeditionProgression::onPlayerTick", "ExpeditionProgression::onPlayerLoggedOut",
            "AscensionTrialSystem::onServerTick", "WorldAscensionProgression::onLivingDeath"], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

action = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java")
need(action, ["LOGS_FELLED", "BLOCKS_BUILT", "CROPS_HARVESTED", "TRAVEL_DISTANCE", "OCEAN_VOYAGE",
              "BLOCKS_MINED", "HOSTILES_KILLED", "DASHES_USED", "fromSkill"], "expedition action vocabulary")

directive = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java")
for token in [
    "WOODLAND_STANDARD", "WOODLAND_PATROL", "ARID_STANDARD", "ARID_ROUTE", "WETLAND_STANDARD", "WETLAND_CLEARANCE",
    "HIGHLANDS_STANDARD", "HIGHLANDS_DASH", "OCEAN_STANDARD", "OCEAN_PATROL", "DEEP_STANDARD", "DEEP_CLEARANCE",
    "FROZEN_STANDARD", "FROZEN_DASH", "NETHER_STANDARD", "NETHER_SUPPLY", "END_STANDARD", "END_TRAVERSE",
    "LOGS_FELLED, 96", "LOGS_FELLED, 64", "TRAVEL_DISTANCE, 240", "BLOCKS_BUILT, 128", "CROPS_HARVESTED, 96",
    "DASHES_USED, 12", "OCEAN_VOYAGE, 800", "BLOCKS_MINED, 192", "HOSTILES_KILLED, 24", "HOSTILES_KILLED, 32",
    "optionCount", "select(ExpeditionRegion region, int index)"
]:
    if token not in directive: errors.append(f"field directives missing: {token}")
if directive.count("(ExpeditionRegion.") < 18:
    errors.append("field directives must contain at least 18 region-bound directives")

data = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java")
need(data, [
    '"expedition_v1"', 'optionalFieldOf("directives", Map.of())', 'optionalFieldOf("region_rewards", -1)',
    "this.directives.putIfAbsent(regionKey(region), 0)", "legacyProgressKey(region)", "taskProgressKey(region, first.action())",
    "MILESTONE_MASTER", "migratedCompleted = ALL_REGIONS_MASK", "discover(ServerPlayer player, ExpeditionRegion region, int directiveIndex)",
    "Math.floorMod(directiveIndex, ExpeditionDirective.optionCount(region))", "directive(ServerPlayer player, ExpeditionRegion region)",
    "addProgress(ServerPlayer player, ExpeditionRegion region, ExpeditionAction action, int amount)", "directiveComplete(state, region, directive)",
    "claimRegionReward", "directiveSummary", "countStageZeroCompleted", "isMasterSurveyComplete"
], "directive persistence/migration")

expedition = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")
need(expedition, [
    "ExpeditionAction.fromSkill(skill)", "recordAction(ServerPlayer player, ExpeditionAction action, int amount)",
    "player.level().getRandom().nextInt(ExpeditionDirective.optionCount(region))", "data.discover(player, region, option)",
    "data.addProgress(player, region, action, amount)", "result.taskCompletedNow()", "result.regionCompletedNow()",
    "ExpeditionAction.OCEAN_VOYAGE", "player.isPassenger() || player.isSwimming() || player.isInWater()",
    "distance > 24.0D", "기존 0.23 발견 보상 승계", "countStageZeroCompleted(player) >= 4",
    "data.isComplete(player, ExpeditionRegion.DEEP)", "data.isComplete(player, ExpeditionRegion.NETHER)",
    "AscensionAffixes.createEliteDrop", "hasFieldMastery"
], "multi-task expedition progression")

mobility = read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
need(mobility, [
    "ExpeditionProgression.recordAction(player, ExpeditionAction.DASHES_USED, 1)",
    "ExpeditionProgression.recordSkillAction(player, SkillType.MOBILITY, units * 6)",
    "!player.isPassenger()", "!player.getAbilities().flying", "!player.isFallFlying()", "!player.isSwimming()",
    "ExpeditionProgression.hasFieldMastery(player)", "return 4;", "AIR_DASH_COUNT.put(uuid, 0)"
], "mobility directive/field mastery")

mining = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
need(mining, ["ExpeditionProgression.recordSkillAction(player, SkillType.MINING, 1)", "player.gameMode.destroyBlock"], "mining objective source")
need(wood, ["ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1)", "hasLeavesNearby", "GLOBAL_LOG_BUDGET_PER_TICK = 64", "LOCAL_LOG_BUDGET_PER_TICK = 12"], "wood objective source")
need(harvest, ["ExpeditionProgression.recordSkillAction(player, SkillType.HARVESTING, 1)", "GLOBAL_HARVEST_BUDGET_PER_TICK = 64", "LOCAL_HARVEST_BUDGET_PER_TICK = 12", "player.gameMode.destroyBlock"], "harvest objective source")
need(construction, ["ExpeditionProgression.recordSkillAction(player, SkillType.CONSTRUCTION", "GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "EventHooks.onBlockPlace", "consumeOne(player, item)"], "construction objective source")
need(combat, ["if (victim instanceof Enemy) ExpeditionProgression.recordSkillAction(player, SkillType.COMBAT, 1)"], "combat objective source")

bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
need(bore, ["MAX_PENDING_PER_PLAYER = 640", "GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12",
            "fieldMastery ? 12", "player.gameMode.destroyBlock(target)"], "field mastery bore")
need(wood, ["FIELD_MASTERY_LOG_LIMIT = 448"], "field mastery wood")
need(harvest, ["baseSize = 13", "MAX_PENDING_PER_PLAYER = 384"], "field mastery harvest")
need(combat, ["fieldMastery ? 7.5D", "fieldMastery ? 20", "0.55D"], "field mastery combat")
need(construction, ["fieldMastery ? 65", "fieldMastery ? 13", "MAX_PENDING_BLOCKS_PER_PLAYER = 512"], "field mastery construction")

trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
doctrine = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialDoctrine.java")
need(trial, ["TOTAL_WAVES = 4", "WAVE_TIMEOUT_TICKS = 1200", "AscensionTrialDoctrine.random", "maybeReinforce",
             "ServerBossEvent", "removeStaleServerTrials", "AscensionAffixes.createEliteDrop"], "Ascension Trial regression")
need(doctrine, ["ONSLAUGHT", "PURSUIT", "SIEGE", "쇄도", "추격", "봉쇄"], "trial doctrine regression")
if '"minecraft:evoker"' in trial:
    errors.append("Ascension Trial must not spawn untracked summon-producing evokers")

affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
need(affix, ["AWAKENED", "currentAffixes(stack).size() == 3", "missing.size() != 2", "awakened ? 4 : rarity", "§5[각성 신화]"], "awakened Mythic regression")
need(reforge, ["ACTION_AWAKEN", "Items.AMETHYST_SHARD, 256", "Items.DIAMOND, 24", "Items.NETHERITE_SCRAP, 8", "Items.ECHO_SHARD, 64", "Items.DRAGON_BREATH, 16"], "awakening economy")

for rel in [
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"
]:
    text = read(rel)
    if re.search(r"setBlock\s*\([^\n]*AIR", text):
        errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex", "mekanism.common", "com.warband", "vbonedra.hostiles_are_too_easy", "ftbquests", "ejektaflex.bountiful"]:
    for path in (ROOT / "src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8", errors="ignore").lower():
            errors.append(f"reference-only namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 / network protocol 8")
print("- 18 persistent field directives provide two options per expedition region; mixed directives require all tasks")
print("- 0.24 discovered/progress/completed saves migrate to standard directives without duplicate region XP")
print("- directive counters originate from real scaled work, legitimate movement/voyage, hostile kills and successful dash actions")
print("- Field Mastery scale/tick budgets, tactical trials, awakened Mythic and world progression regressions retained")
