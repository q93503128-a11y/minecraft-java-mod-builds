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
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
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
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.36.0-alpha.1"], "toolchain/version")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.36.0-alpha.1"', "FieldRecoveryService::onLivingDeath", "ExpeditionOperationSystem::onLivingDeath", "OutpostService::onFinalizeSpawn", "ApexHuntSystem::onServerTick", "AscensionTrialSystem::onServerTick"], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

# 0.36 physical commissioning: real loaded site before a finalizable funding call consumes anything.
site = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java")
need(site, [
    "ANCHOR_RADIUS = 4", "SITE_RADIUS = 6", "validateForFinalFunding", "sendStatus",
    "Blocks.STONE_BRICKS", "Blocks.IRON_BLOCK", "Blocks.BLAST_FURNACE", "Blocks.STONECUTTER", "Blocks.HOPPER",
    "Blocks.GOLD_BLOCK", "Blocks.LODESTONE", "Blocks.CARTOGRAPHY_TABLE", "Blocks.TARGET",
    "Blocks.OBSIDIAN", "Blocks.CRYING_OBSIDIAN", "Blocks.BEACON", "Blocks.ENCHANTING_TABLE", "Blocks.ENDER_CHEST",
    "FieldDepotData.get(player).depots(player)", "level.hasChunkAt(pos)", "level.mayInteract(player, pos)",
    "blockEntity instanceof Container", "case INDUSTRIAL_WORKS", "case APEX_TRACKING_POST", "case ASCENSION_NEXUS"
], "0.36 physical commissioning")
if any(token in site for token in ["setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("0.36 commissioning must not force-load chunks")
infra = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra, [
    "InfrastructureSiteService.requiresSite(project)", "canFullyFundNow(player, data, project)",
    "InfrastructureSiteService.validateForFinalFunding(player, project)", "InfrastructureSiteService.sendStatus(player, project)",
    "siteValidatedForCompletion", "FieldDepotService.countMaterial(player, item)", "FieldDepotService.consume(player, item, amount)"
], "0.36 finalization gate")
ordered(infra, ["boolean wasComplete = data.isComplete(project);", "if (wasComplete) {", "boolean siteValidatedForCompletion", "validateForFinalFunding"], "0.36 old-complete compatibility")
ordered(infra, ["validateForFinalFunding", "int consumed = 0;"], "0.36 validate-before-consume")
if "stack.shrink(take)" in infra:
    errors.append("InfrastructureService must keep shared physical logistics rather than inventory-only shrink")

# 0.35 explicit offload remains bounded and lossless.
depot = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(depot, [
    "MAIN_INVENTORY_FIRST_SLOT = 9", "MAIN_INVENTORY_END_EXCLUSIVE = 36", "countOffloadableMainInventory", "offloadBulkMaterials", "isBulkMaterial",
    "ItemTags.LOGS", "ItemStack.isSameItemSameComponents(source, existing)", "container.canPlaceItem(slot, source)",
    "container.setItem(slot, source.copyWithCount(move))", "source.shrink(move)", "player.containerMenu.broadcastChanges()",
    "REGISTER_RADIUS = 4", "SUPPLY_RADIUS = 32", "OutpostService.EXTENDED_SUPPLY_RADIUS", "level.hasChunkAt(pos)", "level.mayInteract(player, pos)"
], "0.34/0.35 logistics")
if any(token in depot for token in ["setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("physical logistics must not force-load chunks")
offload_start = depot.find("public static int offloadBulkMaterials")
insert_start = depot.find("private static int insertIntoContainers")
if offload_start < 0 or insert_start < 0:
    errors.append("0.35 offload method boundaries missing")
else:
    body = depot[offload_start:insert_start]
    need(body, ["for (int slot = MAIN_INVENTORY_FIRST_SLOT; slot < end; slot++)", "moved += insertIntoContainers(source, containers);"], "0.35 main-inventory boundary")
    if "for (int slot = 0;" in body: errors.append("0.35 offload must not scan hotbar slot0")
production = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production, ['ACTION_BULK_OFFLOAD = "bulk_offload"', "FieldDepotService.offloadBulkMaterials(player)", "FieldDepotService.countMatching(player, input::matches)", "FieldDepotService.consumeMatching(player, input::matches, amount)", "new ItemStack(Items.GOLD_INGOT, 32)"], "production regressions")

# Field combat admission stays physically carried.
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(apex, ["ECHO_SHARD_COST = 8", "AMETHYST_COST = 32", "GOLD_COST = 32", "count(player, Items.ECHO_SHARD)"], "Apex carry-in")
need(trial, ["ECHO_SHARD_COST = 32", "AMETHYST_COST = 64", "DRAGON_BREATH_COST = 8", "count(player, Items.ECHO_SHARD)", "TOTAL_WAVES = 4"], "Trial carry-in")
if "FieldDepotService" in apex or "FieldDepotService" in trial: errors.append("Apex/Trial entry must not become remote Barrel payment")
if '"minecraft:evoker"' in trial: errors.append("Ascension Trial must not directly spawn evokers")

# Physical depot/outpost/recovery contracts.
outpost = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java")
need(outpost, ["STRUCTURE_RADIUS = 5", "ACTIVE_OWNER_RADIUS = 64", "EXTENDED_SUPPLY_RADIUS = 64", "SAFE_RADIUS = 24", "state.getBlock() instanceof BedBlock", "Blocks.CAMPFIRE", "Blocks.CRAFTING_TABLE", "Blocks.FURNACE", 'if (!"NATURAL".equals(event.getSpawnType().name())) return;', "isRecoveryOperational"], "0.30 outpost")
if any(token in outpost for token in ["setChunkForced", "addRegionTicket", "getChunk("]): errors.append("outposts must not force-load chunks")
depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
need(depot_data, ['"field_depots_v1"', "MAX_DEPOTS_PER_PLAYER = 3", "CLAIMED_BY_OTHER"], "0.29 depot")
production_data = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionData.java")
need(production_data, ['"production_v1"', "MAX_BUFFER = 3", "MAX_SUPPLY_CHARGES = 3", "consumeSupplyCharges"], "0.28 production")
recovery = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java")
need(recovery, ["DEATH_RADIUS = 96", "SUPPLY_CHARGE_COST = 1", "data.queuePending(player)", "player.teleportTo(", "data.completePending(player)"], "0.31 recovery")
if any(token in recovery for token in ["setChunkForced", "addRegionTicket", "getChunk("]): errors.append("field recovery must not force-load chunks")

# 0.32/0.33 sortie catalog, complication and physical-return gates.
complication = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java")
need(complication, ['NONE("기본 작전"', 'DEEP_FRONT("전선 고착"', 'FORWARD_SHIFT("전선 재전개"', 'HOT_EXTRACTION("긴급 철수"', "case 0 -> 4800;", "case 1 -> 3600;", "default -> 3000;"], "0.33 complications")
opdata = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationData.java")
need(opdata, ['"expedition_operations_v1"', 'optionalFieldOf("complication", "NONE")', 'optionalFieldOf("complication_state", 0)', 'optionalFieldOf("extraction_deadline", 0L)', "armExtraction", "state.clearActive()"], "operation persistence")
operation = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java")
for region in ["WOODLAND", "ARID", "WETLAND", "HIGHLANDS", "OCEAN", "DEEP", "FROZEN", "NETHER", "END"]:
    need(operation, [f"{region}(ExpeditionRegion.{region}"], "nine operation catalog")
opsys = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java")
need(opsys, ["START_RADIUS = 4", "WORK_RADIUS = 48", "RETURN_RADIUS = 8", "FORWARD_SHIFT_EXTRA = 48", "OutpostService.nearestActiveOutpost", "ExpeditionComplication.DEEP_FRONT", "ExpeditionComplication.FORWARD_SHIFT", "ExpeditionComplication.HOT_EXTRACTION", "OutpostService.isRecoveryOperational"], "operation lifecycle")
if any(token in opsys for token in ["setChunkForced", "addRegionTicket", "getChunk("]): errors.append("operations must not force-load chunks")

# Existing region, mastery, behavior and tick-budget safety.
directive = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java")
for marker in ["WOODLAND_STANDARD", "ARID_STANDARD", "WETLAND_STANDARD", "HIGHLANDS_STANDARD", "OCEAN_STANDARD", "DEEP_STANDARD", "FROZEN_STANDARD", "NETHER_STANDARD", "END_STANDARD"]:
    need(directive, [marker], "expedition directives")
incident = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java")
need(incident, ["CHECK_INTERVAL_TICKS = 600", "START_CHANCE = 0.10D", "EntitySpawnReason.TRIGGERED"], "incidents")
apex_catalog = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java")
need(apex_catalog, ["CHARGE", "REINFORCE", "PLAGUE", "SKIRMISH", "PULL", "LEAP", "FROST", "WITHER", "VOID"], "Apex patterns")
for rel, needles in {
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java": ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12", "player.gameMode.destroyBlock(target)"],
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java": ["GLOBAL_LOG_BUDGET_PER_TICK = 64", "FIELD_MASTERY_LOG_LIMIT = 448"],
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java": ["GLOBAL_HARVEST_BUDGET_PER_TICK = 64", "baseSize = 13"],
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java": ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "MAX_PENDING_BLOCKS_PER_PLAYER = 512", "FieldDepotService.consumeOne(player, item)", "EventHooks.onBlockPlace"],
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java": ["REPLANT_BUDGET_PER_TICK = 64", "FieldDepotService.consumeOne(player, kind.seed())", "EventHooks.onBlockPlace"],
}.items():
    need(read(rel), needles, f"safety {rel}")
combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
need(combat, ["fieldMastery ? 7.5D", "fieldMastery ? 20"], "combat Field Mastery")
mobility = read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
need(mobility, ["return 4;", "AIR_DASH_COUNT"], "mobility Field Mastery")
mutation = read("src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java")
need(mutation, ["MUTATION_CHANCE = 0.18D", "Mutation.WITHERED", "Mutation.PHASE", "Mutation.PLAGUE"], "mutations")
warband = read("src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java")
need(warband, ["ROUT_TICKS = 160", "3 + worldStage", "6 + worldStage"], "warbands")
affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
need(affix, ['AWAKENED = "awakened"', "currentAffixes(stack).size() == 3", "awakened ? 4 : rarity"], "awakened mythic")

# UI and canon lock.
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(guide, ["물리 준공 현장", "산업 가공소 준공", "정점 추적소 준공", "승천 중추 준공", "마지막 프로젝트 재료를 하나도 소비하지 않습니다", "슬롯9~35", "Stage0 4:00 / Stage1 3:00 / Stage2 2:30"], "guide")
infra_radial = read("src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java")
need(infra_radial, ["마지막 실제 배럴 준공 현장", "등록 배럴 기반 추적소 준공 현장", "등록 배럴 기반 중추 준공 현장"], "infrastructure radial")
radial = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(radial, ["현장 일괄 적재", "마지막 투입은 실제 배럴 준공 현장 필요", "채집 → 적재 → 물리 준공 → 생산/전초 → 원정"], "production radial")
readme, project, changelog, third = read("README.md"), read("PROJECT.md"), read("CHANGELOG.md"), read("THIRD_PARTY_NOTICES.md")
need(readme, ["0.36.0-alpha.1", "Physical Commissioning Sites", "Industrial Works", "Apex Tracking Post", "Ascension Nexus", "existing completed", "No new SavedData", "0.35.0-alpha.1"], "README canon")
need(project, ["0.36 Physical Commissioning Sites", "ANCHOR_RADIUS = 4", "SITE_RADIUS = 6", "validateForFinalFunding", "existing completed", "protocol remains `8`", "0.35 High-volume Field Offload"], "PROJECT canon")
need(changelog, ["0.36.0-alpha.1", "InfrastructureSiteService", "finalizable funding", "existing completed", "0.35.0-alpha.1"], "CHANGELOG canon")
need(third, ["Deep Rock Galactic — product reference only for 0.33", "Warframe — product reference only for 0.33", "Heracles — design reference only for 0.32"], "third-party policy")

for forbidden in ["harmonised.pmmo", "alrex.parcool", "mekanism.common", "com.simibubi.create", "com.minecolonies", "net.blay09.mods.waystones", "de.maxhenkel.corpse", "terrarium.heracles", "io.ejekta.bountiful"]:
    for path in (ROOT / "src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8", errors="ignore").lower():
            errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

for rel in ["src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java", "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java", "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]:
    if re.search(r"setBlock\s*\([^\n]*AIR", read(rel)): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.36 gates only finalizable Industrial/Apex/Nexus funding behind explicit real loaded commissioning sites")
print("- Industrial uses a nearby real Barrel; Apex/Nexus require an owned registered Barrel; site radius6 and mayInteract are authoritative")
print("- site validation happens before the final funding call consumes any project material; existing completed projects remain compatible")
print("- no new SavedData/packet/background tick/chunk force-load; commissioning is explicit and bounded")
print("- 0.35 offload,0.34 integrated logistics,0.33 complications,0.32 sorties,0.31 recovery,0.30 outposts and older regressions retained")
