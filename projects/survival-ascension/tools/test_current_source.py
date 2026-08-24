#!/usr/bin/env python3
from pathlib import Path
import re
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
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/FreightRailheadService.java",
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
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.46.0-alpha.1"], "toolchain")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "protocol")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.46.0-alpha.1"', "ConstructionProgression::onBlockPlaced", "OutpostSiegeBreachService::onServerTick", "OutpostSiegeSystem::onServerTick"], "main")

# 0.44 external/content-pack equipment imprint.
affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
need(affix, [
    'BASE_NAME = "base_name"', "ItemTags.SWORDS", "ItemTags.PICKAXES", "ItemTags.AXES", "ItemTags.SHOVELS", "ItemTags.HOES",
    "public static boolean canImprint(ItemStack stack)", "stack.getMaxStackSize() == 1", "categoryForItem(stack) != Category.NONE",
    "public static boolean imprint(ItemStack stack, RandomSource random, int requestedRarity)",
    "root.putString(BASE_NAME, baseName)", "stack.getHoverName().getString()",
    "stack.update(DataComponents.CUSTOM_DATA", "tag.put(ROOT, root)"
], "0.44 external gear imprint")
forbid(affix, ["biomesoplenty", "tbos", "amethyst_resonance"], "0.44 hard optional-mod equipment dependency")
need(affix, ["GEAR_CATEGORIES", "Category.SHOVEL", "public static int adjustShovelArea", "Math.min(13, base + bonus)", "Items.NETHERITE_SHOVEL"], "0.46 shovel affix category")

reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
need(reforge, [
    "ACTION_IMPRINT = 3", "WorldAscensionData.get(player.getServer()).stage()", "stage + 1",
    "AscensionAffixes.canImprint(held)", "AscensionAffixes.imprint(held, player.level().getRandom(), rarity)",
    "new MaterialCost(Items.AMETHYST_SHARD, 24", "new MaterialCost(Items.AMETHYST_SHARD, 48", "new MaterialCost(Items.AMETHYST_SHARD, 96",
    "FieldDepotService.countMaterial", "FieldDepotService.consume"
], "0.44 imprint routing/costs")

equipment_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java")
need(equipment_ui, [
    'new Entry("승천 각인"', "Action.IMPRINT", "AscensionAffixes.canImprint(held)",
    "EquipmentReforgeService.ACTION_IMPRINT", "new EquipmentActionPayload(EquipmentReforgeService.ACTION_IMPRINT)"
], "0.44 equipment radial")

# Current guide/pacing/mining/logistics-priority contracts.
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(guide, [
    "CONTENT_TOP = 64", "CONTENT_BOTTOM_MARGIN = 44", "SCROLL_STEP = 30",
    "mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)",
    "graphics.enableScissor", "graphics.disableScissor", "maxScroll", "scrollOffset",
    'h("월드 승천")', "위더를 처음 격파하면 전설 단계(1)", "엔더 드래곤을 처음 격파하면 종말 단계(2)",
    'h("외부 장비와 승천 각인")', 'h("외부 바이옴 원정")', 'h("승천 각인")', "표준 검/곡괭이/도끼/삽/괭이 태그 장비",
    "물류 통과 창고군", "일반 바닐라 작업대 조합은 조합칸/인벤토리 규칙을 그대로 따릅니다.",
    'h("화물 하역장")', "레일6개 이상", "동력레일1개 이상", "호퍼1개 이상"
], "0.44 scrollable current-state guide")
forbid(guide, ["0.44부터", "0.43부터", "0.42부터", "0.41부터", "0.40부터"], "guide patch-note policy")

tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning, [
    "long base = 40L + 8L * level + Math.round(1.5D * level * level);",
    "if (level < 10) factor = 0.20D + 0.03D * level;",
    "else if (level < 30) factor = 0.50D + 0.015D * (level - 10);",
    "else if (level < 60) factor = 0.80D + 0.0065D * (level - 30);",
    "else factor = 1.0D;",
    "return Math.max(8L, Math.round(base * factor));"
], "early mastery curve")

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
], "mining XP tiers")
forbid(mining, ["if (state.is(VALUABLE_ORES)) return 20;"], "flat valuable-ore XP")
need(mining, ["ItemTags.SHOVELS", "BlockTags.MINEABLE_WITH_SHOVEL", "handleShovelBreak", "isValidShovelBreak", "breakShovelArea", "AscensionAffixes.adjustShovelArea", "SkillType.MINING"], "0.46 shovel Mining integration")
forbid(mining, ["adjustMiningVeinLimit(tool, SkillTuning.miningVeinLimit(miningLevel)); // shovel"], "shovel vein coupling")

field = read("src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java")
need(field, [
    "재료 소비: 모드 제작·건축·인프라 비용은 가까운 사용 가능 물류 통부터",
    "List<Container> containers = usableContainers(player);",
    "player.containerMenu.broadcastChanges();"
], "logistics storage-first")
ordered(field, [
    "public static boolean consumeMatching",
    "List<Container> containers = usableContainers(player);",
    "for (Container container : containers)",
    "player.getInventory().getContainerSize() && remaining > 0"
], "storage before carried inventory")
forbid(field, ["setChunkForced", "addRegionTicket", "getChunk("], "physical logistics")

