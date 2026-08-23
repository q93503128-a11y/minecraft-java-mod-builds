#!/usr/bin/env python3
from pathlib import Path
import sys

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

def forbid(text, needles, label):
    for needle in needles:
        if needle in text:
            errors.append(f"{label} forbidden: {needle}")

required = [
    "README.md", "PROJECT.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md", "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionProgram.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = read("gradle.properties")
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.38.0-alpha.1"], "toolchain/version")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, [
    'VERSION = "0.38.0-alpha.1"',
    "OutpostSiegeSystem::onLivingDeath", "OutpostSiegeSystem::onServerTick",
    "OutpostSiegeSystem::onEntityJoin", "OutpostSiegeSystem::onPlayerLoggedOut",
    "FieldRecoveryService::onLivingDeath", "ExpeditionOperationSystem::onLivingDeath",
    "OutpostService::onFinalizeSpawn", "ApexHuntSystem::onServerTick", "AscensionTrialSystem::onServerTick"
], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

# 0.38: an active real outpost becomes a bounded defendable objective.
siege = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java")
need(siege, [
    "START_RADIUS = 4", "DEFENSE_RADIUS = 64", "BREACH_RADIUS = 6", "BREACH_LIMIT = 200", "SUPPLY_CHARGE_COST = 1",
    "TOTAL_WAVES = 3", "SIEGE_TIMEOUT_TICKS = 4800", "OWNER_GRACE_TICKS = 200", "WAVE_DELAY_TICKS = 60", "ENGAGE_RADIUS = 16",
    "OutpostService.nearestActiveOutpost(player, START_RADIUS)", "OutpostService.isRecoveryOperational",
    "EntitySpawnReason.TRIGGERED", "level.hasChunkAt(pos)",
    "siege.breachPressure + breachers * 5", "siege.breachPressure - 10", "siege.breachPressure >= BREACH_LIMIT",
    "mob.setTarget(null)", "mob.getNavigation().moveTo(siege.anchor.getX() + 0.5D",
    '"minecraft:ravager"', '"minecraft:pillager"', '"minecraft:vindicator"', '"minecraft:witch"', '"minecraft:enderman"',
    "SkillProgressionService.award(owner, SkillType.COMBAT, 350)", "SkillProgressionService.award(owner, SkillType.COMBAT, 500)",
    "survivalascension_expedition_incident_ready", "survivalascension_apex_hunt_ready", "survivalascension_ascension_trial_ready",
    "onPlayerLoggedOut", "onEntityJoin", "event.setCanceled(true)"
], "0.38 outpost siege")
forbid(siege, ["setChunkForced", "addRegionTicket", "getChunk(", "Attributes.MAX_HEALTH", "Attributes.ATTACK_DAMAGE", "AttributeModifier"], "0.38 siege safety/stat policy")
ordered(siege, ["if (!spawnWave(siege))", "if (!production.consumeSupplyCharge(player))", "ACTIVE.put(player.getUUID(), siege)"], "0.38 spawn-before-charge/start ordering")

production = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production, [
    'ACTION_OUTPOST_SIEGE = "outpost_siege"',
    "if (ACTION_OUTPOST_SIEGE.equals(action)) { OutpostSiegeSystem.startOrStatus(player); return; }",
    "if (OutpostSiegeSystem.isActive(player))", "진행 중인 §c전초 방어전",
    "OutpostSiegeSystem.sendStatus(player)", "방어1"
], "0.38 production routing")
infra = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra, [
    "ProductionService.ACTION_OUTPOST_SIEGE.equals(action)",
    "if (OutpostSiegeSystem.isActive(player))", "전초 방어전",
    "InfrastructureSiteService.validateForFinalFunding(player, project)",
    "FieldDepotService.countMaterial(player, item)", "FieldDepotService.consume(player, item, amount)"
], "0.38 infrastructure encounter gate")
recovery = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java")
need(recovery, ["OutpostSiegeSystem.isActive(player)", "사건·전초 방어·정점 사냥·승천 시련", "DEATH_RADIUS = 96", "player.teleportTo(", "data.completePending(player)"], "0.38 recovery exclusion")
radial = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(radial, ["전초 방어전", "Action.SIEGE", "case SIEGE -> ProductionService.ACTION_OUTPOST_SIEGE;", "앵커6블록 돌파압력", "전초 건설/방어"], "0.38 radial")

# 0.37 physical warehouse clusters remain compatible inside field_depots_v1.
depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
need(depot_data, [
    '"field_depots_v1"', 'optionalFieldOf("warehouse_links", List.of())',
    "MAX_DEPOTS_PER_PLAYER = 3", "MAX_LINKED_BARRELS_PER_DEPOT = 8", "MAX_LINK_RADIUS = 6",
    "record LinkedBarrel", "linkedBarrels", "linkedCount", "totalLinkedCount", "addLink", "removeLink",
    "isRegisteredAnchor", "isLinkedByOwner", "isLinkedByAny"
], "0.37 warehouse persistence")
field = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(field, [
    "toggleWarehouseNearest", "activeStorageBarrelCount", "FieldDepotData.MAX_LINK_RADIUS", "FieldDepotData.MAX_LINKED_BARRELS_PER_DEPOT",
    "data.addLink(player, depot, target)", "data.removeLink(player, dimension, target)",
    "if (!level.hasChunkAt(pos)) continue;", "level.mayInteract(player, pos)", "blockEntity instanceof Container",
    "resolved.sort(Comparator.comparingDouble", "MAIN_INVENTORY_FIRST_SLOT = 9", "MAIN_INVENTORY_END_EXCLUSIVE = 36",
    "offloadBulkMaterials", "ItemStack.isSameItemSameComponents(source, existing)", "container.canPlaceItem(slot, source)", "source.copyWithCount(move)"
], "0.37 warehouse resolver + 0.35 offload")
forbid(field, ["setChunkForced", "addRegionTicket", "getChunk("], "physical logistics")
warehouse_start = field.find("public static void toggleWarehouseNearest")
warehouse_end = field.find("public static void sendStatus", warehouse_start)
if warehouse_start < 0 or warehouse_end < 0:
    errors.append("0.37 warehouse action boundary missing")
