#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []
def read(rel):
    p = ROOT / rel
    if not p.exists(): errors.append(f"missing: {rel}"); return ""
    return p.read_text(encoding="utf-8")
def need(text, needles, label):
    for n in needles:
        if n not in text: errors.append(f"{label} missing: {n}")
def forbid(text, needles, label):
    for n in needles:
        if n in text: errors.append(f"{label} forbidden: {n}")
def ordered(text, needles, label):
    pos = -1
    for n in needles:
        pos = text.find(n, pos + 1)
        if pos < 0: errors.append(f"{label} missing/order: {n}"); return

required = [
 "README.md","PROJECT.md","CHANGELOG.md","gradle.properties",
 "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
 "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
 "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java",
 "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java",
 "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java",
 "src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java",
 "src/main/java/kr/moonseungjun/survivalascension/production/OutpostFortificationService.java",
 "src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java",
 "src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeBreachService.java",
 "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java",
 "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
 "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
 "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java",
 "src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java",
 "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
 "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionMode.java",
 "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
 "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
 "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java",
 "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
 "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
 "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
 "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
 "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
 "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]
for rel in required:
    if not (ROOT/rel).exists(): errors.append(f"missing: {rel}")

props=read("gradle.properties"); need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_version=0.41.0-alpha.1"],"toolchain")
network=read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java"); need(network,['PROTOCOL = "8"'],"protocol")
main=read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main,['VERSION = "0.41.0-alpha.1"',"ConstructionProgression::onBlockPlaced","ConstructionProgression::onServerTick","OutpostSiegeBreachService::onServerTick","OutpostSiegeSystem::onServerTick","OutpostSiegeSystem::onLivingDeath"],"main")

# 0.41 Civil Works and causeway construction.
project=read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java")
need(project,[
 'CIVIL_WORKS(', '"civil_works", "토목 공사소"', 'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 2048)',
 'new Requirement(Items.COBBLESTONE, "조약돌", 1536)', 'new Requirement(Items.GRAVEL, "자갈", 1536)',
 'new Requirement(Items.IRON_INGOT, "철 주괴", 256)', 'new Requirement(Items.COPPER_INGOT, "구리 주괴", 256)'],"0.41 civil project")
mode=read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionMode.java")
need(mode,['CAUSEWAY("causeway", "도로/교량", 60)'],"0.41 construction mode")
construction=read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(construction,[
 "GLOBAL_BLOCK_BUDGET_PER_TICK = 64","MAX_PENDING_BLOCKS_PER_PLAYER = 512","CAUSEWAY_WIDTH = 3",
 "ConstructionMode.CAUSEWAY","InfrastructureProject.CIVIL_WORKS","fieldMastery ? 65 : SkillTuning.constructionLineLength(level)",
 "for (int distance = 0; distance < length; distance++)","for (int lateral = -halfWidth; lateral <= halfWidth; lateral++)",
 "forwardX * distance + sideX * lateral","forwardZ * distance + sideZ * lateral",
 "if (!level.hasChunkAt(target)) return PlaceResult.SKIPPED;","level.mayInteract(player, target)","EventHooks.onBlockPlace",
 "FieldDepotService.hasMaterial(player, item)","FieldDepotService.consumeOne(player, item)","int localBudget = Math.min(8, budget)"],"0.41 causeway runtime")
forbid(construction,["setChunkForced","addRegionTicket","getChunk("],"0.41 construction loading policy")
ordered(construction,["if (!level.hasChunkAt(target))","if (!level.mayInteract(player, target))","EventHooks.onBlockPlace","level.setBlockAndUpdate","FieldDepotService.consumeOne"],"0.41 placement protection/material order")
site=read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java")
need(site,[
 "CIVIL_SITE = new SiteProfile(true","Blocks.STONE_BRICKS","Blocks.SCAFFOLDING","Blocks.IRON_BLOCK","Blocks.STONECUTTER","Blocks.CRAFTING_TABLE",
 "case CIVIL_WORKS -> CIVIL_SITE","ANCHOR_RADIUS = 4","SITE_RADIUS = 6","level.hasChunkAt(pos)","level.mayInteract(player, pos)","blockEntity instanceof Container"],"0.41 civil commissioning")
forbid(site,["setChunkForced","addRegionTicket","getChunk("],"commissioning")
infra=read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra,["project == InfrastructureProject.CIVIL_WORKS","토목 시공","InfrastructureSiteService.validateForFinalFunding(player, project)","FieldDepotService.countMaterial(player, item)","FieldDepotService.consume(player, item, amount)"],"0.41 infra routing")
ordered(infra,["boolean wasComplete = data.isComplete(project);","InfrastructureSiteService.validateForFinalFunding(player, project)","int consumed = 0;"],"final funding site-before-material")
construction_ui=read("src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java")
need(construction_ui,["도로/교량","토목 공사소","ConstructionMode.CAUSEWAY","3폭 × 17/33/49/65"],"0.41 construction UI")
infra_ui=read("src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java")
need(infra_ui,["토목 공사소","InfrastructureProject.CIVIL_WORKS","Items.SCAFFOLDING"],"0.41 infrastructure UI")

