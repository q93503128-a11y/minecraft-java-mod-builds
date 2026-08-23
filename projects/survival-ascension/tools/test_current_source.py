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
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntData.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
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
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.31.0-alpha.1"], "gradle properties")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.31.0-alpha.1"', "OutpostService::onFinalizeSpawn",
            "FieldRecoveryService::onLivingDeath", "FieldRecoveryService::onPlayerRespawn",
            "ApexHuntSystem::onServerTick", "ExpeditionIncidentSystem::onPlayerTick",
            "AscensionTrialSystem::onServerTick", "ConstructionProgression::onServerTick",
            "IrrigationReplantService::onServerTick"], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

# 0.28 production remains bounded and atomic.
infra_project = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java")
need(infra_project, ["INDUSTRIAL_WORKS(", '"industrial_works"', '"산업 가공소"',
                     'Items.STONE_BRICKS, "석재 벽돌", 1024', 'Items.IRON_INGOT, "철 주괴", 512',
                     'Items.COPPER_INGOT, "구리 주괴", 512', 'Items.REDSTONE, "레드스톤", 256',
                     'Items.AMETHYST_SHARD, "자수정 조각", 128'], "Industrial Works")
infra_data = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java")
need(infra_data, ['"infrastructure_v1"', 'project.id() + ":" + requirementIndex'], "infrastructure compatibility")
program = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionProgram.java")
need(program, ["METALWORKS(", "TIMBERWORKS(", "PROVISIONS(", "PRECISION(",
               'Input.item(Items.RAW_IRON, "철 원석", 96)', 'Input.item(Items.RAW_COPPER, "구리 원석", 96)',
               'Input.item(Items.COAL, "석탄", 64)', 'Input.tag(ItemTags.LOGS, "통나무", 192)',
               'Input.item(Items.COBBLESTONE, "조약돌", 384)', 'Input.item(Items.IRON_INGOT, "철 주괴", 32)',
               'Input.item(Items.WHEAT, "밀", 128)', 'Input.item(Items.CARROT, "당근", 64)',
               'Input.item(Items.POTATO, "감자", 64)', 'Input.item(Items.BEETROOT, "비트", 32)',
               'Input.item(Items.REDSTONE, "레드스톤", 128)', 'Input.item(Items.AMETHYST_SHARD, "자수정 조각", 64)',
               'Input.item(Items.GOLD_INGOT, "금 주괴", 32)', 'Input.item(Items.QUARTZ, "네더 석영", 64)'], "four production programs")
production_data = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionData.java")
need(production_data, ['"production_v1"', "MAX_BUFFER = 3", "MAX_SUPPLY_CHARGES = 3", "consumeSupplyCharge",
                       "consumeSupplyCharges", "normalizeCycles(state)",
                       "while (state.supplyCharges < MAX_SUPPLY_CHARGES", "state.metalworks--", "state.timberworks--",
                       "state.provisions--", "state.precision--", "state.cycles++", "state.supplyCharges++"],
     "production persistence/cycle")
ordered(production_data, ["public boolean consumeSupplyCharges", "if (state.supplyCharges < amount) return false;",
                          "state.supplyCharges -= amount;", "normalizeCycles(state);", "setDirty();"],
        "atomic multi-charge consumption")
production_service = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production_service, ['ACTION_DEPOT_TOGGLE = "toggle_field_depot"', 'ACTION_OUTPOST_UPGRADE = "upgrade_outpost"',
                          'ACTION_FIELD_RECOVERY = "field_recovery"', "FieldDepotService.toggleNearest(player)",
                          "OutpostService.upgradeNearest(player)", "FieldRecoveryService.configure(player)",
                          "FieldRecoveryService.sendStatus(player)", "new ItemStack(Items.GOLD_INGOT, 32)",
                          "new ItemStack(Items.AMETHYST_SHARD, 16)", "new ItemStack(Items.ECHO_SHARD, 2)"],
     "production service")

# 0.29 physical depot contracts remain real-stock/local.
depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
need(depot_data, ['"field_depots_v1"', "MAX_DEPOTS_PER_PLAYER = 3", "CLAIMED_BY_OTHER", "LIMIT_REACHED",
                  "depot.key().equals(candidate.key())"], "field depot saved data")
field_depot = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(field_depot, ["REGISTER_RADIUS = 4", "SUPPLY_RADIUS = 32", "Blocks.BARREL", "level.mayInteract(player, barrel)",
                   "production.consumeSupplyCharge(player)", "OutpostService.onDepotRemoved", "countMaterial",
                   "public static boolean consume(ServerPlayer player, Item item, int amount)",
                   "depots.sort(Comparator.comparingDouble", "OutpostService.isActiveForLogistics(player, depot)",
                   "OutpostService.EXTENDED_SUPPLY_RADIUS", "level.hasChunkAt(pos)", "stack.shrink(take)", "container.setChanged()"],
     "physical field depot runtime")
