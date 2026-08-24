#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing: {rel}")
        return ""
    return p.read_text(encoding="utf-8")

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
        pos = text.find(needle, pos + 1)
        if pos < 0:
            errors.append(f"{label} missing/order: {needle}")
            return

required = [
    "README.md", "PROJECT.md", "CHANGELOG.md", "gradle.properties", "THIRD_PARTY_NOTICES.md",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostFortificationService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeBreachService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldRecoveryService.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionMode.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperation.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionComplication.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
]
for rel in required:
    if not (ROOT / rel).exists():
        errors.append(f"missing: {rel}")

props = read("gradle.properties")
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.42.1-alpha.1"], "toolchain")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "protocol")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.42.1-alpha.1"', "ConstructionProgression::onBlockPlaced", "OutpostSiegeBreachService::onServerTick", "OutpostSiegeSystem::onServerTick"], "main")

# 0.42.1 guide/pacing/mining/logistics-priority pass.
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(guide, [
    "CONTENT_TOP = 64", "CONTENT_BOTTOM_MARGIN = 44", "SCROLL_STEP = 30",
    "mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)",
    "graphics.enableScissor", "graphics.disableScissor", "maxScroll", "scrollOffset",
    'h("월드 승천")', "위더를 처음 격파하면 전설 단계(1)", "엔더 드래곤을 처음 격파하면 종말 단계(2)",
    "물류 통과 창고군", "일반 바닐라 작업대 조합은 조합칸/인벤토리 규칙을 그대로 따릅니다."
], "0.42.1 scrollable current-state guide")
forbid(guide, ["0.42부터", "0.41부터", "0.40부터"], "guide patch-note policy")

tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning, [
    "long base = 40L + 8L * level + Math.round(1.5D * level * level);",
    "if (level < 10) factor = 0.20D + 0.03D * level;",
    "else if (level < 30) factor = 0.50D + 0.015D * (level - 10);",
    "else if (level < 60) factor = 0.80D + 0.0065D * (level - 30);",
    "else factor = 1.0D;",
    "return Math.max(8L, Math.round(base * factor));"
], "0.42.1 early mastery curve")

mining = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")
need(mining, [
    "if (state.is(Blocks.COPPER_ORE)) return 7;",
    "if (state.is(Blocks.DEEPSLATE_COPPER_ORE)) return 8;",
    "if (state.is(Blocks.IRON_ORE)) return 9;",
    "if (state.is(Blocks.DIAMOND_ORE)) return 18;",
    "if (state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) return 20;",
    "if (state.is(Blocks.ANCIENT_DEBRIS)) return 24;",
    "if (state.is(Blocks.OBSIDIAN)) return 16;",
    "if (state.is(Blocks.CRYING_OBSIDIAN)) return 18;",
    "return Math.max(1, Math.min(8, (int) Math.ceil(hardness)));"
], "0.42.1 mining XP tiers")
forbid(mining, ["if (state.is(VALUABLE_ORES)) return 20;"], "flat valuable-ore XP")

field = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(field, [
    "재료 소비: 모드 제작·건축·인프라 비용은 가까운 사용 가능 물류 통부터",
    "List<Container> containers = usableContainers(player);",
    "player.containerMenu.broadcastChanges();"
], "0.42.1 logistics storage-first")
ordered(field, [
    "public static boolean consumeMatching",
    "List<Container> containers = usableContainers(player);",
    "for (Container container : containers)",
    "player.getInventory().getContainerSize() && remaining > 0"
], "0.42.1 storage before carried inventory")
forbid(field, ["setChunkForced", "addRegionTicket", "getChunk("], "physical logistics")

prod_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(prod_ui, ["창고 통 연결", "4블록 내 기본 통 앵커", "물리 화물 수레", "Items.CHEST_MINECART", "Action.FREIGHT", "ProductionService.ACTION_FREIGHT"], "0.42.1 logistics wording/freight UI")

