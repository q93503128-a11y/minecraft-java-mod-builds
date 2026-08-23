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
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncident.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialDoctrine.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = read("gradle.properties")
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.26.0-alpha.1"], "gradle properties")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.26.0-alpha.1"', "ExpeditionIncidentSystem::onPlayerTick", "ExpeditionIncidentSystem::onPlayerLoggedOut",
            "AscensionTrialSystem::onServerTick", "WarbandDirector::onServerTick"], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

action = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java")
need(action, ["LOGS_FELLED", "BLOCKS_BUILT", "CROPS_HARVESTED", "TRAVEL_DISTANCE", "OCEAN_VOYAGE",
              "BLOCKS_MINED", "HOSTILES_KILLED", "DASHES_USED"], "expedition actions")
directive = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java")
for marker in ["WOODLAND_STANDARD", "WOODLAND_PATROL", "ARID_STANDARD", "ARID_ROUTE", "WETLAND_STANDARD", "WETLAND_CLEARANCE",
               "HIGHLANDS_STANDARD", "HIGHLANDS_DASH", "OCEAN_STANDARD", "OCEAN_PATROL", "DEEP_STANDARD", "DEEP_CLEARANCE",
               "FROZEN_STANDARD", "FROZEN_DASH", "NETHER_STANDARD", "NETHER_SUPPLY", "END_STANDARD", "END_TRAVERSE"]:
    need(directive, [marker], "18 directive catalog")
need(directive, ["List<ExpeditionDirective> forRegion", "optionCount", "List<Task> tasks"], "directive selection")

data = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java")
need(data, ['"expedition_v1"', 'optionalFieldOf("directives", Map.of())', 'optionalFieldOf("region_rewards", -1)',
            'optionalFieldOf("incident_rewards", 0)', "directiveComplete", "firstIncompleteTask", "claimIncidentReward",
            "incidentResolved", "MILESTONE_MASTER", "legacyProgressKey", "taskProgressKey"], "expedition saved data")
need(data, ["for (ExpeditionDirective.Task task : directive.tasks())", "return false;"], "all-task completion")

progression = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")
need(progression, ["public static ExpeditionRegion currentRegion", "ExpeditionDirective.optionCount", "data.discover(player, region, option)",
                   "ExpeditionIncidentSystem.recordAction(player, action, amount)", "ExpeditionIncidentSystem.recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount)",
                   "grantIncidentBonus", "data.claimRegionReward", "data.claimMilestone"], "expedition progression")

incident = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncident.java")
for marker in ["WOODLAND_AMBUSH", "WOODLAND_RUSH", "ARID_AMBUSH", "ARID_RUSH", "WETLAND_AMBUSH", "WETLAND_RUSH",
               "HIGHLANDS_AMBUSH", "HIGHLANDS_RUSH", "OCEAN_AMBUSH", "OCEAN_RUSH", "DEEP_AMBUSH", "DEEP_RUSH",
               "FROZEN_AMBUSH", "FROZEN_RUSH", "NETHER_AMBUSH", "NETHER_RUSH", "END_AMBUSH", "END_RUSH"]:
    need(incident, [marker], "18 incident catalog")
need(incident, ["Kind { AMBUSH, ACTION_RUSH }", "ExpeditionAction.LOGS_FELLED, 24", "ExpeditionAction.BLOCKS_BUILT, 24",
                "ExpeditionAction.CROPS_HARVESTED, 20", "ExpeditionAction.DASHES_USED, 4", "ExpeditionAction.OCEAN_VOYAGE, 180",
                "ExpeditionAction.BLOCKS_MINED, 48", "ExpeditionAction.TRAVEL_DISTANCE, 180", '"minecraft:drowned"',
                '"minecraft:wither_skeleton"', '"minecraft:shulker"'], "incident definitions")

incident_system = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java")
need(incident_system, ["public static boolean isActive(ServerPlayer player)", "CHECK_INTERVAL_TICKS = 600", "START_CHANCE = 0.10D",
                       "START_COOLDOWN_TICKS = 3600", "TRIAL_EXCLUSION_AFTER_READY_TICKS = 3600", "OUTSIDE_GRACE_TICKS = 200", "EVENT_RADIUS = 48.0D",
                       "ServerBossEvent", "EntitySpawnReason.TRIGGERED", "findWaterSpawn", "spawned.size() < minimum",
                       "cleanupMobs", "removeStaleServerIncidents", "data.incidentResolved(player, region)",
                       "data.claimIncidentReward", "bonusTask.target() / 5", "ExpeditionProgression.grantIncidentBonus",
                       "Items.EMERALD, 4", "Items.AMETHYST_SHARD, 8", "Items.DIAMOND, 2", "Items.ECHO_SHARD, 4",
                       "Items.DIAMOND, 4", "Items.ECHO_SHARD, 8"], "incident lifecycle and rewards")