if any(token in field_depot for token in ["getChunk(", "setChunkForced", "addRegionTicket"]):
    errors.append("field depots must not force-load chunks")
ordered(field_depot, ["public static boolean consume(ServerPlayer player, Item item, int amount)",
                      "player.getInventory()", "for (Container container : usableContainers(player))"],
        "player inventory must be consumed before depot stock")

# 0.30 physical outpost contracts remain intact.
outpost_data = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostData.java")
need(outpost_data, ['"outpost_v1"', "MAX_OUTPOSTS_PER_PLAYER = FieldDepotData.MAX_DEPOTS_PER_PLAYER",
                    "record OutpostEntry(String dimension, int x, int y, int z)",
                    'OutpostEntry.CODEC.listOf().optionalFieldOf("outposts", List.of())', "Set<String> seen",
                    "sanitized.size() >= MAX_OUTPOSTS_PER_PLAYER", "isOutpost", "upgrade", "remove", "setDirty()"],
     "outpost saved data")
outpost = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java")
need(outpost, ["UPGRADE_RADIUS = 4", "STRUCTURE_RADIUS = 5", "ACTIVE_OWNER_RADIUS = 64",
               "EXTENDED_SUPPLY_RADIUS = 64", "SAFE_RADIUS = 24", "SUPPLY_CHARGE_COST = 2",
               "IRON_COST = 32", "GOLD_COST = 8", "COAL_COST = 32", "nearestOwnedDepot(player, UPGRADE_RADIUS)",
               "level.mayInteract(player, anchor)", "production.consumeSupplyCharges(player, SUPPLY_CHARGE_COST)",
               "FieldDepotService.countMaterial(player, Items.IRON_INGOT)",
               "FieldDepotService.consume(player, Items.IRON_INGOT, IRON_COST)",
               "state.getBlock() instanceof BedBlock", "Blocks.CAMPFIRE", "Blocks.SOUL_CAMPFIRE", "Blocks.CRAFTING_TABLE",
               "Blocks.FURNACE", "Blocks.BLAST_FURNACE", "Blocks.SMOKER", "owner.level() != level",
               'if (!"NATURAL".equals(event.getSpawnType().name())) return;', "event.setCanceled(true)",
               "!level.hasChunkAt(pos) || !level.mayInteract(player, pos)",
               "nearestActiveOutpost", "isRecoveryOperational"], "physical outpost runtime")
if any(token in outpost for token in ["getChunk(", "setChunkForced", "addRegionTicket"]):
    errors.append("outposts must not force-load chunks")
ordered(outpost, ['if (!"NATURAL".equals(event.getSpawnType().name())) return;', "event.setCanceled(true)"],
        "NATURAL-only hostile suppression")
if '"TRIGGERED".equals(event.getSpawnType().name())' in outpost:
    errors.append("outpost safe zone must not cancel TRIGGERED encounter spawns")

# 0.31 one-use death-bound recovery persistence.
recovery_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryData.java")
need(recovery_data, ['"field_recovery_v1"', "record RecoveryPoint(String dimension, int x, int y, int z)",
                     'RecoveryPoint.CODEC.listOf().optionalFieldOf("armed", List.of())',
                     'RecoveryPoint.CODEC.listOf().optionalFieldOf("pending", List.of())',
                     'Codec.INT.optionalFieldOf("recoveries", 0)', "public void arm", "public boolean queuePending",
                     "state.pending = state.armed;", "state.armed = null;", "public void rearmPending",
                     "public void completePending", "state.recoveries++", "setDirty()"], "field recovery SavedData")
ordered(recovery_data, ["public boolean queuePending", "state.pending = state.armed;", "state.armed = null;", "setDirty();"],
        "armed to pending transition")

