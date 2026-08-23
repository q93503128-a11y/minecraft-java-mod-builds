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
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntData.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java",
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
]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = read("gradle.properties")
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.35.0-alpha.1"], "toolchain/version")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.35.0-alpha.1"', "ExpeditionOperationSystem::onLivingDeath", "ExpeditionOperationSystem::onPlayerTick",
            "FieldRecoveryService::onLivingDeath", "FieldRecoveryService::onPlayerRespawn", "OutpostService::onFinalizeSpawn",
            "ApexHuntSystem::onServerTick", "AscensionTrialSystem::onServerTick"], "main registration")
if "FieldDepotService::on" in main:
    errors.append("0.35 offload must remain explicit and must not add a FieldDepotService background event listener")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

# 0.35: explicit output-side bulk offload into the existing real physical logistics network.
depot = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(depot, [
    "MAIN_INVENTORY_FIRST_SLOT = 9", "MAIN_INVENTORY_END_EXCLUSIVE = 36",
    "countOffloadableMainInventory", "offloadBulkMaterials", "isBulkMaterial", "insertIntoContainers",
    "ItemTags.LOGS", "Items.RAW_IRON", "Items.RAW_GOLD", "Items.COBBLED_DEEPSLATE", "Items.NETHERRACK",
    "Items.WHEAT", "Items.WHEAT_SEEDS", "Items.NETHER_STAR", "Items.DRAGON_BREATH",
    "ItemStack.isSameItemSameComponents(source, existing)", "container.canPlaceItem(slot, source)",
    "container.getMaxStackSize(source)", "existing.getMaxStackSize()", "source.getMaxStackSize()",
    "container.setItem(slot, source.copyWithCount(move))", "source.shrink(move)", "container.setChanged()",
    "player.containerMenu.broadcastChanges()"
], "0.35 explicit bulk offload")
offload_start = depot.find("public static int offloadBulkMaterials")
insert_start = depot.find("private static int insertIntoContainers")
usable_start = depot.find("private static List<Container> usableContainers")
if offload_start < 0 or insert_start < 0 or usable_start < 0:
    errors.append("0.35 offload method boundaries missing")
else:
    offload_body = depot[offload_start:insert_start]
    need(offload_body, ["List<Container> containers = usableContainers(player);",
                        "Math.min(MAIN_INVENTORY_END_EXCLUSIVE, player.getInventory().getContainerSize())",
                        "for (int slot = MAIN_INVENTORY_FIRST_SLOT; slot < end; slot++)",
                        "if (!isBulkMaterial(source)) continue;",
                        "moved += insertIntoContainers(source, containers);"], "0.35 offload main-inventory boundary")
    if "for (int slot = 0;" in offload_body:
        errors.append("0.35 offload must not scan hotbar slot0 or equipment through a full-inventory loop")
    insert_body = depot[insert_start:usable_start]
    ordered(insert_body, ["if (existing.isEmpty()) continue;",
                          "if (!ItemStack.isSameItemSameComponents(source, existing)) continue;",
                          "existing.grow(move);",
                          "if (!existing.isEmpty() || !container.canPlaceItem(slot, source)) continue;",
                          "container.setItem(slot, source.copyWithCount(move));"], "0.35 merge-before-empty insertion")

production = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production, [
    'ACTION_BULK_OFFLOAD = "bulk_offload"', "if (ACTION_BULK_OFFLOAD.equals(action)) { bulkOffload(player); return; }",
    "FieldDepotService.activeDepotCount(player)", "FieldDepotService.countOffloadableMainInventory(player)",
    "FieldDepotService.offloadBulkMaterials(player)", "대상에서 제외", "남은 적재 공간", "핫바/장비 유지",
    "new ItemStack(Items.GOLD_INGOT, 32)", "new ItemStack(Items.AMETHYST_SHARD, 16)", "new ItemStack(Items.ECHO_SHARD, 2)"
], "0.35 production offload action")
infra = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra, ["ProductionService.ACTION_BULK_OFFLOAD.equals(action)", "FieldDepotService.countMaterial(player, item)",
             "FieldDepotService.consume(player, item, amount)"], "0.35 infrastructure action routing")
radial = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(radial, ["현장 일괄 적재", "Items.HOPPER", "Action.OFFLOAD", "case OFFLOAD -> ProductionService.ACTION_BULK_OFFLOAD;",
              "인벤토리+사용 가능 물류 배럴", "채집 → 일괄 적재 → 생산/인프라"], "0.35 radial presentation")

