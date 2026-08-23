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

def forbid(text, needles, label):
    for needle in needles:
        if needle in text:
            errors.append(f"{label} forbidden: {needle}")

def ordered(text, needles, label):
    pos = -1
    for needle in needles:
        nxt = text.find(needle, pos + 1)
        if nxt < 0:
            errors.append(f"{label} missing/order: {needle}")
            return
        pos = nxt

required = [
    "README.md", "PROJECT.md", "CHANGELOG.md", "gradle.properties",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostFortificationService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = read("gradle.properties")
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.39.0-alpha.1"], "toolchain/version")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.39.0-alpha.1"',
    "OutpostSiegeSystem::onLivingDeath", "OutpostSiegeSystem::onServerTick",
    "OutpostSiegeSystem::onEntityJoin", "OutpostSiegeSystem::onPlayerLoggedOut",
    "FieldRecoveryService::onLivingDeath", "ExpeditionOperationSystem::onLivingDeath",
    "OutpostService::onFinalizeSpawn", "ApexHuntSystem::onServerTick", "AscensionTrialSystem::onServerTick"], "main registration")

# 0.39 physical bastion construction contract.
fort = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostFortificationService.java")
need(fort, [
    "INNER_RADIUS = 6", "OUTER_RADIUS = 12", "VERTICAL_DOWN = 3", "VERTICAL_UP = 4",
    "MIN_COLUMNS_PER_QUADRANT = 12", "MIN_TOTAL_COLUMNS = MIN_COLUMNS_PER_QUADRANT * 4",
    "BlockTags.WALLS", "Blocks.IRON_BARS", "Blocks.NETHER_BRICK_FENCE",
    "OutpostService.isRecoveryOperational", "level.hasChunkAt(pos)", "fortifiedColumn",
    "if (dx >= 0 && dz < 0) northEast++", "else if (dx < 0 && dz < 0) northWest++",
    "else if (dx >= 0) southEast++", "else southWest++",
    "northEast >= MIN_COLUMNS_PER_QUADRANT", "northWest >= MIN_COLUMNS_PER_QUADRANT",
    "southEast >= MIN_COLUMNS_PER_QUADRANT", "southWest >= MIN_COLUMNS_PER_QUADRANT"
], "0.39 physical fortification")
forbid(fort, ["SavedData", "setChunkForced", "addRegionTicket", "getChunk("], "0.39 fortification safety")
ordered(fort, ["for (int dx = -OUTER_RADIUS", "for (int dz = -OUTER_RADIUS", "if (!fortifiedColumn", "northEast++"], "0.39 one-column distributed scan")

