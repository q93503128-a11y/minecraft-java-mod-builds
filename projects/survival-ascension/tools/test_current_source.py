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

def ordered(text, needles, label):
    pos = -1
    for needle in needles:
        nxt = text.find(needle, pos + 1)
        if nxt < 0:
            errors.append(f"{label} missing/order: {needle}")
            return
        pos = nxt

required = [
    "README.md", "PROJECT.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md",
    "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
    "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionProgram.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntData.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = read("gradle.properties")
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.32.0-alpha.1"], "toolchain/version")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.32.0-alpha.1"', "ExpeditionOperationSystem::onLivingDeath",
            "ExpeditionOperationSystem::onPlayerTick", "ExpeditionOperationSystem::onPlayerLoggedIn",
            "FieldRecoveryService::onLivingDeath", "FieldRecoveryService::onPlayerRespawn",
            "OutpostService::onFinalizeSpawn", "ApexHuntSystem::onServerTick", "AscensionTrialSystem::onServerTick"], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

# 0.32 exact authored operation catalog.
operation = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java")
operation_markers = [
    'WOODLAND(ExpeditionRegion.WOODLAND, "심림 순환 벌채", 96, 24000',
    'new Task(ExpeditionAction.LOGS_FELLED, 128), new Task(ExpeditionAction.TRAVEL_DISTANCE, 240)',
    'ARID(ExpeditionRegion.ARID, "사막 보급로 개척", 96, 24000',
    'new Task(ExpeditionAction.BLOCKS_BUILT, 96), new Task(ExpeditionAction.TRAVEL_DISTANCE, 240)',
    'WETLAND(ExpeditionRegion.WETLAND, "습지 채집·소탕", 96, 24000',
    'new Task(ExpeditionAction.CROPS_HARVESTED, 80), new Task(ExpeditionAction.HOSTILES_KILLED, 8)',
    'HIGHLANDS(ExpeditionRegion.HIGHLANDS, "능선 장거리 순찰", 128, 24000',
    'new Task(ExpeditionAction.TRAVEL_DISTANCE, 600), new Task(ExpeditionAction.DASHES_USED, 12)',
    'OCEAN(ExpeditionRegion.OCEAN, "외해 순항", 128, 24000',
    'new Task(ExpeditionAction.OCEAN_VOYAGE, 900), new Task(ExpeditionAction.HOSTILES_KILLED, 8)',
    'DEEP(ExpeditionRegion.DEEP, "심층 채굴 회수", 128, 30000',
    'new Task(ExpeditionAction.BLOCKS_MINED, 192), new Task(ExpeditionAction.HOSTILES_KILLED, 10)',
    'FROZEN(ExpeditionRegion.FROZEN, "백설 장거리 순찰", 128, 30000',
    'new Task(ExpeditionAction.TRAVEL_DISTANCE, 600), new Task(ExpeditionAction.HOSTILES_KILLED, 10)',
    'NETHER(ExpeditionRegion.NETHER, "네더 전진 작전", 160, 30000',
    'new Task(ExpeditionAction.HOSTILES_KILLED, 24), new Task(ExpeditionAction.BLOCKS_MINED, 96)',
    'END(ExpeditionRegion.END, "공허 외곽 소탕", 160, 36000',
    'new Task(ExpeditionAction.HOSTILES_KILLED, 28), new Task(ExpeditionAction.TRAVEL_DISTANCE, 360)',
    "public record Task(ExpeditionAction action, int target)"
]
need(operation, operation_markers, "0.32 operation catalog")
if operation.count("ExpeditionRegion.") < 9:
    errors.append("operation catalog must contain all nine regions")

# 0.32 persistence: one active sortie, exact origin, deadline, progress, first-clears.
opdata = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationData.java")
need(opdata, ['"expedition_operations_v1"', "ALL_REGIONS_MASK", 'optionalFieldOf("active_region", "")',
              'optionalFieldOf("dimension", "")', 'optionalFieldOf("deadline", 0L)',
              'optionalFieldOf("range_reached", false)', 'optionalFieldOf("progress_a", 0)',
              'optionalFieldOf("progress_b", 0)', 'optionalFieldOf("completed_mask", 0)',
              'optionalFieldOf("total_completions", 0)', 'optionalFieldOf("mastery_claimed", false)',
              "ExpeditionRegion.valueOf", "catch (IllegalArgumentException", "clamp(entry.progressA()",
              "clamp(entry.progressB()", "if (state.activeRegion != null || deadline <= 0L) return false;",
              "state.rangeReached = true", "state.completedMask |= region.bit()", "state.totalCompletions++",
              "state.clearActive();", "Integer.bitCount(state.completedMask)"], "0.32 operation persistence")