# 0.34: one local physical logistics resolver, inventory first then nearest usable Barrels.
need(depot, [
    "REGISTER_RADIUS = 4", "SUPPLY_RADIUS = 32", "Predicate<ItemStack>",
    "public static int countMatching(ServerPlayer player, Predicate<ItemStack> matcher)",
    "public static boolean consumeMatching(ServerPlayer player, Predicate<ItemStack> matcher, int amount)",
    "return countMatching(player, stack -> stack.is(item));",
    "return consumeMatching(player, stack -> stack.is(item), amount);",
    "matcher.test(stack)", "depots.sort(Comparator.comparingDouble", "OutpostService.EXTENDED_SUPPLY_RADIUS",
    "level.hasChunkAt(pos)", "level.getBlockState(pos).is(Blocks.BARREL)", "level.mayInteract(player, pos)",
    "container.setChanged()", "OutpostService.onDepotRemoved"
], "0.34 physical logistics resolver")
ordered(depot, ["for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)",
                "for (Container container : usableContainers(player))"], "0.34 count inventory-before-depot")
consume_start = depot.find("public static boolean consumeMatching")
if consume_start >= 0:
    consume_body = depot[consume_start:offload_start if offload_start > consume_start else None]
    ordered(consume_body, ["for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++)",
                           "for (Container container : usableContainers(player))"], "0.34 consume inventory-before-depot")