# 0.42 physical freight relay retained.
freight = read("src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java")
need(freight, [
    'OWNER_KEY = "survivalascension_freight_owner"',
    'ORIGIN_DIMENSION_KEY = "survivalascension_freight_origin_dimension"',
    'ORIGIN_X_KEY = "survivalascension_freight_origin_x"',
    'ORIGIN_Y_KEY = "survivalascension_freight_origin_y"',
    'ORIGIN_Z_KEY = "survivalascension_freight_origin_z"',
    "INTERACTION_RADIUS = 4",
    "InfrastructureProject.INDUSTRIAL_WORKS",
    "InfrastructureProject.CIVIL_WORKS",
    "MinecartChest",
    "OutpostService.nearestActiveOutpost(player, INTERACTION_RADIUS)",
    "BlockTags.RAILS",
    "FieldDepotService.isBulkMaterial",
    "FieldDepotData.get(player).linkedBarrels(player, depot)",
    "level.hasChunkAt(pos)",
    "level.mayInteract(player, pos)",
    "Blocks.BARREL",
    "blockEntity instanceof Container",
    "ItemStack.isSameItemSameComponents(source, existing)",
    "target.canPlaceItem(slot, source)",
    "target.getMaxStackSize(source)",
    "source.copyWithCount(move)",
    "destination.pos().equals(origin)",
    "clearManifest(cart)",
], "0.42 freight")
forbid(freight, [
    "SavedData", "setChunkForced", "addRegionTicket", "getChunk(", "addFreshEntity",
    "teleportTo", "randomTeleport", "consumeSupplyCharge", "giveOrDrop", "award(",
], "0.42 freight physical-only policy")
ordered(freight, [
    "if (!isEmpty(cart))", "List<Container> source = storageForDepot", "int moved = moveBulkInto(source, cart)",
    "data.putString(OWNER_KEY", "data.putString(ORIGIN_DIMENSION_KEY"
], "0.42 load order")
ordered(freight, [
    "destination.pos().equals(origin)", "List<Container> targets = storageForDepot", "int moved = moveBulkOut(cart, targets)", "if (remaining <= 0) clearManifest(cart)"
], "0.42 unload order")
production = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production, ['ACTION_FREIGHT = "physical_freight"', "FreightService.transferNearest(player)", "FreightService.sendStatus(player)"], "0.42 production routing")
infra = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra, ["ProductionService.ACTION_FREIGHT", "project == InfrastructureProject.CIVIL_WORKS"], "0.42 infrastructure routing")

# 0.41 Civil Works and causeway retained.
project = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java")
need(project, [
    'CIVIL_WORKS(', '"civil_works", "토목 공사소"',
    'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 2048)',
    'new Requirement(Items.COBBLESTONE, "조약돌", 1536)',
    'new Requirement(Items.GRAVEL, "자갈", 1536)',
], "0.41 civil project")
mode = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionMode.java")
need(mode, ['CAUSEWAY("causeway", "도로/교량", 60)'], "0.41 construction mode")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(construction, [
    "GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "MAX_PENDING_BLOCKS_PER_PLAYER = 512", "CAUSEWAY_WIDTH = 3",
    "ConstructionMode.CAUSEWAY", "InfrastructureProject.CIVIL_WORKS",
    "fieldMastery ? 65 : SkillTuning.constructionLineLength(level)",
    "if (!level.hasChunkAt(target)) return PlaceResult.SKIPPED;", "level.mayInteract(player, target)",
    "EventHooks.onBlockPlace", "FieldDepotService.consumeOne(player, item)", "int localBudget = Math.min(8, budget)"
], "0.41 causeway runtime")
forbid(construction, ["setChunkForced", "addRegionTicket", "getChunk("], "construction loading policy")
site = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java")
need(site, ["CIVIL_SITE = new SiteProfile(true", "Blocks.SCAFFOLDING", "case CIVIL_WORKS -> CIVIL_SITE", "level.hasChunkAt(pos)", "level.mayInteract(player, pos)"], "0.41 commissioning")