else:
    warehouse_body = field[warehouse_start:warehouse_end]
    forbid(warehouse_body, ["consumeSupplyCharge", "consumeSupplyCharges"], "0.37 warehouse link cost")

# 0.36 commissioning stays old-complete compatible and validates before consumption.
site = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java")
need(site, [
    "ANCHOR_RADIUS = 4", "SITE_RADIUS = 6", "validateForFinalFunding", "Blocks.STONE_BRICKS", "Blocks.IRON_BLOCK",
    "Blocks.LODESTONE", "Blocks.BEACON", "FieldDepotData.get(player).depots(player)", "level.hasChunkAt(pos)",
    "level.mayInteract(player, pos)", "blockEntity instanceof Container"
], "0.36 commissioning")
forbid(site, ["setChunkForced", "addRegionTicket", "getChunk("], "commissioning")
ordered(infra, ["boolean wasComplete = data.isComplete(project);", "if (wasComplete) {", "validateForFinalFunding", "int consumed = 0;"], "0.36 old-complete + validate-before-consume")

# 0.35 offload keeps hotbar/equipment out and finite physical insertion semantics.
offload_start = field.find("public static int offloadBulkMaterials")
insert_start = field.find("private static int insertIntoContainers")
if offload_start < 0 or insert_start < 0:
    errors.append("0.35 offload method boundaries missing")
else:
    offload_body = field[offload_start:insert_start]
    need(offload_body, ["for (int slot = MAIN_INVENTORY_FIRST_SLOT; slot < end; slot++)", "moved += insertIntoContainers(source, containers);"], "0.35 slot boundary")
    if "for (int slot = 0;" in offload_body: errors.append("0.35 offload must not scan hotbar slot0")

# Encounter admission remains physically carried; no remote Barrel payment.
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(apex, ["ECHO_SHARD_COST = 8", "AMETHYST_COST = 32", "GOLD_COST = 32", "count(player, Items.ECHO_SHARD)"], "Apex carried entry")
need(trial, ["ECHO_SHARD_COST = 32", "AMETHYST_COST = 64", "DRAGON_BREATH_COST = 8", "TOTAL_WAVES = 4", "count(player, Items.ECHO_SHARD)"], "Trial carried entry")
forbid(apex, ["FieldDepotService"], "Apex remote payment")
forbid(trial, ["FieldDepotService", '"minecraft:evoker"'], "Trial remote payment/evoker")

# Physical outpost, sortie and complication contracts remain.
outpost = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java")
need(outpost, ["STRUCTURE_RADIUS = 5", "ACTIVE_OWNER_RADIUS = 64", "EXTENDED_SUPPLY_RADIUS = 64", "SAFE_RADIUS = 24", 'if (!"NATURAL".equals(event.getSpawnType().name())) return;', "isRecoveryOperational"], "0.30 outpost")
forbid(outpost, ["setChunkForced", "addRegionTicket", "getChunk("], "outpost")
complication = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java")
need(complication, ['DEEP_FRONT("전선 고착"', 'FORWARD_SHIFT("전선 재전개"', 'HOT_EXTRACTION("긴급 철수"', "case 0 -> 4800;", "case 1 -> 3600;", "default -> 3000;"], "0.33 complications")
operation = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java")
for region in ["WOODLAND", "ARID", "WETLAND", "HIGHLANDS", "OCEAN", "DEEP", "FROZEN", "NETHER", "END"]:
    need(operation, [f"{region}(ExpeditionRegion.{region}"], "nine operation catalog")
opsys = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java")
need(opsys, ["START_RADIUS = 4", "WORK_RADIUS = 48", "RETURN_RADIUS = 8", "FORWARD_SHIFT_EXTRA = 48", "OutpostService.isRecoveryOperational"], "0.32/0.33 operation lifecycle")
forbid(opsys, ["setChunkForced", "addRegionTicket", "getChunk("], "operations")

# Final mastery scale and tick budgets remain bounded.
tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning, ["if (level >= 100) return 11;", "if (level >= 100) return 192;", "if (level >= 100) return 384;", "if (level >= 100) return 49;"], "Mastery VI")
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(bore, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12"], "mining budget")
need(wood, ["GLOBAL_LOG_BUDGET_PER_TICK = 64", "LOCAL_LOG_BUDGET_PER_TICK = 12", "448"], "wood budget/field scale")
need(harvest, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12", "13"], "harvest budget/field scale")
need(construction, ["65", "13"], "construction field scale")

if errors:
    print("SOURCE AUDIT FAIL")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.38 turns an active physical outpost into an explicit three-wave defendable objective with breach pressure")
print("- siege requires active outpost within4, supply1, structure/owner radius64, breach radius6/limit200, loaded TRIGGERED spawns only")
print("- attackers advance toward the physical anchor and only engage a nearby defending owner, preventing long-range kiting from defeating the objective")
print("- stage difficulty changes mob-role composition instead of adding a blanket HP multiplier")
print("- siege excludes incident/operation/Apex/Trial overlap and siege deaths do not consume field recovery")
print("- no new SavedData/packet/client coordinate/force-load; runtime is bounded and cleaned on logout/stale reload")
print("- 0.37 warehouse clusters,0.36 commissioning,0.35 offload,0.34 inputs,0.33 complications,0.32 sorties and older regressions retained")