prod_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(prod_ui, ["창고 통 연결", "4블록 내 기본 통 앵커", "물리 화물 수레", "레일6+·동력레일·호퍼·제어", "Items.CHEST_MINECART", "Action.FREIGHT", "ProductionService.ACTION_FREIGHT"], "0.43 logistics/freight UI")

# 0.43 physical freight railheads; 0.42 transfer semantics retained.
freight = read("src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java")
need(freight, [
    'OWNER_KEY = "survivalascension_freight_owner"',
    'ORIGIN_DIMENSION_KEY = "survivalascension_freight_origin_dimension"',
    'ORIGIN_X_KEY = "survivalascension_freight_origin_x"',
    'ORIGIN_Y_KEY = "survivalascension_freight_origin_y"',
    'ORIGIN_Z_KEY = "survivalascension_freight_origin_z"',
    "INTERACTION_RADIUS = 4", "InfrastructureProject.INDUSTRIAL_WORKS", "InfrastructureProject.CIVIL_WORKS",
    "MinecartChest", "OutpostService.nearestActiveOutpost(player, INTERACTION_RADIUS)", "BlockTags.RAILS",
    "FreightRailheadService.validate(player, outpost, cart)", "FreightRailheadService.sendStatus(player, outpost, cart)",
    "FieldDepotService.isBulkMaterial", "FieldDepotData.get(player).linkedBarrels(player, depot)",
    "level.hasChunkAt(pos)", "level.mayInteract(player, pos)", "Blocks.BARREL", "blockEntity instanceof Container",
    "ItemStack.isSameItemSameComponents(source, existing)", "target.canPlaceItem(slot, source)",
    "target.getMaxStackSize(source)", "source.copyWithCount(move)", "destination.pos().equals(origin)", "clearManifest(cart)"
], "0.43 freight")
forbid(freight, ["SavedData", "setChunkForced", "addRegionTicket", "getChunk(", "addFreshEntity", "teleportTo", "randomTeleport", "consumeSupplyCharge", "giveOrDrop", "award("], "freight physical-only policy")
ordered(freight, ["if (!isOnLoadedRail(level, cart))", "FreightRailheadService.validate(player, outpost, cart)", "String taggedOwner = cart.getPersistentData().getStringOr(OWNER_KEY"], "railhead before freight mutation")

railhead = read("src/main/java/kr/moonseungjun/survivalascension/production/FreightRailheadService.java")
need(railhead, [
    "RAILHEAD_RADIUS = 6", "MIN_RAIL_BLOCKS = 6", "MIN_POWERED_RAILS = 1", "MIN_HOPPERS = 1", "MIN_CONTROLS = 1",
    "BlockTags.RAILS", "Blocks.POWERED_RAIL", "Blocks.HOPPER", "Blocks.LEVER", "Blocks.REDSTONE_BLOCK",
    "level.hasChunkAt(pos)", "level.mayInteract(player, pos)", "cartRailPosition(level, cart)",
    "POWERED_NEAR_CART_SQ = 9", "HOPPER_NEAR_CART_SQ = 9", "CONTROL_NEAR_CART_SQ = 16",
    "poweredNearCart", "hopperNearCart", "controlNearCart", "inspection.complete()"
], "0.43 physical railhead")
forbid(railhead, ["SavedData", "setChunkForced", "addRegionTicket", "getChunk(", "addFreshEntity", "teleportTo", "randomTeleport", "consumeSupplyCharge", "award("], "railhead no-automation/no-force-load policy")

production = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production, ['ACTION_FREIGHT = "physical_freight"', "FreightService.transferNearest(player)", "FreightService.sendStatus(player)", "레일6+·동력레일·호퍼·제어"], "production routing")
infra = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infra, ["ProductionService.ACTION_FREIGHT", "project == InfrastructureProject.CIVIL_WORKS"], "infrastructure routing")

# Civil Works and causeway retained.
project = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java")
need(project, ['CIVIL_WORKS(', '"civil_works", "토목 공사소"', 'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 2048)', 'new Requirement(Items.COBBLESTONE, "조약돌", 1536)', 'new Requirement(Items.GRAVEL, "자갈", 1536)'], "civil project")
mode = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionMode.java")
need(mode, ['CAUSEWAY("causeway", "도로/교량", 60)'], "construction mode")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(construction, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "MAX_PENDING_BLOCKS_PER_PLAYER = 512", "CAUSEWAY_WIDTH = 3", "ConstructionMode.CAUSEWAY", "InfrastructureProject.CIVIL_WORKS", "fieldMastery ? 65 : SkillTuning.constructionLineLength(level)", "if (!level.hasChunkAt(target)) return PlaceResult.SKIPPED;", "level.mayInteract(player, target)", "EventHooks.onBlockPlace", "FieldDepotService.consumeOne(player, item)", "int localBudget = Math.min(8, budget)"], "causeway runtime")
forbid(construction, ["setChunkForced", "addRegionTicket", "getChunk("], "construction loading policy")
site = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java")
need(site, ["CIVIL_SITE = new SiteProfile(true", "Blocks.SCAFFOLDING", "case CIVIL_WORKS -> CIVIL_SITE", "level.hasChunkAt(pos)", "level.mayInteract(player, pos)"], "commissioning")