# 0.40/0.39/0.38 combat-infrastructure boundaries retained.
breach = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeBreachService.java")
need(breach, ["BASTION_ONLY_WAVE = 4", "RAVAGER_BREAK_COOLDOWN = 30", "VINDICATOR_BREAK_COOLDOWN = 60", "EventHooks.canEntityGrief(level, mob)", "EventHooks.onEntityDestroyBlock", "level.destroyBlock(target.pos(), true, mob)"], "0.40 breach")
forbid(breach, ["setChunkForced", "addRegionTicket", "getChunk(", "Attributes.MAX_HEALTH", "Attributes.ATTACK_DAMAGE"], "breach policy")
fort = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostFortificationService.java")
need(fort, ["INNER_RADIUS = 6", "OUTER_RADIUS = 12", "MIN_COLUMNS_PER_QUADRANT = 12", "BlockTags.WALLS", "Blocks.IRON_BARS", "Blocks.NETHER_BRICK_FENCE"], "0.39 fortification")
siege = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java")
need(siege, ["DEFENSE_RADIUS = 64", "BREACH_RADIUS = 6", "BREACH_LIMIT = 200", "SUPPLY_CHARGE_COST = 1", "BASTION_SUPPLY_CHARGE_COST = 2", "TOTAL_WAVES = 3", "BASTION_TOTAL_WAVES = 4", "SIEGE_TIMEOUT_TICKS = 4800", "BASTION_TIMEOUT_TICKS = 6000"], "0.39/0.38 siege")
forbid(siege, ["setChunkForced", "addRegionTicket", "getChunk(", "Attributes.MAX_HEALTH", "Attributes.ATTACK_DAMAGE"], "siege policy")

# Physical logistics contracts retained.
need(field, ["toggleWarehouseNearest", "activeStorageBarrelCount", "MAIN_INVENTORY_FIRST_SLOT = 9", "MAIN_INVENTORY_END_EXCLUSIVE = 36", "public static boolean isBulkMaterial", "offloadBulkMaterials"], "physical logistics")
depot = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotData.java")
need(depot, ['"field_depots_v1"', 'optionalFieldOf("warehouse_links", List.of())', "MAX_DEPOTS_PER_PLAYER = 3", "MAX_LINKED_BARRELS_PER_DEPOT = 8"], "warehouse")
outpost = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java")
need(outpost, ["nearestActiveOutpost(ServerPlayer player, int radius)", "ACTIVE_OWNER_RADIUS = 64", "isRecoveryOperational"], "outpost freight endpoint")

# Carried admissions and expedition rules remain independent from logistics.
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(apex, ["ECHO_SHARD_COST = 8", "AMETHYST_COST = 32", "GOLD_COST = 32"], "Apex carried entry")
forbid(apex, ["FieldDepotService"], "Apex remote payment")
need(trial, ["ECHO_SHARD_COST = 32", "AMETHYST_COST = 64", "DRAGON_BREATH_COST = 8", "TOTAL_WAVES = 4"], "Trial carried entry")
forbid(trial, ["FieldDepotService", '"minecraft:evoker"'], "Trial policy")
ops = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java")
need(ops, ["START_RADIUS = 4", "WORK_RADIUS = 48", "RETURN_RADIUS = 8", "FORWARD_SHIFT_EXTRA = 48", "OutpostService.isRecoveryOperational"], "operations")
forbid(ops, ["setChunkForced", "addRegionTicket", "getChunk("], "operations")

# Existing work scale remains.
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
need(bore, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12"], "mining budget")
need(wood, ["GLOBAL_LOG_BUDGET_PER_TICK = 64", "LOCAL_LOG_BUDGET_PER_TICK = 12", "448"], "wood scale")
need(harvest, ["GLOBAL_HARVEST_BUDGET_PER_TICK = 64", "LOCAL_HARVEST_BUDGET_PER_TICK = 12", "baseSize = 13"], "harvest scale")

if errors:
    print("SOURCE AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.42.1 guide scrolls and documents current World Ascension instead of embedding patch-history wording")
print("- early mastery requirements are discounted through Lv59 and converge to the retained late quadratic curve at Lv60")
print("- mining XP is material-tiered; copper7/8 no longer outranks obsidian16/crying18")
print("- logistics-backed costs consume nearest usable physical Barrel/통 storage before carried inventory; vanilla crafting and Apex/Trial carried admissions remain separate")
print("- 0.42 physical freight, 0.41 Civil Works causeways and 0.40/0.39/0.38 defense contracts remain")