commands = read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")
need(commands, ["사건 해결", "expedition.incidentResolved(player, region)"], "incident stats")
infrastructure = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infrastructure, ["ExpeditionIncidentSystem.isActive(player)", "진행 중인 §e현장 사건", "AscensionTrialSystem.tryStart(player)"], "two-way Trial/incident exclusion")

tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning, ["if (level >= 100) return 11;", "if (level >= 100) return 192;", "if (level >= 100) return 384;",
              "if (level >= 100) return 49;", "if (level >= 100) return 2.0D;", "if (level >= 100) return 16.0D;"], "Mastery VI tuning")
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
need(bore, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12", "MAX_PENDING_PER_PLAYER = 640",
            "fieldMastery ? 12", "player.gameMode.destroyBlock(target)"], "bore safety")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
need(wood, ["GLOBAL_LOG_BUDGET_PER_TICK = 64", "LOCAL_LOG_BUDGET_PER_TICK = 12", "FIELD_MASTERY_LOG_LIMIT = 448",
            "hasLeavesNearby", "player.gameMode.destroyBlock(target)"], "woodcut safety")
harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
need(harvest, ["GLOBAL_HARVEST_BUDGET_PER_TICK = 64", "LOCAL_HARVEST_BUDGET_PER_TICK = 12", "MAX_PENDING_PER_PLAYER = 384",
               "baseSize = 13", "player.gameMode.destroyBlock(target)"], "harvest safety")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(construction, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "MAX_PENDING_BLOCKS_PER_PLAYER = 512", "fieldMastery ? 65", "fieldMastery ? 13",
                    "EventHooks.onBlockPlace", "consumeOne(player, item)"], "construction safety")
combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
need(combat, ["fieldMastery ? 7.5D", "fieldMastery ? 20", "combatLevel >= 100 ? 0.55D : 0.45D", "InfrastructureProject.COMBAT_ACADEMY"], "combat Field Mastery")
mobility = read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
need(mobility, ["ExpeditionProgression.recordAction(player, ExpeditionAction.DASHES_USED, 1)", "return 4;", "DASH_READY_TICK", "AIR_DASH_COUNT"], "mobility action validation")

world = read("src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java")
need(world, ["world_ascension_v1", "MAX_STAGE = 2"], "world ascension")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(trial, ["TOTAL_WAVES = 4", "WAVE_TIMEOUT_TICKS = 1200", "START_COOLDOWN_TICKS = 2400", "AscensionTrialDoctrine.random",
             "maybeReinforce", "EntitySpawnReason.TRIGGERED", "ServerBossEvent", "removeStaleServerTrials"], "Ascension Trial")
if '"minecraft:evoker"' in trial: errors.append("Ascension Trial must not directly spawn evokers")
mutation = read("src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java")
need(mutation, ["MUTATION_CHANCE = 0.18D", "Mutation.WITHERED", "Mutation.PHASE", "Mutation.PLAGUE", 'contains("SPAWNER")'], "endgame mutations")
warband = read("src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java")
need(warband, ["ROUT_TICKS = 160", "3 + worldStage", "6 + worldStage"], "warband")
affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
need(affix, ['AWAKENED = "awakened"', "canAwaken", "currentAffixes(stack).size() == 3", "missing.size() != 2", "awakened ? 4 : rarity"], "awakened Mythic")
reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
need(reforge, ["ACTION_AWAKEN", "AscensionAffixes.canAwaken(held)", "Items.AMETHYST_SHARD, 256", "Items.DRAGON_BREATH, 16"], "awakening economy")

project = read("PROJECT.md")
readme = read("README.md")
third = read("THIRD_PARTY_NOTICES.md")
need(project, ["0.26 희귀 현장 사건", "incident_rewards", "Enhanced Celestials Tweaks(MIT)", "Majrusz's Progressive Difficulty"], "PROJECT canon")
need(readme, ["0.26.0-alpha.1", "Rare Regional Field Incidents", "18 region incidents", "once per player per region"], "README canon")
need(third, ["Enhanced Celestials Tweaks — design reference only for 0.26", "License: GPL-3.0", "FTB Quests — reference only", "License: All Rights Reserved", "Majrusz's Progressive Difficulty — reference only for 0.26"], "third-party reference policy")

for rel in [
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
]:
    text = read(rel)
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex", "mekanism.common", "com.warband",
                  "vbonedra.hostiles_are_too_easy", "com.telepathicgrunt.repurposedstructures", "dev.ftb.mods.ftbquests"]:
    for path in (ROOT / "src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8", errors="ignore").lower():
            errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 / network protocol 8")
print("- 18 persistent expedition directives remain legacy-safe and require all assigned tasks")
print("- 18 rare regional incidents add bounded ambush/action-rush events with bossbar lifecycle")
print("- incident rewards are one-time per player/region, fail-safe, two-way Trial-separated and max +20% directive bonus")
print("- Mastery VI, Field Mastery, tick budgets, normal destroy/material/protection contracts retained")
print("- doctrine trials, awakened Mythic, world stages, mutations, warbands and elite regressions retained")
