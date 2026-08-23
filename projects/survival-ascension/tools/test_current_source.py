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
 "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
 "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java",
 "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
 "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java",
 "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
 "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
 "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
 "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
 "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
 "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
 "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java"]
for rel in required:
    if not (ROOT/rel).exists(): errors.append(f"missing: {rel}")

props=read("gradle.properties"); need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_version=0.40.0-alpha.1"],"toolchain")
network=read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java"); need(network,['PROTOCOL = "8"'],"protocol")
main=read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main,['VERSION = "0.40.0-alpha.1"',"OutpostSiegeBreachService::onServerTick","OutpostSiegeSystem::onServerTick","OutpostSiegeSystem::onLivingDeath","OutpostSiegeSystem::onEntityJoin","FieldRecoveryService::onLivingDeath"],"main")

breach=read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeBreachService.java")
need(breach,[
 'SIEGE_OWNER_KEY = "survivalascension_outpost_siege_owner"','SIEGE_WAVE_KEY = "survivalascension_outpost_siege_wave"',
 'RAVAGER_ID = "minecraft:ravager"','VINDICATOR_ID = "minecraft:vindicator"',"BASTION_ONLY_WAVE = 4","OWNER_SCAN_RADIUS = 96",
 "BREAK_SEARCH_HORIZONTAL = 2","BREAK_SEARCH_DOWN = 1","BREAK_SEARCH_UP = 2","RAVAGER_BREAK_COOLDOWN = 30","VINDICATOR_BREAK_COOLDOWN = 60",
 "OutpostSiegeSystem.isActive(owner)","BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())","BlockTags.WALLS","Blocks.IRON_BARS","Blocks.NETHER_BRICK_FENCE",
 "OutpostFortificationService.INNER_RADIUS","OutpostFortificationService.OUTER_RADIUS","OutpostService.isRecoveryOperational","level.hasChunkAt(pos)",
 "EventHooks.canEntityGrief(level, mob)","level.mayInteract(owner, target.pos())","state.canEntityDestroy(level, target.pos(), mob)",
 "EventHooks.onEntityDestroyBlock(mob, target.pos(), state)","level.destroyBlock(target.pos(), true, mob)",
 "level.getBlockEntity(target.pos()) != null","toBlockX * towardAnchorX + toBlockZ * towardAnchorZ <= 0.0D"],"0.40 breach")
forbid(breach,["setChunkForced","addRegionTicket","getChunk(","Attributes.MAX_HEALTH","Attributes.ATTACK_DAMAGE","AttributeModifier"],"0.40 breach policy")
ordered(breach,["EventHooks.canEntityGrief(level, mob)","findTarget(owner, level, mob)","level.mayInteract(owner, target.pos())","state.canEntityDestroy","EventHooks.onEntityDestroyBlock","level.destroyBlock(target.pos(), true, mob)"],"0.40 protection")

fort=read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostFortificationService.java")
need(fort,["INNER_RADIUS = 6","OUTER_RADIUS = 12","VERTICAL_DOWN = 3","VERTICAL_UP = 4","MIN_COLUMNS_PER_QUADRANT = 12","MIN_TOTAL_COLUMNS = MIN_COLUMNS_PER_QUADRANT * 4","BlockTags.WALLS","Blocks.IRON_BARS","Blocks.NETHER_BRICK_FENCE","level.hasChunkAt(pos)","northEast >= MIN_COLUMNS_PER_QUADRANT","northWest >= MIN_COLUMNS_PER_QUADRANT","southEast >= MIN_COLUMNS_PER_QUADRANT","southWest >= MIN_COLUMNS_PER_QUADRANT"],"0.39 fortification")
forbid(fort,["SavedData","setChunkForced","addRegionTicket","getChunk("],"fortification")

siege=read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java")
need(siege,["START_RADIUS = 4","DEFENSE_RADIUS = 64","BREACH_RADIUS = 6","BREACH_LIMIT = 200","SUPPLY_CHARGE_COST = 1","BASTION_SUPPLY_CHARGE_COST = 2","TOTAL_WAVES = 3","BASTION_TOTAL_WAVES = 4","SIEGE_TIMEOUT_TICKS = 4800","BASTION_TIMEOUT_TICKS = 6000","SiegeMode.OUTPOST","SiegeMode.BASTION","OutpostFortificationService.validateForBastion","production.consumeSupplyCharges(player, mode.supplyCost)","EntitySpawnReason.TRIGGERED","level.hasChunkAt(pos)","siege.breachPressure + breachers * 5","siege.breachPressure - 10","ownerDefendingAnchor","SkillProgressionService.award(owner, SkillType.COMBAT, 650)","SkillProgressionService.award(owner, SkillType.CONSTRUCTION, 250)","SkillProgressionService.award(owner, SkillType.COMBAT, 900)","SkillProgressionService.award(owner, SkillType.CONSTRUCTION, 350)","allyXp = siege.mode == SiegeMode.BASTION ? 70 : 40"],"0.39/0.38 siege")
forbid(siege,["setChunkForced","addRegionTicket","getChunk(","Attributes.MAX_HEALTH","Attributes.ATTACK_DAMAGE","AttributeModifier"],"siege policy")
ordered(siege,["if (!spawnWave(siege))","if (!production.consumeSupplyCharges(player, mode.supplyCost))","ACTIVE.put(player.getUUID(), siege)"],"spawn-before-charge")