ordered(opdata, ["public CompletionResult complete", "state.totalCompletions++", "state.clearActive();", "setDirty();"],
        "operation completion persistence")

opsys = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java")
need(opsys, ["START_RADIUS = 4", "WORK_RADIUS = 48", "RETURN_RADIUS = 8", "SUPPLY_CHARGE_COST = 1",
              "OutpostService.nearestActiveOutpost(player, START_RADIUS)", "expedition.isComplete(player, region)",
              "production.consumeSupplyCharge(player)", "ApexHuntSystem.isActive(player)", "AscensionTrialSystem.isActive(player)",
              "level.getGameTime() + operation.durationTicks()", "distanceSq >= operation.rangeTarget() * operation.rangeTarget()",
              "data.markRangeReached(player)", "active.anchor().distSqr(player.blockPosition()) < WORK_RADIUS * WORK_RADIUS",
              "ExpeditionProgression.currentRegion(player) != active.region()", "data.addProgress(player, i, amount, task.target())",
              "data.objectivesComplete(player, operation)", "distanceSq <= RETURN_RADIUS * RETURN_RADIUS",
              "OutpostService.isRecoveryOperational(player, level, active.dimension(), active.anchor())",
              'fail(player, "작전 중 다른 차원으로 이탈했습니다.")', 'fail(player, "작전 제한시간을 초과했습니다.")',
              'fail(player, "게임 모드가 변경되어 작전이 종료되었습니다.")', 'fail(player, "작전 중 사망했습니다.',
              "SkillProgressionService.award(player, operation.region().rewardSkill(), operation.skillXpReward())",
              "new ItemStack(Items.NETHERITE_SCRAP, 2)", "new ItemStack(Items.ECHO_SHARD, 16)",
              "new ItemStack(Items.AMETHYST_SHARD, 64)", "new ItemStack(Items.DRAGON_BREATH, 8)",
              "player.giveExperiencePoints(300)"], "0.32 operation lifecycle")
if any(token in opsys for token in ["teleportTo(", "setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("0.32 operations must not teleport or force-load chunks")
ordered(opsys, ["if (active.anchor().distSqr(player.blockPosition()) < WORK_RADIUS * WORK_RADIUS) return;",
                "if (ExpeditionProgression.currentRegion(player) != active.region()) return;", "data.addProgress"],
        "operation work gate order")
ordered(opsys, ["data.objectivesComplete(player, operation)", "distanceSq <= RETURN_RADIUS * RETURN_RADIUS",
                "OutpostService.isRecoveryOperational", "complete(player, operation)"], "physical return gate")

# Existing validated actions feed operations, including dedicated ocean voyage.
progression = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")
need(progression, ["ExpeditionIncidentSystem.recordAction(player, action, amount);",
                   "ExpeditionOperationSystem.recordAction(player, action, amount);",
                   "ExpeditionIncidentSystem.recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount);",
                   "ExpeditionOperationSystem.recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount);"],
     "operation action plumbing")

# Routing/UI/status and encounter mutual exclusion.
production_service = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production_service, ['ACTION_FIELD_OPERATION = "field_operation"', "ExpeditionOperationSystem.startOrStatus(player)",
                          "ExpeditionOperationSystem.sendStatus(player)", "consumeSupplyCharge"], "operation production routing")
ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(ui, ['"원정 작전"', "new ItemStack(Items.SPYGLASS)", "Action.OPERATION", "ProductionService.ACTION_FIELD_OPERATION"],
     "operation radial UI")
infra = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra, ["ProductionService.ACTION_FIELD_OPERATION.equals(action)", "ExpeditionOperationSystem.isActive(player)",
             '§4[정점 사냥]', '§5[승천 시련]'], "operation encounter exclusion")
commands = read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")
need(commands, ["ExpeditionOperationData.get(player)", "operations.uniqueCompleted(player)", "operations.totalCompletions(player)",
                '§6[원정 작전]'], "operation stats")
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(guide, ["반복 원정 작전", "전진선", "전초48블록 밖", "같은 전초8블록"], "operation guide")

# 0.31 recovery regression: prepaid one-use, same-dim96, challenge-excluded, consume after successful teleport.
recovery_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryData.java")
need(recovery_data, ['"field_recovery_v1"', 'optionalFieldOf("armed", List.of())', 'optionalFieldOf("pending", List.of())',
                     "state.pending = state.armed", "state.armed = null", "state.recoveries++"], "0.31 recovery data")
