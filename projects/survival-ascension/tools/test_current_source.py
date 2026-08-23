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

# Binary files are existence-checked only. Do not UTF-8 decode wrapper jars.
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
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntData.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
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
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.27.0-alpha.1"], "gradle properties")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.27.0-alpha.1"', "ExpeditionIncidentSystem::onPlayerTick", "ExpeditionIncidentSystem::onPlayerLoggedOut",
            "ApexHuntSystem::onServerTick", "ApexHuntSystem::onEntityJoin", "ApexHuntSystem::onPlayerLoggedOut",
            "AscensionTrialSystem::onServerTick", "WarbandDirector::onServerTick"], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

# 0.25 persistent directive regressions
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

# 0.26 incident regressions
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

# 0.27 Stage-1 tracking-post gate
infra_project = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java")
need(infra_project, ["APEX_TRACKING_POST(", '"apex_tracking_post"', '"정점 추적소"', '"전설 단계 · 완수한 원정권에서 반복 정점 사냥 개방", 1',
                     'Items.IRON_INGOT, "철 주괴", 512', 'Items.GOLD_INGOT, "금 주괴", 256',
                     'Items.AMETHYST_SHARD, "자수정 조각", 256', 'Items.ECHO_SHARD, "메아리 조각", 32',
                     'Items.NETHER_STAR, "네더의 별", 1'], "apex tracking post")
infrastructure = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infrastructure, ["project == InfrastructureProject.APEX_TRACKING_POST", "ApexHuntSystem.tryStart(player)",
                      "ApexHuntSystem.isActive(player)", "진행 중인 §e정점 사냥", "AscensionTrialSystem.tryStart(player)"],
     "apex infrastructure flow")
infra_data = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java")
need(infra_data, ['"infrastructure_v1"', "project.id() + \":\" + requirementIndex"], "infrastructure compatibility")

# 0.27 archetype catalog and non-flat behavior identities
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java")
for marker in ["WOODLAND_BREAKER", "ARID_COMMANDER", "WETLAND_PLAGUEHEART", "HIGHLAND_HUNTER", "OCEAN_TYRANT",
               "DEEP_STALKER", "FROZEN_WARDEN", "NETHER_REAVER", "END_HARBINGER"]:
    need(apex, [marker], "nine apex archetypes")
need(apex, ["CHARGE", "REINFORCE", "PLAGUE", "SKIRMISH", "PULL", "LEAP", "FROST", "WITHER", "VOID",
            '"minecraft:ravager"', '"minecraft:elder_guardian"', '"minecraft:wither_skeleton"', '"minecraft:enderman"',
            "double healthBonus", "double armorBonus", "double attackBonus"], "apex archetype behavior/stats")

apex_data = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntData.java")
need(apex_data, ['"apex_hunt_v1"', 'optionalFieldOf("defeated", 0)', 'optionalFieldOf("victories", 0)',
                 'optionalFieldOf("mastery_claimed", false)', "recordVictory", "uniqueDefeated", "allDefeated", "claimMasteryReward"],
     "apex saved data")

apex_system = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
need(apex_system, ["ECHO_SHARD_COST = 8", "AMETHYST_COST = 32", "GOLD_COST = 32",
                   "HUNT_TIMEOUT_TICKS = 1800", "START_COOLDOWN_TICKS = 2400", "OWNER_GRACE_TICKS = 200",
                   "PLAYER_RADIUS = 64.0D", "RECALL_RADIUS = 48.0D", "EXCLUSION_RADIUS = 96.0D",
                   "ExpeditionIncidentSystem.isActive(player)", "ExpeditionData.get(player).isComplete(player, region)",
                   "INCIDENT_READY_TICK_KEY", "TRIAL_READY_TICK_KEY", "ServerBossEvent", "EntitySpawnReason.TRIGGERED",
                   "APEX_OWNER_KEY", "onEntityJoin", "removeStaleServerHunts", "cleanupMobs", "syncBossBarPlayers",
                   "addReinforcements", "chargeExecuteTick", "phaseOneTriggered", "phaseTwoTriggered",
                   "MobEffects.POISON", "boss.heal", "MobEffects.SLOWNESS", "MobEffects.WITHER", "MobEffects.LEVITATION",
                   "nextDouble() < 0.20D", "new ItemStack(Items.NETHERITE_SCRAP, 4)",
                   "new ItemStack(Items.ECHO_SHARD, 32)", "new ItemStack(Items.DRAGON_BREATH, 16)",
                   "player.giveExperiencePoints(50)", "ApexHuntData.get(owner)", "data.claimMasteryReward(owner)"],
     "apex hunt lifecycle/rewards")
need(apex_system, ["AttributeModifier.Operation.ADD_VALUE", "archetype.healthBonus()", "archetype.armorBonus()",
                   "archetype.attackBonus()"], "apex archetype-specific stat construction")

commands = read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")
need(commands, ["ApexHuntData.get(player)", "정점 사냥", "apex.uniqueDefeated(player)", "apex.victories(player)"], "apex stats")

# Physical-scale and safety regressions
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

# Existing endgame/economy regressions
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

# Documentation/reference policy
project = read("PROJECT.md")
readme = read("README.md")
third = read("THIRD_PARTY_NOTICES.md")
need(project, ["0.27 정점 추적소 / Apex Hunts", "apex_hunt_v1", "blanket HP multiplier", "공식 `Shadows-of-Fire/Apotheosis` GitHub"], "PROJECT canon")
need(readme, ["0.27.0-alpha.1", "Apex Tracking Post + Behavior-driven Apex Hunts", "Nine region Apex archetypes", "apex_hunt_v1"], "README canon")
need(third, ["Official GitHub `26.1` code license: MIT License", "Silent Gear — design reference only for 0.27",
             "Enhanced Celestials Tweaks — design reference only for 0.26", "FTB Quests — reference only", "License: All Rights Reserved"],
     "third-party reference policy")

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
print("- 18 persistent expedition directives and 18 one-time-reward regional incidents retained")
print("- Stage-1 Apex Tracking Post opens nine behavior-driven region hunts with bounded lifecycle and resource sinks")
print("- Apex first defeats persist in apex_hunt_v1; 9/9 reward is one-time and Trial remains deterministic Mythic source")
print("- incidents, Apex Hunts and Ascension Trial have explicit overlap guards")
print("- Mastery VI, Field Mastery, tick budgets, normal destroy/material/protection contracts retained")
print("- awakened Mythic, world stages, mutations, warbands and elite regressions retained")