# 0.40 physical breachers retained.
breach=read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeBreachService.java")
need(breach,[
 'SIEGE_OWNER_KEY = "survivalascension_outpost_siege_owner"','SIEGE_WAVE_KEY = "survivalascension_outpost_siege_wave"',
 'RAVAGER_ID = "minecraft:ravager"','VINDICATOR_ID = "minecraft:vindicator"',"BASTION_ONLY_WAVE = 4","OWNER_SCAN_RADIUS = 96",
 "RAVAGER_BREAK_COOLDOWN = 30","VINDICATOR_BREAK_COOLDOWN = 60","OutpostSiegeSystem.isActive(owner)",
 "BlockTags.WALLS","Blocks.IRON_BARS","Blocks.NETHER_BRICK_FENCE","OutpostService.isRecoveryOperational","level.hasChunkAt(pos)",
 "EventHooks.canEntityGrief(level, mob)","level.mayInteract(owner, target.pos())","state.canEntityDestroy(level, target.pos(), mob)",
 "EventHooks.onEntityDestroyBlock(mob, target.pos(), state)","level.destroyBlock(target.pos(), true, mob)"],"0.40 breach")
forbid(breach,["setChunkForced","addRegionTicket","getChunk(","Attributes.MAX_HEALTH","Attributes.ATTACK_DAMAGE","AttributeModifier"],"0.40 breach policy")

# 0.39/0.38 defense retained.
fort=read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostFortificationService.java")
need(fort,["INNER_RADIUS = 6","OUTER_RADIUS = 12","MIN_COLUMNS_PER_QUADRANT = 12","BlockTags.WALLS","Blocks.IRON_BARS","Blocks.NETHER_BRICK_FENCE","level.hasChunkAt(pos)"],"0.39 fortification")
forbid(fort,["SavedData","setChunkForced","addRegionTicket","getChunk("],"fortification")
siege=read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java")
need(siege,["START_RADIUS = 4","DEFENSE_RADIUS = 64","BREACH_RADIUS = 6","BREACH_LIMIT = 200","SUPPLY_CHARGE_COST = 1","BASTION_SUPPLY_CHARGE_COST = 2","TOTAL_WAVES = 3","BASTION_TOTAL_WAVES = 4","SIEGE_TIMEOUT_TICKS = 4800","BASTION_TIMEOUT_TICKS = 6000","SiegeMode.OUTPOST","SiegeMode.BASTION","OutpostFortificationService.validateForBastion","EntitySpawnReason.TRIGGERED","siege.breachPressure + breachers * 5","siege.breachPressure - 10"],"0.39/0.38 siege")
forbid(siege,["setChunkForced","addRegionTicket","getChunk(","Attributes.MAX_HEALTH","Attributes.ATTACK_DAMAGE","AttributeModifier"],"siege policy")

# Logistics/expedition/endgame carry-in retained.
field=read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(field,["toggleWarehouseNearest","activeStorageBarrelCount","MAIN_INVENTORY_FIRST_SLOT = 9","MAIN_INVENTORY_END_EXCLUSIVE = 36","if (!level.hasChunkAt(pos)) continue;","level.mayInteract(player, pos)","blockEntity instanceof Container","offloadBulkMaterials"],"physical logistics")
forbid(field,["setChunkForced","addRegionTicket","getChunk("],"physical logistics")
depot=read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java"); need(depot,['"field_depots_v1"','optionalFieldOf("warehouse_links", List.of())',"MAX_DEPOTS_PER_PLAYER = 3","MAX_LINKED_BARRELS_PER_DEPOT = 8"],"warehouse")
apex=read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java"); trial=read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(apex,["ECHO_SHARD_COST = 8","AMETHYST_COST = 32","GOLD_COST = 32","count(player, Items.ECHO_SHARD)"],"Apex carried entry"); forbid(apex,["FieldDepotService"],"Apex remote payment")
need(trial,["ECHO_SHARD_COST = 32","AMETHYST_COST = 64","DRAGON_BREATH_COST = 8","TOTAL_WAVES = 4","count(player, Items.ECHO_SHARD)"],"Trial carried entry"); forbid(trial,["FieldDepotService",'"minecraft:evoker"'],"Trial policy")
ops=read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java"); need(ops,["START_RADIUS = 4","WORK_RADIUS = 48","RETURN_RADIUS = 8","FORWARD_SHIFT_EXTRA = 48","OutpostService.isRecoveryOperational"],"operations"); forbid(ops,["setChunkForced","addRegionTicket","getChunk("],"operations")

# Existing work scale remains.
bore=read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java"); wood=read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java"); harvest=read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
need(bore,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","LOCAL_BLOCK_BUDGET_PER_TICK = 12"],"mining budget"); need(wood,["GLOBAL_LOG_BUDGET_PER_TICK = 64","LOCAL_LOG_BUDGET_PER_TICK = 12","448"],"wood scale"); need(harvest,["GLOBAL_HARVEST_BUDGET_PER_TICK = 64","LOCAL_HARVEST_BUDGET_PER_TICK = 12","baseSize = 13"],"harvest scale")

if errors:
 print("SOURCE AUDIT FAIL")
 for e in errors: print("-",e)
 sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.41 Civil Works consumes a large Stage1 material sink plus a real registered-Barrel commissioning yard")
print("- Construction Lv60+ causeway uses existing protected queue for 3-wide forward physical roads/bridges at 17/33/49/65 length")
print("- causeway targets are loaded-only and retain player-first/physical-depot materials, mayInteract, placement hooks and 64-global/8-local tick budgets")
print("- 0.40 breachers,0.39 fortification,0.38 defense,0.37 warehouse,0.36 commissioning and older progression contracts remain")
print("- no new SavedData ID, packet, client coordinate, second builder engine, getChunk, ticket or force-load")
