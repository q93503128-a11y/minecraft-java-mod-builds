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
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
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
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.37.0-alpha.1"], "toolchain/version")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.37.0-alpha.1"', "FieldRecoveryService::onLivingDeath", "ExpeditionOperationSystem::onLivingDeath", "OutpostService::onFinalizeSpawn", "ApexHuntSystem::onServerTick", "AscensionTrialSystem::onServerTick"], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

# 0.37 Physical Warehouse Clusters: optional migration inside field_depots_v1, bounded real Barrel capacity.
depot_data = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
need(depot_data, [
    '"field_depots_v1"', 'optionalFieldOf("warehouse_links", List.of())',
    "MAX_DEPOTS_PER_PLAYER = 3", "MAX_LINKED_BARRELS_PER_DEPOT = 8", "MAX_LINK_RADIUS = 6",
    "record LinkedBarrel", "anchor_x", "anchor_y", "anchor_z", "linkedBarrels", "linkedCount", "totalLinkedCount",
    "isRegisteredAnchor", "isLinkedByOwner", "isLinkedByAny", "isPositionClaimed", "addLink", "removeLink",
    "link.anchorPos().distSqr(link.pos()) > MAX_LINK_RADIUS * MAX_LINK_RADIUS", "claimed.add(link.key())",
    "links.removeIf(link -> link.anchorKey().equals(anchorKey))"
], "0.37 warehouse persistence")
ordered(depot_data, ["players.put(entry.uuid(), sanitized);", "Set<String> claimed", "for (LinkedBarrel link : links)"], "0.37 old-save depots before optional links")

field = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(field, [
    "toggleWarehouseNearest", "activeStorageBarrelCount", "nearestOwnedDepotForTarget",
    "FieldDepotData.MAX_LINK_RADIUS", "FieldDepotData.MAX_LINKED_BARRELS_PER_DEPOT",
    "data.isRegisteredAnchor(dimension, target)", "data.isLinkedByOwner(player, dimension, target)", "data.isLinkedByAny(dimension, target)",
    "data.addLink(player, depot, target)", "data.removeLink(player, dimension, target)",
    "for (FieldDepotData.LinkedBarrel link", "if (!level.hasChunkAt(pos)) continue;", "data.removeLink(player, dimension, pos)",
    "resolved.sort(Comparator.comparingDouble", "level.mayInteract(player, pos)", "blockEntity instanceof Container",
    "MAIN_INVENTORY_FIRST_SLOT = 9", "MAIN_INVENTORY_END_EXCLUSIVE = 36", "offloadBulkMaterials",
    "ItemStack.isSameItemSameComponents(source, existing)", "container.canPlaceItem(slot, source)", "source.copyWithCount(move)"
], "0.37 warehouse resolver + 0.35 offload")
if any(token in field for token in ["setChunkForced", "addRegionTicket", "getChunk("]):
    errors.append("0.37 physical warehouse/logistics must not force-load chunks")
warehouse_start = field.find("public static void toggleWarehouseNearest")
status_start = field.find("public static void sendStatus", warehouse_start)
if warehouse_start < 0 or status_start < 0:
    errors.append("0.37 warehouse action boundary missing")
else:
    warehouse_body = field[warehouse_start:status_start]
    if "consumeSupplyCharge" in warehouse_body or "consumeSupplyCharges" in warehouse_body:
        errors.append("0.37 linked warehouse barrels must not consume a supply charge")

production = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production, [
    'ACTION_WAREHOUSE_TOGGLE = "toggle_warehouse_barrel"',
    "if (ACTION_WAREHOUSE_TOGGLE.equals(action)) { FieldDepotService.toggleWarehouseNearest(player); return; }",
    "FieldDepotService.activeStorageBarrelCount(player)", "거점 앵커/창고 배럴/전초 재고",
    "FieldDepotService.countMatching(player, input::matches)", "FieldDepotService.consumeMatching(player, input::matches, amount)"
], "0.37 production route")
infra = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra, ["ProductionService.ACTION_WAREHOUSE_TOGGLE.equals(action)", "InfrastructureSiteService.validateForFinalFunding(player, project)", "FieldDepotService.countMaterial(player, item)", "FieldDepotService.consume(player, item, amount)"], "infrastructure routes")
radial = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(radial, ["창고 배럴 연결", "Action.WAREHOUSE", "case WAREHOUSE -> ProductionService.ACTION_WAREHOUSE_TOGGLE;", "거점당 최대8", "실제 창고군"], "0.37 radial")

# 0.36 commissioning remains validation-before-consumption and loaded-only.
site = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java")
need(site, ["ANCHOR_RADIUS = 4", "SITE_RADIUS = 6", "validateForFinalFunding", "Blocks.IRON_BLOCK", "Blocks.LODESTONE", "Blocks.BEACON", "FieldDepotData.get(player).depots(player)", "level.hasChunkAt(pos)", "level.mayInteract(player, pos)", "blockEntity instanceof Container"], "0.36 commissioning")
if any(token in site for token in ["setChunkForced", "addRegionTicket", "getChunk("]): errors.append("commissioning must not force-load chunks")
ordered(infra, ["boolean wasComplete = data.isComplete(project);", "if (wasComplete) {", "validateForFinalFunding", "int consumed = 0;"], "0.36 old-complete + validate-before-consume")

# 0.35/0.34 offload + integrated input contract.
offload_start = field.find("public static int offloadBulkMaterials")
insert_start = field.find("private static int insertIntoContainers")
if offload_start < 0 or insert_start < 0:
    errors.append("offload method boundaries missing")