if any(token in depot for token in ["setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("0.34/0.35 physical logistics must not force-load chunks")
need(production, [
    "FieldDepotService.countMatching(player, input::matches)",
    "FieldDepotService.consumeMatching(player, input::matches, amount)",
    "인벤토리 우선 → 가까운 물류 배럴 순으로 투입"
], "0.34 industrial logistics")
if "for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)" in production:
    errors.append("ProductionService must not keep an inventory-only batch counter")
need(infra, [
    "FieldDepotService.countMaterial(player, item)", "FieldDepotService.consume(player, item, amount)",
    "인벤토리 우선 → 가까운 물류 배럴 순으로 인출", "투입원: 인벤토리 + 현재 사용 가능한 등록 배럴/전초 재고",
    "ProductionService.ACTION_FIELD_OPERATION.equals(action)", "ExpeditionOperationSystem.isActive(player)"
], "0.34 infrastructure logistics")
if "stack.shrink(take)" in infra:
    errors.append("InfrastructureService must not keep a direct inventory-only funding loop")
reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
need(reforge, [
    "FieldDepotService.countMaterial(player, cost.item())", "FieldDepotService.consume(player, cost.item(), cost.count())",
    "ACTION_AWAKEN", "Items.AMETHYST_SHARD, 256", "Items.DIAMOND, 24", "Items.NETHERITE_SCRAP, 8",
    "Items.ECHO_SHARD, 64", "Items.DRAGON_BREATH, 16", "salvageRewards"
], "0.34 equipment logistics")

# Deliberate boundary: field encounter admission remains carried inventory only.
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(apex, ["ECHO_SHARD_COST = 8", "AMETHYST_COST = 32", "GOLD_COST = 32",
            "count(player, Items.ECHO_SHARD)", "consume(player, Items.ECHO_SHARD, ECHO_SHARD_COST)"], "Apex carried entry cost")
need(trial, ["ECHO_SHARD_COST = 32", "AMETHYST_COST = 64", "DRAGON_BREATH_COST = 8",
             "count(player, Items.ECHO_SHARD)", "consume(player, Items.DRAGON_BREATH, DRAGON_BREATH_COST)"], "Trial carried entry cost")
if "FieldDepotService" in apex or "FieldDepotService" in trial:
    errors.append("0.34/0.35 must not make Apex/Trial entry a remote Barrel payment")

# 0.29/0.30 physical constraints still gate the resolver.
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
need(depot_data, ['"field_depots_v1"', "MAX_DEPOTS_PER_PLAYER = 3", "CLAIMED_BY_OTHER"], "0.29 depot persistence")
production_data = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionData.java")
need(production_data, ['"production_v1"', "MAX_BUFFER = 3", "MAX_SUPPLY_CHARGES = 3", "consumeSupplyCharge",
                       "consumeSupplyCharges", "normalizeCycles(state)"], "0.28 production persistence")

# 0.33 bounded sortie complication catalog and persistence remain unchanged.
complication = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java")
need(complication, ['NONE("기본 작전"', 'DEEP_FRONT("전선 고착"', 'FORWARD_SHIFT("전선 재전개"', 'HOT_EXTRACTION("긴급 철수"',
                   "case 0 -> 4800;", "case 1 -> 3600;", "default -> 3000;"], "0.33 complication catalog")
opdata = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationData.java")
need(opdata, ['"expedition_operations_v1"', 'optionalFieldOf("complication", "NONE")',
              'optionalFieldOf("complication_state", 0)', 'optionalFieldOf("extraction_deadline", 0L)',
              "ExpeditionComplication.valueOf(entry.complication())", "ExpeditionComplication.NONE",
              "beginForwardShift", "completeForwardShift", "armExtraction", "Math.min(state.deadline, extractionDeadline)",
              "sanitizeComplicationState", "state.totalCompletions++", "state.clearActive()"], "0.33 operation persistence")
operation = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java")
need(operation, [
    'WOODLAND(ExpeditionRegion.WOODLAND, "심림 순환 벌채", 96, 24000',
    'ARID(ExpeditionRegion.ARID, "사막 보급로 개척", 96, 24000',
    'WETLAND(ExpeditionRegion.WETLAND, "습지 채집·소탕", 96, 24000',
    'HIGHLANDS(ExpeditionRegion.HIGHLANDS, "능선 장거리 순찰", 128, 24000',
    'OCEAN(ExpeditionRegion.OCEAN, "외해 순항", 128, 24000',
    'DEEP(ExpeditionRegion.DEEP, "심층 채굴 회수", 128, 30000',
    'FROZEN(ExpeditionRegion.FROZEN, "백설 장거리 순찰", 128, 30000',
    'NETHER(ExpeditionRegion.NETHER, "네더 전진 작전", 160, 30000',
    'END(ExpeditionRegion.END, "공허 외곽 소탕", 160, 36000',
    "public record Task(ExpeditionAction action, int target)"
], "0.32 nine-operation catalog")
if operation.count("ExpeditionRegion.") < 9:
    errors.append("operation catalog must contain all nine regions")
opsys = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java")
need(opsys, ["START_RADIUS = 4", "WORK_RADIUS = 48", "RETURN_RADIUS = 8", "FORWARD_SHIFT_EXTRA = 48",
              "OutpostService.nearestActiveOutpost(player, START_RADIUS)", "production.consumeSupplyCharge(player)",
              "ExpeditionComplication complication = chooseComplication", "data.markRangeReached(player)",
              "if (distanceSq < WORK_RADIUS * WORK_RADIUS) return;", "ExpeditionProgression.currentRegion(player) != active.region()",
              "ExpeditionComplication.DEEP_FRONT", "ExpeditionComplication.FORWARD_SHIFT", "ExpeditionComplication.HOT_EXTRACTION",
              "operation.rangeTarget() + FORWARD_SHIFT_EXTRA", "data.beginForwardShift", "data.completeForwardShift",
              "data.armExtraction", "data.addProgress", "distanceSq <= RETURN_RADIUS * RETURN_RADIUS",
              "OutpostService.isRecoveryOperational", 'fail(player, "작전 중 사망했습니다.',
              'fail(player, "작전 중 다른 차원으로 이탈했습니다.")', 'fail(player, "작전 제한시간을 초과했습니다.")'], "0.32/0.33 operation lifecycle")
if any(token in opsys for token in ["teleportTo(", "setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("operations must not teleport or force-load chunks")
ordered(opsys, ["if (distanceSq < WORK_RADIUS * WORK_RADIUS) return;",
                "if (ExpeditionProgression.currentRegion(player) != active.region()) return;",
                "ExpeditionComplication.DEEP_FRONT", "ExpeditionComplication.FORWARD_SHIFT", "data.addProgress"],
        "operation gate order")
ordered(opsys, ["data.objectivesComplete(player, operation)", "distanceSq <= RETURN_RADIUS * RETURN_RADIUS",
                "OutpostService.isRecoveryOperational", "complete(player, operation)"], "physical return gate")
progression = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")
need(progression, ["ExpeditionIncidentSystem.recordAction(player, action, amount);",
                   "ExpeditionOperationSystem.recordAction(player, action, amount);",
                   "ExpeditionIncidentSystem.recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount);",
                   "ExpeditionOperationSystem.recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount);"], "expedition action plumbing")

# 0.31 recovery remains one prepaid bounded death return.
recovery_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryData.java")
need(recovery_data, ['"field_recovery_v1"', 'optionalFieldOf("armed", List.of())', 'optionalFieldOf("pending", List.of())',
                     "state.pending = state.armed", "state.armed = null", "state.recoveries++"], "0.31 recovery data")
recovery = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java")
need(recovery, ["DEATH_RADIUS = 96", "SUPPLY_CHARGE_COST = 1", "production.consumeSupplyCharge(player)",
                "ExpeditionIncidentSystem.isActive(player)", "ApexHuntSystem.isActive(player)", "AscensionTrialSystem.isActive(player)",
                "data.queuePending(player)", "player.teleportTo(", "data.completePending(player)"], "0.31 recovery")
ordered(recovery, ["boolean moved = player.teleportTo", "if (!moved)", "data.completePending(player)"], "recovery consume after successful teleport")
if any(token in recovery for token in ["setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("field recovery must not force-load chunks")

# Expeditions/incidents and encounter patterns remain.
directive = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java")
for marker in ["WOODLAND_STANDARD", "WOODLAND_PATROL", "ARID_STANDARD", "ARID_ROUTE", "WETLAND_STANDARD", "WETLAND_CLEARANCE",
               "HIGHLANDS_STANDARD", "HIGHLANDS_DASH", "OCEAN_STANDARD", "OCEAN_PATROL", "DEEP_STANDARD", "DEEP_CLEARANCE",
               "FROZEN_STANDARD", "FROZEN_DASH", "NETHER_STANDARD", "NETHER_SUPPLY", "END_STANDARD", "END_TRAVERSE"]:
    need(directive, [marker], "18 directive catalog")
incident = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java")
need(incident, ["CHECK_INTERVAL_TICKS = 600", "START_CHANCE = 0.10D", "EVENT_RADIUS = 48.0D",
                "EntitySpawnReason.TRIGGERED", "data.claimIncidentReward", "cleanupMobs"], "incident lifecycle")
apex_catalog = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java")
need(apex_catalog, ["WOODLAND_BREAKER", "ARID_COMMANDER", "WETLAND_PLAGUEHEART", "HIGHLAND_HUNTER", "OCEAN_TYRANT",
                    "DEEP_STALKER", "FROZEN_WARDEN", "NETHER_REAVER", "END_HARBINGER",
                    "CHARGE", "REINFORCE", "PLAGUE", "SKIRMISH", "PULL", "LEAP", "FROST", "WITHER", "VOID"], "Apex catalog")
need(apex, ["HUNT_TIMEOUT_TICKS = 1800", "EntitySpawnReason.TRIGGERED", "data.claimMasteryReward(owner)"], "Apex lifecycle")
need(trial, ["TOTAL_WAVES = 4", "WAVE_TIMEOUT_TICKS = 1200", "EntitySpawnReason.TRIGGERED", "removeStaleServerTrials",
             "public static boolean isActive(ServerPlayer player)"], "Ascension Trial")
if '"minecraft:evoker"' in trial:
    errors.append("Ascension Trial must not directly spawn evokers")

# Mastery VI / Field Mastery and tick-budget regressions.
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

# UI/canon lock.
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(guide, ["통합 물류 백본 / 현장 일괄 적재", "현장 일괄 적재", "슬롯9~35", "핫바0~8", "청크 강제로드",
             "전선 고착", "전선 재전개", "긴급 철수", "Stage0 4:00 / Stage1 3:00 / Stage2 2:30"], "0.35 guide")
readme = read("README.md")
project = read("PROJECT.md")
changelog = read("CHANGELOG.md")
third = read("THIRD_PARTY_NOTICES.md")
need(readme, ["0.35.0-alpha.1", "High-volume Field Offload", "slots `9..35`", "Hotbar slots `0..8`",
              "matching existing stacks", "canPlaceItem", "No new SavedData", "0.34.0-alpha.1", "0.33.0-alpha.1"], "README canon")
need(project, ["0.35 High-volume Field Offload", "MAIN_INVENTORY_FIRST_SLOT = 9", "MAIN_INVENTORY_END_EXCLUSIVE = 36",
               "ItemStack.isSameItemSameComponents", "Container.canPlaceItem", "no new SavedData", "protocol remains8",
               "0.34 Integrated Logistics Backbone", "0.33 Sortie Complications"], "PROJECT canon")
need(changelog, ["0.35.0-alpha.1", "ACTION_BULK_OFFLOAD", "slots9..35", "nearest-first", "isSameItemSameComponents",
                 "no item-pickup hook", "0.34.0-alpha.1"], "CHANGELOG canon")
need(third, ["Deep Rock Galactic — product reference only for 0.33", "Warframe — product reference only for 0.33",
             "Heracles — design reference only for 0.32", "Bountiful — reference only for 0.24+"], "third-party policy")

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
print("- 0.35 adds explicit main-inventory bulk offload into the existing nearby physical Barrel network")
print("- slots9..35 only; hotbar0..8/equipment/offhand preserved; authored bulk-material whitelist only")
print("- nearest usable Barrel first, merge-before-empty, same components, slot acceptance and finite stack capacity respected")
print("- full/partial capacity preserves every unaccepted source remainder; no automatic pickup/tick routing")
print("- 0.34 integrated inputs,0.33 complications,0.32 nine sorties,0.31 recovery,0.30 outpost and older regressions retained")