# 0.31 recovery must be prepaid, death-qualified, challenge-safe and success-consumed.
recovery = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java")
need(recovery, ["ARM_RADIUS = 4", "DEATH_RADIUS = 96", "SUPPLY_CHARGE_COST = 1",
                "OutpostService.nearestActiveOutpost(player, ARM_RADIUS)", "production.consumeSupplyCharge(player)",
                "data.arm(player, outpost.dimension(), outpost.pos())", "data.rearmPending",
                "ExpeditionIncidentSystem.isActive(player)", "ApexHuntSystem.isActive(player)",
                "AscensionTrialSystem.isActive(player)", "armed.dimension().equals(level.dimension().toString())",
                "armed.pos().distSqr(player.blockPosition()) > DEATH_RADIUS * DEATH_RADIUS",
                "OutpostService.isRecoveryOperational", "data.queuePending(player)",
                "server.execute(() -> tryRecoverNow(player, false))", "findSafeArrival",
                "level.hasChunkAt(feet)", "level.mayInteract(player, feet)",
                "floor.isFaceSturdy(level, below, Direction.UP)", "getCollisionShape", "getFluidState",
                "player.teleportTo", "if (!moved)", "data.completePending(player)",
                "player.setDeltaMovement(0.0D, 0.0D, 0.0D)", "player.fallDistance = 0.0F"],
     "field recovery runtime")
ordered(recovery, ["ProductionData production = ProductionData.get(player);", "production.consumeSupplyCharge(player)",
                   "data.arm(player, outpost.dimension(), outpost.pos())"], "prepaid recovery arm")
ordered(recovery, ["ExpeditionIncidentSystem.isActive(player)", "ApexHuntSystem.isActive(player)",
                   "AscensionTrialSystem.isActive(player)", "armed.dimension().equals(level.dimension().toString())",
                   "DEATH_RADIUS * DEATH_RADIUS", "data.queuePending(player)"], "death qualification order")
ordered(recovery, ["boolean moved = player.teleportTo", "if (!moved)", "player.setDeltaMovement", "data.completePending(player)"],
        "pending token consumed only after successful teleport")
if recovery.count("teleportTo(") != 1:
    errors.append("field recovery must have exactly one teleport call")
if any(token in recovery for token in ["getChunk(", "setChunkForced", "addRegionTicket"]):
    errors.append("field recovery must not force-load chunks")
# configure() may call tryRecoverNow only for a pre-existing pending death; it must not directly teleport an armed/live player.
configure_start = recovery.find("public static void configure")
retry_start = recovery.find("private static boolean tryRecoverNow")
if configure_start < 0 or retry_start < 0 or "teleportTo(" in recovery[configure_start:retry_start]:
    errors.append("normal recovery configuration must not directly fast-travel the player")

# Routing/UI/status must expose recovery through the existing packet action only.
infrastructure = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infrastructure, ["ProductionService.ACTION_DEPOT_TOGGLE.equals(action)", "ProductionService.ACTION_OUTPOST_UPGRADE.equals(action)",
                      "ProductionService.ACTION_FIELD_RECOVERY.equals(action)", "ProductionService.perform(player, action)",
                      "ApexHuntSystem.tryStart(player)", "AscensionTrialSystem.tryStart(player)"], "infrastructure routing")
production_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(production_ui, ['"물류 거점 연결"', '"전초기지 승격"', '"현장 복귀 계약"',
                     "new ItemStack(Items.BARREL)", "new ItemStack(Items.CAMPFIRE)", "new ItemStack(Items.COMPASS)",
                     "Action.DEPOT", "Action.OUTPOST", "Action.RECOVERY",
                     "ProductionService.ACTION_FIELD_RECOVERY"], "production radial")
commands = read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")
need(commands, ["FieldDepotData.get(player)", "OutpostData.get(player)", "FieldRecoveryData.get(player)",
                "recovery.pending(player)", "recovery.armed(player)", "recovery.recoveries(player)",
                "OutpostService.activeCount(player)"], "depot/outpost/recovery stats")

# Construction and irrigation keep protection and actual-resource semantics.
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(construction, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "MAX_PENDING_BLOCKS_PER_PLAYER = 512",
                    "FieldDepotService.hasMaterial(player, item)", "EventHooks.onBlockPlace", "level.setBlockAndUpdate(target, state)",
                    "FieldDepotService.consumeOne(player, item)", "level.removeBlock(target, false)"], "construction depot integration")
ordered(construction, ["FieldDepotService.hasMaterial(player, item)", "EventHooks.onBlockPlace", "level.setBlockAndUpdate(target, state)",
                       "FieldDepotService.consumeOne(player, item)"], "construction validation/material order")
irrigation = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java")
need(irrigation, ["REPLANT_BUDGET_PER_TICK = 64", "FieldDepotService.hasMaterial(player, kind.seed())",
                  "EventHooks.onBlockPlace", "level.setBlockAndUpdate(pos, young)", "FieldDepotService.consumeOne(player, kind.seed())",
                  "level.removeBlock(pos, false)"], "irrigation depot integration")