recovery = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java")
need(recovery, ["DEATH_RADIUS = 96", "SUPPLY_CHARGE_COST = 1", "production.consumeSupplyCharge(player)",
                "ExpeditionIncidentSystem.isActive(player)", "ApexHuntSystem.isActive(player)", "AscensionTrialSystem.isActive(player)",
                "armed.pos().distSqr(player.blockPosition()) > DEATH_RADIUS * DEATH_RADIUS",
                "data.queuePending(player)", "player.teleportTo(", "if (!moved)", "data.completePending(player)"], "0.31 recovery")
ordered(recovery, ["boolean moved = player.teleportTo", "if (!moved)", "data.completePending(player)"],
        "recovery consumes token only after successful teleport")
if any(token in recovery for token in ["setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("field recovery must not force-load chunks")

# 0.30 physical outpost / 0.29 real-Barrel logistics regressions.
outpost = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java")
need(outpost, ["STRUCTURE_RADIUS = 5", "ACTIVE_OWNER_RADIUS = 64", "EXTENDED_SUPPLY_RADIUS = 64", "SAFE_RADIUS = 24",
               "SUPPLY_CHARGE_COST = 2", "IRON_COST = 32", "GOLD_COST = 8", "COAL_COST = 32",
               "state.getBlock() instanceof BedBlock", "Blocks.CAMPFIRE", "Blocks.CRAFTING_TABLE", "Blocks.FURNACE",
               'if (!"NATURAL".equals(event.getSpawnType().name())) return;', "event.setCanceled(true)",
               "nearestActiveOutpost", "isRecoveryOperational"], "0.30 outpost")
if any(token in outpost for token in ["setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("outposts must not force-load chunks")
if '"TRIGGERED".equals(event.getSpawnType().name())' in outpost:
    errors.append("outpost safe zone must not cancel TRIGGERED spawns")

depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
need(depot_data, ['"field_depots_v1"', "MAX_DEPOTS_PER_PLAYER = 3", "CLAIMED_BY_OTHER"], "0.29 depot data")
depot = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(depot, ["REGISTER_RADIUS = 4", "SUPPLY_RADIUS = 32", "Blocks.BARREL", "level.mayInteract(player, barrel)",
             "OutpostService.EXTENDED_SUPPLY_RADIUS", "stack.shrink(take)", "container.setChanged()"], "0.29 depot runtime")
if any(token in depot for token in ["setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("field depots must not force-load chunks")

production_data = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionData.java")
need(production_data, ['"production_v1"', "MAX_BUFFER = 3", "MAX_SUPPLY_CHARGES = 3", "consumeSupplyCharge",
                       "consumeSupplyCharges", "normalizeCycles(state)"], "0.28 production")

# Original expedition/directive/incident behavior remains intact.
directive = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java")
for marker in ["WOODLAND_STANDARD", "WOODLAND_PATROL", "ARID_STANDARD", "ARID_ROUTE", "WETLAND_STANDARD", "WETLAND_CLEARANCE",
               "HIGHLANDS_STANDARD", "HIGHLANDS_DASH", "OCEAN_STANDARD", "OCEAN_PATROL", "DEEP_STANDARD", "DEEP_CLEARANCE",
               "FROZEN_STANDARD", "FROZEN_DASH", "NETHER_STANDARD", "NETHER_SUPPLY", "END_STANDARD", "END_TRAVERSE"]:
    need(directive, [marker], "18 directive catalog")
expdata = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java")
need(expdata, ['"expedition_v1"', 'optionalFieldOf("region_rewards", -1)', 'optionalFieldOf("incident_rewards", 0)',
               "directiveComplete", "MILESTONE_MASTER"], "expedition migration")
incident = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java")
need(incident, ["CHECK_INTERVAL_TICKS = 600", "START_CHANCE = 0.10D", "EVENT_RADIUS = 48.0D",
                "EntitySpawnReason.TRIGGERED", "data.claimIncidentReward", "cleanupMobs"], "incident lifecycle")

# Apex and Ascension Trial regressions.
apex_catalog = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java")
need(apex_catalog, ["WOODLAND_BREAKER", "ARID_COMMANDER", "WETLAND_PLAGUEHEART", "HIGHLAND_HUNTER", "OCEAN_TYRANT",
                    "DEEP_STALKER", "FROZEN_WARDEN", "NETHER_REAVER", "END_HARBINGER",
                    "CHARGE", "REINFORCE", "PLAGUE", "SKIRMISH", "PULL", "LEAP", "FROST", "WITHER", "VOID"], "Apex catalog")
apex_system = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
need(apex_system, ["HUNT_TIMEOUT_TICKS = 1800", "EntitySpawnReason.TRIGGERED", "data.claimMasteryReward(owner)"], "Apex lifecycle")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(trial, ["TOTAL_WAVES = 4", "WAVE_TIMEOUT_TICKS = 1200", "EntitySpawnReason.TRIGGERED", "removeStaleServerTrials",
             "public static boolean isActive(ServerPlayer player)"], "Ascension Trial")
if '"minecraft:evoker"' in trial:
    errors.append("Ascension Trial must not directly spawn evokers")

# Mastery VI / Field Mastery / queue safety / awakened Mythic regressions.
tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning, ["if (level >= 100) return 11;", "if (level >= 100) return 192;", "if (level >= 100) return 384;",
              "if (level >= 100) return 49;", "if (level >= 100) return 2.0D;", "if (level >= 100) return 16.0D;"], "Mastery VI")
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
need(bore, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12", "MAX_PENDING_PER_PLAYER = 640",
            "fieldMastery ? 12", "player.gameMode.destroyBlock(target)"], "bore safety")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
need(wood, ["GLOBAL_LOG_BUDGET_PER_TICK = 64", "LOCAL_LOG_BUDGET_PER_TICK = 12", "FIELD_MASTERY_LOG_LIMIT = 448"], "wood safety")
harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
need(harvest, ["GLOBAL_HARVEST_BUDGET_PER_TICK = 64", "LOCAL_HARVEST_BUDGET_PER_TICK = 12", "baseSize = 13"], "harvest safety")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(construction, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "MAX_PENDING_BLOCKS_PER_PLAYER = 512",
                    "FieldDepotService.consumeOne(player, item)", "EventHooks.onBlockPlace"], "construction safety")
irrigation = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java")
need(irrigation, ["REPLANT_BUDGET_PER_TICK = 64", "FieldDepotService.consumeOne(player, kind.seed())", "EventHooks.onBlockPlace"], "irrigation safety")
combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
need(combat, ["fieldMastery ? 7.5D", "fieldMastery ? 20", "combatLevel >= 100 ? 0.55D : 0.45D"], "combat Field Mastery")
mobility = read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
need(mobility, ["return 4;", "DASH_READY_TICK", "AIR_DASH_COUNT"], "mobility Field Mastery")
mutation = read("src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java")
need(mutation, ["MUTATION_CHANCE = 0.18D", "Mutation.WITHERED", "Mutation.PHASE", "Mutation.PLAGUE"], "mutations")
warband = read("src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java")
need(warband, ["ROUT_TICKS = 160", "3 + worldStage", "6 + worldStage"], "warband")
affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
need(affix, ['AWAKENED = "awakened"', "currentAffixes(stack).size() == 3", "missing.size() != 2", "awakened ? 4 : rarity"], "awakened Mythic")
reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
need(reforge, ["ACTION_AWAKEN", "Items.AMETHYST_SHARD, 256", "Items.DRAGON_BREATH, 16"], "awakening economy")

# Canon/reference policy.
readme = read("README.md")
project = read("PROJECT.md")
third = read("THIRD_PARTY_NOTICES.md")
need(readme, ["0.32.0-alpha.1", "Out-and-back Expedition Operations", "expedition_operations_v1",
              "심림 순환 벌채", "공허 외곽 소탕", "within 8 blocks", "Heracles"], "README canon")
need(project, ["0.32 Out-and-back Expedition Operations", "expedition_operations_v1", "WORK_RADIUS", "within8", "protocol remains8"], "PROJECT canon")
need(third, ["Heracles — design reference only for 0.32", "Copyright (c) 2023 Terrarium Earth", "License: MIT License",
             "No Heracles quest data", "Bountiful — reference only for 0.24+", "License: GPL-3.0"], "third-party policy")

for forbidden in ["harmonised.pmmo", "alrex.parcool", "mekanism.common", "com.warband",
                  "vbonedra.hostiles_are_too_easy", "com.telepathicgrunt.repurposedstructures", "dev.ftb.mods.ftbquests",
                  "com.simibubi.create", "com.minecolonies", "net.blay09.mods.waystones", "de.maxhenkel.corpse",
                  "earth.terrarium.heracles", "terrarium.heracles", "io.ejekta.bountiful"]:
    for path in (ROOT / "src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8", errors="ignore").lower():
            errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

for rel in [
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
]:
    text = read(rel)
    if re.search(r"setBlock\s*\([^\n]*AIR", text):
        errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.32 expedition_operations_v1 persists one out-and-back sortie with exact origin, deadline and bounded progress")
print("- all nine operation profiles,96/128/160 outbound gates,48-block work gate and8-block physical return are locked")
print("- operation actions reuse validated expedition hooks; no teleport, client destination or chunk force-load is introduced")
print("- operation death/dimension/time/game-mode failures and Apex/Trial mutual exclusion are enforced")
print("- 0.31 recovery,0.30 outpost,0.29 Barrel logistics, directives/incidents,Apex/Trial and mastery regressions retained")