# Combat-infrastructure boundaries retained.
breach = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeBreachService.java")
need(breach, ["BASTION_ONLY_WAVE = 4", "RAVAGER_BREAK_COOLDOWN = 30", "VINDICATOR_BREAK_COOLDOWN = 60", "EventHooks.canEntityGrief(level, mob)", "EventHooks.onEntityDestroyBlock", "level.destroyBlock(target.pos(), true, mob)"], "breach")
forbid(breach, ["setChunkForced", "addRegionTicket", "getChunk(", "Attributes.MAX_HEALTH", "Attributes.ATTACK_DAMAGE"], "breach policy")
fort = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostFortificationService.java")
need(fort, ["INNER_RADIUS = 6", "OUTER_RADIUS = 12", "MIN_COLUMNS_PER_QUADRANT = 12", "BlockTags.WALLS", "Blocks.IRON_BARS", "Blocks.NETHER_BRICK_FENCE"], "fortification")
siege = read("src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java")
need(siege, ["DEFENSE_RADIUS = 64", "BREACH_RADIUS = 6", "BREACH_LIMIT = 200", "SUPPLY_CHARGE_COST = 1", "BASTION_SUPPLY_CHARGE_COST = 2", "TOTAL_WAVES = 3", "BASTION_TOTAL_WAVES = 4", "SIEGE_TIMEOUT_TICKS = 4800", "BASTION_TIMEOUT_TICKS = 6000"], "siege")
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

# 0.45 optional external-world expedition bridge.
expedition_region = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java")
need(expedition_region, ["private final TagKey<Biome> integrationTag;", "if (biome.is(integrationTag)) return true;"], "0.45 expedition tag-first bridge")
forbid(expedition_region, ["biomesoplenty.", "tbos.", "amethyst_resonance."], "0.45 hard optional-mod biome dependency")
bop_ids = []
for region in ("woodland", "arid", "wetland", "highlands", "ocean", "deep", "frozen", "nether", "end"):
    tag_text = read(f"src/main/resources/data/survivalascension/tags/worldgen/biome/expedition/{region}.json")
    need(tag_text, ['"replace": false'], f"0.45 expedition tag {region}")
    ids = re.findall(r'"id"\s*:\s*"(biomesoplenty:[^"]+)"', tag_text)
    soft = re.findall(r'\{\s*"id"\s*:\s*"(biomesoplenty:[^"]+)"\s*,\s*"required"\s*:\s*false\s*\}', tag_text)
    if ids != soft:
        errors.append(f"0.45 expedition tag {region} contains non-optional or malformed BOP entry")
    bop_ids.extend(ids)
if len(bop_ids) != len(set(bop_ids)):
    errors.append("0.45 duplicate BOP biome id across expedition tags")
deep_tag = read("src/main/resources/data/survivalascension/tags/worldgen/biome/expedition/deep.json")
need(deep_tag, ['"biomesoplenty:glowing_grotto"', '"biomesoplenty:spider_nest"'], "0.45 BOP deep bridge")

if errors:
    print("SOURCE AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft26.2 / NeoForge26.2.0.38-beta / Java25 / protocol8")
print("- 0.46 standard shovels join Mining mastery as bounded planar earthworks; Scale/Secondary shovel affixes are functional and capped")
print("- 0.45 optional expedition Biome Tags bridge external worldgen before vanilla fallback; BOP entries stay required=false and Deep includes glowing_grotto + spider_nest")
print("- 0.44 standard-tagged external swords/pickaxes/axes/hoes remain imprinted into the existing affix system without hard optional-mod dependencies")
print("- imprint rarity follows World Ascension stage0/1/2 -> Elite/Ascended/Mythic and spends real logistics-backed materials")
print("- external item components stay on the same stack; Survival Ascension writes only nested affix CustomData/display name with a retained base name")
print("- 0.43 physical freight still requires a loaded real railhead at both exact active-outpost endpoints")
print("- freight remains physical-only: no reward, auto-drive, virtual route, SavedData, teleport or force-load")
print("- guide scrolls and documents current World Ascension and equipment integration instead of embedding patch-history wording")
print("- early mastery requirements remain discounted through Lv59 and mining XP remains material-tiered")
print("- logistics-backed costs consume nearest usable physical Barrel/통 storage before carried inventory; vanilla crafting and Apex/Trial carried admissions remain separate")
print("- Civil Works causeways and physical defense contracts remain")