field=read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(field,["toggleWarehouseNearest","activeStorageBarrelCount","MAIN_INVENTORY_FIRST_SLOT = 9","MAIN_INVENTORY_END_EXCLUSIVE = 36","if (!level.hasChunkAt(pos)) continue;","level.mayInteract(player, pos)","blockEntity instanceof Container","offloadBulkMaterials"],"physical logistics")
forbid(field,["setChunkForced","addRegionTicket","getChunk("],"physical logistics")
depot=read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java"); need(depot,['"field_depots_v1"','optionalFieldOf("warehouse_links", List.of())',"MAX_DEPOTS_PER_PLAYER = 3","MAX_LINKED_BARRELS_PER_DEPOT = 8"],"warehouse")
site=read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java"); need(site,["ANCHOR_RADIUS = 4","SITE_RADIUS = 6","validateForFinalFunding","level.hasChunkAt(pos)","level.mayInteract(player, pos)","blockEntity instanceof Container"],"commissioning"); forbid(site,["setChunkForced","addRegionTicket","getChunk("],"commissioning")

apex=read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java"); trial=read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(apex,["ECHO_SHARD_COST = 8","AMETHYST_COST = 32","GOLD_COST = 32","count(player, Items.ECHO_SHARD)"],"Apex carried entry"); forbid(apex,["FieldDepotService"],"Apex remote payment")
need(trial,["ECHO_SHARD_COST = 32","AMETHYST_COST = 64","DRAGON_BREATH_COST = 8","TOTAL_WAVES = 4","count(player, Items.ECHO_SHARD)"],"Trial carried entry"); forbid(trial,["FieldDepotService",'"minecraft:evoker"'],"Trial policy")

outpost=read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java"); need(outpost,["STRUCTURE_RADIUS = 5","ACTIVE_OWNER_RADIUS = 64","EXTENDED_SUPPLY_RADIUS = 64","SAFE_RADIUS = 24",'if (!"NATURAL".equals(event.getSpawnType().name())) return;',"isRecoveryOperational"],"outpost")
ops=read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java"); need(ops,["START_RADIUS = 4","WORK_RADIUS = 48","RETURN_RADIUS = 8","FORWARD_SHIFT_EXTRA = 48","OutpostService.isRecoveryOperational"],"operations"); forbid(ops,["setChunkForced","addRegionTicket","getChunk("],"operations")
comp=read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java"); need(comp,['DEEP_FRONT("전선 고착"','FORWARD_SHIFT("전선 재전개"','HOT_EXTRACTION("긴급 철수"'],"complications")
operation=read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java")
for r in ["WOODLAND","ARID","WETLAND","HIGHLANDS","OCEAN","DEEP","FROZEN","NETHER","END"]: need(operation,[f"{r}(ExpeditionRegion.{r}"],"nine regions")

bore=read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java"); wood=read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java"); harvest=read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"); construction=read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(bore,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","LOCAL_BLOCK_BUDGET_PER_TICK = 12"],"mining budget"); need(wood,["GLOBAL_LOG_BUDGET_PER_TICK = 64","LOCAL_LOG_BUDGET_PER_TICK = 12","448"],"wood scale"); need(harvest,["GLOBAL_HARVEST_BUDGET_PER_TICK = 64","LOCAL_HARVEST_BUDGET_PER_TICK = 12","baseSize = 13"],"harvest scale"); need(construction,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","65","13"],"construction scale")

if errors:
 print("SOURCE AUDIT FAIL")
 for e in errors: print("-",e)
 sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.40 Bastion final-wave Ravager/Vindicator breachers are bounded to real fortification blocks")
print("- mobGriefing + owner protection + block destroyability + NeoForge destroy hook precede every break")
print("- normal 3-wave defense stays non-destructive; broken wall material drops for physical repair")
print("- 0.39 fortification and 0.38/0.37/0.36/0.35/0.34/0.33/0.32 contracts remain")
print("- no new SavedData/packet/client coordinate/getChunk/ticket/force-load")