# Expedition/incidents/Apex/Trial regressions.
directive = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java")
for marker in ["WOODLAND_STANDARD", "WOODLAND_PATROL", "ARID_STANDARD", "ARID_ROUTE", "WETLAND_STANDARD", "WETLAND_CLEARANCE",
               "HIGHLANDS_STANDARD", "HIGHLANDS_DASH", "OCEAN_STANDARD", "OCEAN_PATROL", "DEEP_STANDARD", "DEEP_CLEARANCE",
               "FROZEN_STANDARD", "FROZEN_DASH", "NETHER_STANDARD", "NETHER_SUPPLY", "END_STANDARD", "END_TRAVERSE"]:
    need(directive, [marker], "18 directive catalog")
exp_data = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java")
need(exp_data, ['"expedition_v1"', 'optionalFieldOf("region_rewards", -1)', 'optionalFieldOf("incident_rewards", 0)',
                "directiveComplete", "MILESTONE_MASTER"], "expedition migration")
incident_system = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java")
need(incident_system, ["CHECK_INTERVAL_TICKS = 600", "START_CHANCE = 0.10D", "EVENT_RADIUS = 48.0D",
                       "EntitySpawnReason.TRIGGERED", "data.claimIncidentReward", "bonusTask.target() / 5", "cleanupMobs",
                       "public static boolean isActive(ServerPlayer player)"], "incident lifecycle")
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java")
need(apex, ["WOODLAND_BREAKER", "ARID_COMMANDER", "WETLAND_PLAGUEHEART", "HIGHLAND_HUNTER", "OCEAN_TYRANT",
            "DEEP_STALKER", "FROZEN_WARDEN", "NETHER_REAVER", "END_HARBINGER",
            "CHARGE", "REINFORCE", "PLAGUE", "SKIRMISH", "PULL", "LEAP", "FROST", "WITHER", "VOID"], "Apex catalog")
apex_data = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntData.java")
need(apex_data, ['"apex_hunt_v1"', "recordVictory", "claimMasteryReward"], "Apex save")
apex_system = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
need(apex_system, ["HUNT_TIMEOUT_TICKS = 1800", "EntitySpawnReason.TRIGGERED", "data.claimMasteryReward(owner)",
                   "public static boolean isActive(ServerPlayer player)"], "Apex lifecycle")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(trial, ["TOTAL_WAVES = 4", "WAVE_TIMEOUT_TICKS = 1200", "START_COOLDOWN_TICKS = 2400",
             "OWNER_GRACE_TICKS = 200", "WAVE_COUNTS = {8, 10, 10, 12}", "AscensionTrialDoctrine.random",
             "maybeReinforce", "EntitySpawnReason.TRIGGERED", "removeStaleServerTrials",
             "giveExperiencePoints(200)", "createEliteDrop(trial.level.getRandom(), 3)",
             "new ItemStack(Items.NETHERITE_SCRAP, 2)", "new ItemStack(Items.DIAMOND, 4)",
             "public static boolean isActive(ServerPlayer player)"], "Ascension Trial full regression")
if '"minecraft:evoker"' in trial: errors.append("Ascension Trial must not directly spawn evokers")

# Mastery VI / Field Mastery / endgame regressions.
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

# Canon/reference boundaries.
project = read("PROJECT.md")
readme = read("README.md")
third = read("THIRD_PARTY_NOTICES.md")
need(project, ["0.31 Death-bound Field Recovery", "field_recovery_v1", "same-dimension", "within96", "protocol remains8"], "PROJECT canon")
need(readme, ["0.31.0-alpha.1", "Death-bound Field Recovery", "within 96 blocks", "field_recovery_v1", "no button that teleports"], "README canon")
need(third, ["Waystones — reference only for 0.31", "All Rights Reserved", "Corpse — reference only for 0.31", "LGPL-3.0",
             "does not implement general waystone/outpost fast travel", "No Corpse source code"], "third-party policy")

for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex", "mekanism.common", "com.warband",
                  "vbonedra.hostiles_are_too_easy", "com.telepathicgrunt.repurposedstructures", "dev.ftb.mods.ftbquests",
                  "com.simibubi.create", "com.minecolonies", "net.blay09.mods.waystones", "de.maxhenkel.corpse"]:
    for path in (ROOT / "src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8", errors="ignore").lower():
            errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

for rel in [
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
]:
    text = read(rel)
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.28 production, 0.29 real-Barrel logistics and 0.30 physical outposts remain bounded and non-force-loading")
print("- 0.31 field_recovery_v1 is prepaid one-use, same-dimension96 death-bound and challenge-excluded")
print("- pending recovery survives failed validation/teleport and is consumed only after successful safe return")
print("- no general outpost fast travel; no client-supplied destination; no forced chunk load")
print("- Trial four-wave/reward, TRIGGERED encounters, Mastery VI, Field Mastery and Awakened Mythic regressions retained")