# 0.39/0.38 shared siege runtime.
siege = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java")
need(siege, [
    "START_RADIUS = 4", "DEFENSE_RADIUS = 64", "BREACH_RADIUS = 6", "BREACH_LIMIT = 200",
    "SUPPLY_CHARGE_COST = 1", "BASTION_SUPPLY_CHARGE_COST = 2",
    "TOTAL_WAVES = 3", "BASTION_TOTAL_WAVES = 4",
    "SIEGE_TIMEOUT_TICKS = 4800", "BASTION_TIMEOUT_TICKS = 6000",
    "OWNER_GRACE_TICKS = 200", "WAVE_DELAY_TICKS = 60", "ENGAGE_RADIUS = 16",
    "startBastionOrStatus", "SiegeMode.OUTPOST", "SiegeMode.BASTION",
    "OutpostFortificationService.validateForBastion(player, outpost, true)",
    "ProductionData production = ProductionData.get(player)", "production.supplyCharges(player) < mode.supplyCost",
    "production.consumeSupplyCharges(player, mode.supplyCost)",
    "OutpostService.nearestActiveOutpost(player, START_RADIUS)", "OutpostService.isRecoveryOperational",
    "EntitySpawnReason.TRIGGERED", "level.hasChunkAt(pos)",
    "siege.breachPressure + breachers * 5", "siege.breachPressure - 10", "siege.breachPressure >= BREACH_LIMIT",
    "ownerDefendingAnchor", "mob.setTarget(null)", "mob.getNavigation().moveTo(siege.anchor.getX() + 0.5D",
    "OutpostFortificationService.validateForBastion(owner", "다음 공세 전에 실제 방어진지 사분면 조건이 무너졌습니다",
    "bastionWaveTypes", '"minecraft:ravager"', '"minecraft:pillager"', '"minecraft:vindicator"', '"minecraft:witch"', '"minecraft:enderman"',
    "SkillProgressionService.award(owner, SkillType.COMBAT, 650)", "SkillProgressionService.award(owner, SkillType.CONSTRUCTION, 250)",
    "SkillProgressionService.award(owner, SkillType.COMBAT, 900)", "SkillProgressionService.award(owner, SkillType.CONSTRUCTION, 350)",
    "new ItemStack(Items.NETHERITE_SCRAP, 1)", "allyXp = siege.mode == SiegeMode.BASTION ? 70 : 40",
    "survivalascension_expedition_incident_ready", "survivalascension_apex_hunt_ready", "survivalascension_ascension_trial_ready",
    "onPlayerLoggedOut", "onEntityJoin", "event.setCanceled(true)"
], "0.39 bastion + 0.38 siege runtime")
forbid(siege, ["setChunkForced", "addRegionTicket", "getChunk(", "Attributes.MAX_HEALTH", "Attributes.ATTACK_DAMAGE", "AttributeModifier"], "siege force-load/stat policy")
ordered(siege, ["if (!spawnWave(siege))", "if (!production.consumeSupplyCharges(player, mode.supplyCost))", "ACTIVE.put(player.getUUID(), siege)"], "spawn-before-supply consumption")

production = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production, [
    'ACTION_OUTPOST_SIEGE = "outpost_siege"', 'ACTION_BASTION_SIEGE = "bastion_siege"',
    "if (ACTION_OUTPOST_SIEGE.equals(action)) { OutpostSiegeSystem.startOrStatus(player); return; }",
    "if (ACTION_BASTION_SIEGE.equals(action)) { OutpostSiegeSystem.startBastionOrStatus(player); return; }",
    "if (OutpostSiegeSystem.isActive(player))", "OutpostSiegeSystem.sendStatus(player)", "OutpostFortificationService.sendStatus(player)",
    "방어1 / 요새방어2"
], "0.39 production routing/status")
infra = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra, [
    "ProductionService.ACTION_OUTPOST_SIEGE.equals(action)", "ProductionService.ACTION_BASTION_SIEGE.equals(action)",
    "if (OutpostSiegeSystem.isActive(player))", "전초/요새 방어",
    "InfrastructureSiteService.validateForFinalFunding(player, project)",
    "FieldDepotService.countMaterial(player, item)", "FieldDepotService.consume(player, item, amount)"
], "0.39 infrastructure route/gate")
radial = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(radial, [
    "전초 방어전", "요새 방어전", "Action.SIEGE", "Action.BASTION",
    "case SIEGE -> ProductionService.ACTION_OUTPOST_SIEGE;", "case BASTION -> ProductionService.ACTION_BASTION_SIEGE;",
    "반경6~12 실제 벽 사분면12열씩", "별도 방어력 보너스 없음"
], "0.39 radial")
recovery = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java")
need(recovery, ["OutpostSiegeSystem.isActive(player)", "DEATH_RADIUS = 96", "player.teleportTo(", "data.completePending(player)"], "defense recovery exclusion")

# 0.37 physical warehouse clusters remain in field_depots_v1.
depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
need(depot_data, ['"field_depots_v1"', 'optionalFieldOf("warehouse_links", List.of())',
    "MAX_DEPOTS_PER_PLAYER = 3", "MAX_LINKED_BARRELS_PER_DEPOT = 8", "MAX_LINK_RADIUS = 6",
    "record LinkedBarrel", "linkedBarrels", "addLink", "removeLink", "isRegisteredAnchor", "isLinkedByAny"], "0.37 warehouse persistence")
field = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(field, [
    "toggleWarehouseNearest", "activeStorageBarrelCount", "FieldDepotData.MAX_LINK_RADIUS", "FieldDepotData.MAX_LINKED_BARRELS_PER_DEPOT",
    "data.addLink(player, depot, target)", "data.removeLink(player, dimension, target)",
    "if (!level.hasChunkAt(pos)) continue;", "level.mayInteract(player, pos)", "blockEntity instanceof Container",
    "resolved.sort(Comparator.comparingDouble", "MAIN_INVENTORY_FIRST_SLOT = 9", "MAIN_INVENTORY_END_EXCLUSIVE = 36",
    "offloadBulkMaterials", "ItemStack.isSameItemSameComponents(source, existing)", "container.canPlaceItem(slot, source)", "source.copyWithCount(move)"
], "0.37/0.35 physical logistics")
forbid(field, ["setChunkForced", "addRegionTicket", "getChunk("], "physical logistics")
warehouse_start = field.find("public static void toggleWarehouseNearest")
warehouse_end = field.find("public static void sendStatus", warehouse_start)
if warehouse_start < 0 or warehouse_end < 0:
    errors.append("0.37 warehouse action boundary missing")
else:
    forbid(field[warehouse_start:warehouse_end], ["consumeSupplyCharge", "consumeSupplyCharges"], "0.37 warehouse link cost")

# 0.36 commissioning validates before final funding consumption.
site = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java")
need(site, ["ANCHOR_RADIUS = 4", "SITE_RADIUS = 6", "validateForFinalFunding", "Blocks.STONE_BRICKS", "Blocks.LODESTONE", "Blocks.BEACON",
    "level.hasChunkAt(pos)", "level.mayInteract(player, pos)", "blockEntity instanceof Container"], "0.36 commissioning")
forbid(site, ["setChunkForced", "addRegionTicket", "getChunk("], "commissioning")
ordered(infra, ["boolean wasComplete = data.isComplete(project);", "if (wasComplete) {", "validateForFinalFunding", "int consumed = 0;"], "0.36 finalization ordering")

# Encounter entry remains player-carried.
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(apex, ["ECHO_SHARD_COST = 8", "AMETHYST_COST = 32", "GOLD_COST = 32", "count(player, Items.ECHO_SHARD)"], "Apex carry-in")
need(trial, ["ECHO_SHARD_COST = 32", "AMETHYST_COST = 64", "DRAGON_BREATH_COST = 8", "TOTAL_WAVES = 4", "count(player, Items.ECHO_SHARD)"], "Trial carry-in")
forbid(apex, ["FieldDepotService"], "Apex remote payment")
forbid(trial, ["FieldDepotService", '"minecraft:evoker"'], "Trial remote payment/evoker")

# Older physical field contracts and bounded work remain.
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

# Final scale/tick budgets remain intact.
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(bore, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12"], "mining budget")
need(wood, ["GLOBAL_LOG_BUDGET_PER_TICK = 64", "LOCAL_LOG_BUDGET_PER_TICK = 12", "448"], "wood budget/field scale")
need(harvest, ["GLOBAL_HARVEST_BUDGET_PER_TICK = 64", "LOCAL_HARVEST_BUDGET_PER_TICK = 12", "baseSize = 13"], "harvest budget/field scale")
need(construction, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "Math.min(8, budget)", "fieldMastery ? 65", "fieldMastery ? 13"], "construction budget/field scale")

if errors:
    print("SOURCE AUDIT FAIL")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.39 validates a loaded-only physical fortification annulus radius6..12 with >=12 unique x/z columns in each quadrant")
print("- walls/iron bars/nether-brick fence remain real world blocks; no fortified SavedData flag, auto-builder or passive defense multiplier")
print("- bastion defense is optional supply2/four-wave/five-minute combat; normal 0.38 supply1/three-wave/four-minute defense remains")
print("- fortification is revalidated between bastion waves and first spawn succeeds before supply consumption")
print("- siege pressure remains anchor-directed radius6/limit200 with stage/mode difficulty from mob roles rather than blanket HP/attack scaling")
print("- no new packet/client coordinate/force-load; Incident/Operation/Apex/Trial and Field Recovery boundaries remain shared")
print("- 0.37 warehouse clusters,0.36 commissioning,0.35 offload,0.34 inputs,0.33 complications,0.32 sorties and older regressions retained")