else:
    body = field[offload_start:insert_start]
    need(body, ["for (int slot = MAIN_INVENTORY_FIRST_SLOT; slot < end; slot++)", "moved += insertIntoContainers(source, containers);"], "0.35 slot boundary")
    if "for (int slot = 0;" in body: errors.append("0.35 offload must not scan hotbar slot0")

# Encounter admission remains player-carried.
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(apex, ["ECHO_SHARD_COST = 8", "AMETHYST_COST = 32", "GOLD_COST = 32", "count(player, Items.ECHO_SHARD)"], "Apex carried entry")
need(trial, ["ECHO_SHARD_COST = 32", "AMETHYST_COST = 64", "DRAGON_BREATH_COST = 8", "TOTAL_WAVES = 4", "count(player, Items.ECHO_SHARD)"], "Trial carried entry")
if "FieldDepotService" in apex or "FieldDepotService" in trial: errors.append("Apex/Trial entry must remain inventory-only")
if '"minecraft:evoker"' in trial: errors.append("Trial must not directly spawn evokers")

# Physical outpost / recovery / sortie regressions.
outpost = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java")
need(outpost, ["STRUCTURE_RADIUS = 5", "ACTIVE_OWNER_RADIUS = 64", "EXTENDED_SUPPLY_RADIUS = 64", "SAFE_RADIUS = 24", 'if (!"NATURAL".equals(event.getSpawnType().name())) return;', "isRecoveryOperational"], "outpost")
recovery = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java")
need(recovery, ["DEATH_RADIUS = 96", "SUPPLY_CHARGE_COST = 1", "player.teleportTo(", "data.completePending(player)"], "recovery")
for text, label in [(outpost, "outpost"), (recovery, "recovery")]:
    if any(token in text for token in ["setChunkForced", "addRegionTicket", "getChunk("]): errors.append(f"{label} must not force-load chunks")
complication = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java")
need(complication, ['DEEP_FRONT("전선 고착"', 'FORWARD_SHIFT("전선 재전개"', 'HOT_EXTRACTION("긴급 철수"', "case 0 -> 4800;", "case 1 -> 3600;", "default -> 3000;"], "0.33 complications")
opsys = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java")
need(opsys, ["START_RADIUS = 4", "WORK_RADIUS = 48", "RETURN_RADIUS = 8", "FORWARD_SHIFT_EXTRA = 48", "OutpostService.isRecoveryOperational"], "operation lifecycle")
if any(token in opsys for token in ["setChunkForced", "addRegionTicket", "getChunk("]): errors.append("operations must not force-load chunks")
operation = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java")
for region in ["WOODLAND", "ARID", "WETLAND", "HIGHLANDS", "OCEAN", "DEEP", "FROZEN", "NETHER", "END"]:
    need(operation, [f"{region}(ExpeditionRegion.{region}"], "nine operation catalog")

# Final scale / tick budgets stay intact.
tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning, ["if (level >= 100) return 11;", "if (level >= 100) return 192;", "if (level >= 100) return 384;", "if (level >= 100) return 49;"], "Mastery VI")
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
mobility = read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
need(bore, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12", "fieldMastery ? 12"], "mining budget")
need(wood, ["GLOBAL_LOG_BUDGET_PER_TICK = 64", "FIELD_MASTERY_LOG_LIMIT = 448"], "wood budget")
need(harvest, ["GLOBAL_HARVEST_BUDGET_PER_TICK = 64", "baseSize = 13"], "harvest budget")
need(construction, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "FieldDepotService.consumeOne(player, item)", "EventHooks.onBlockPlace"], "construction safety")
need(combat, ["fieldMastery ? 7.5D", "fieldMastery ? 20"], "combat Field Mastery")
need(mobility, ["return 4;", "AIR_DASH_COUNT"], "mobility Field Mastery")

# Canon/UI lock.
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(guide, ["물리 창고군", "반경6", "최대8", "슬롯9~35", "핫바0~8", "전선 고착", "전선 재전개", "긴급 철수", "Stage0 4:00 / Stage1 3:00 / Stage2 2:30", "청크 강제로드"], "0.37 guide")
readme = read("README.md")
project = read("PROJECT.md")
changelog = read("CHANGELOG.md")
need(readme, ["0.37.0-alpha.1", "Physical Warehouse Clusters", "warehouse_links", "8", "radius 6", "0.36.0-alpha.1"], "README canon")
need(project, ["0.37 Physical Warehouse Clusters", "MAX_LINKED_BARRELS_PER_DEPOT = 8", "MAX_LINK_RADIUS = 6", "optional `warehouse_links`", "0.36 Physical Commissioning Sites"], "PROJECT canon")
need(changelog, ["0.37.0-alpha.1", "ACTION_WAREHOUSE_TOGGLE", "warehouse_links", "nearest-first", "no supply charge", "0.36.0-alpha.1"], "CHANGELOG canon")

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
print("- 0.37 extends each physical depot anchor with up to8 explicitly linked real Barrels inside radius6")
print("- field_depots_v1 keeps backward-compatible optional warehouse_links; global physical positions cannot be double-claimed")
print("- linked storage is loaded-only, mayInteract-gated, real-Container-backed and sorted nearest-first with anchor stock")
print("- unloaded linked Barrels are preserved; loaded missing/non-Barrel links prune without deleting other storage")
print("- linking costs no supply charge and adds no virtual capacity, background scan, new packet or chunk force-load")
print("- 0.36 commissioning,0.35 offload,0.34 integrated inputs,0.33 complications,0.32 sorties and older regressions retained")
