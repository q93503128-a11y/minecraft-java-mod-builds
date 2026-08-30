#!/usr/bin/env python3
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
WORKER = JAVA / "settlement/SettlementWorkerService.java"
CONSTRUCTION = JAVA / "settlement/SettlementConstructionService.java"
CONTEXT = JAVA / "settlement/SettlementContextService.java"
PROPS = ROOT / "gradle.properties"
LOCK = ROOT / "COMPANION_LOCK.json"


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one occurrence, found {count}")
    return text.replace(old, new, 1)


def sub_once(text, pattern, replacement, label):
    result, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return result


props = read(PROPS)
if "mod_version=0.1.0-alpha.86" not in props:
    raise SystemExit("Alpha.87 requires Alpha.86 project base")
props = replace_once(props, "mod_version=0.1.0-alpha.86", "mod_version=0.1.0-alpha.87", "version")
props = props.rstrip() + "\n\n# Alpha.87 production logistics: one-stack batching, wider loaded-only search, automatic farm replanting, and non-blocking 100% construction finalization.\n"
write(PROPS, props)

worker = read(WORKER)
worker = sub_once(
    worker,
    r'    private static final long ARRIVAL_FOOD_COST = 4L;\n'
    r'    private static final int TREE_SEARCH_RADIUS = 18;\n'
    r'    private static final int MAX_LOGS_PER_TRIP = 4;\n'
    r'    private static final int MAX_CROPS_PER_TRIP = 4;\n'
    r'    private static final int MAX_STONE_PER_TRIP = 3;\n',
    '''    private static final long ARRIVAL_FOOD_COST = 4L;
    private static final int LOCAL_RESOURCE_ROUTE_MARGIN = 56;
    private static final int TREE_SEARCH_RADIUS = 48;
    private static final int QUARRY_SEARCH_RADIUS = 40;
    private static final int MINE_HORIZONTAL_SEARCH_RADIUS = 24;
    private static final int MINE_SEARCH_DEPTH = 48;
    private static final int PRODUCTION_HAUL_STACK = 64;
    private static final int MAX_LOGS_PER_WORK = 12;
    private static final int MAX_STONE_PER_WORK = 16;
''',
    "worker constants",
)
worker = worker.replace(
    "workerRouteEvidenceLoaded(level, data, building.workCenter(), 24)",
    "workerRouteEvidenceLoaded(level, data, building.workCenter(), LOCAL_RESOURCE_ROUTE_MARGIN)",
)
worker = worker.replace(
    "workerRouteBounds(data, workCenter, 24)",
    "workerRouteBounds(data, workCenter, LOCAL_RESOURCE_ROUTE_MARGIN)",
)
worker = sub_once(
    worker,
    r'        for \(BuildingRecord building : data\.buildings\(\)\) \{\n'
    r'            if \(building\.buildingType\(\) != BuildingType\.HOUSE\) continue;\n'
    r'            minX = Math\.min\(minX, building\.originX\(\) - margin\);\n'
    r'            maxX = Math\.max\(maxX, building\.originX\(\) \+ building\.rotatedWidth\(\) - 1 \+ margin\);\n'
    r'            minZ = Math\.min\(minZ, building\.originZ\(\) - margin\);\n'
    r'            maxZ = Math\.max\(maxZ, building\.originZ\(\) \+ building\.rotatedDepth\(\) - 1 \+ margin\);\n'
    r'        \}\n',
    '',
    "remove dormant house-rest evidence",
)

work_methods = '''    private static void workLumber(ServerLevel level, SettlementData data,
                                   FrontierWorkerEntity worker, BuildingRecord camp) {
        ItemStack carried = worker.getMainHandItem();
        Item expected = carried.isEmpty() ? null : carried.getItem();
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToTownStorage(level, data, worker, carried);
            return;
        }
        BlockPos target = findTree(level, data, camp.workCenter(), expected);
        if (target == null) {
            if (!carried.isEmpty()) deliverToTownStorage(level, data, worker, carried);
            else move(worker, camp.workCenter(), 0.82D);
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 8.0D) {
            move(worker, target, 0.92D);
            return;
        }
        if (!workDue(level, camp, LUMBER_WORK_PERIOD_TICKS)) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToTownStorage(level, data, worker, carried);
            return;
        }
        ItemStack harvested = harvestVerticalTrunk(level, data, target, item, Math.min(MAX_LOGS_PER_WORK, room));
        if (!harvested.isEmpty() && appendCargo(worker, harvested)) worker.swing(InteractionHand.MAIN_HAND);
    }

    private static void workFarm(ServerLevel level, SettlementData data,
                                 FrontierWorkerEntity worker, BuildingRecord farm) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty() && !carried.is(Items.WHEAT)) {
            deliverToTownStorage(level, data, worker, carried);
            return;
        }
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToTownStorage(level, data, worker, carried);
            return;
        }
        if (worker.distanceToSqr(farm.workCenter().getX() + 0.5D, farm.workCenter().getY(), farm.workCenter().getZ() + 0.5D) > 64.0D) {
            move(worker, farm.workCenter(), 0.88D);
            return;
        }
        if (!workDue(level, farm, FARM_WORK_PERIOD_TICKS)) return;
        BuildingType type = farm.buildingType();
        if (type == null) return;
        int room = cargoRoom(worker, Items.WHEAT);
        int harvested = 0;
        int replanted = 0;
        for (int x = 0; x < type.width(); x++) {
            for (int z = 0; z < type.depth(); z++) {
                BlockPos crop = farm.origin().offset(x, 1, z);
                if (!level.hasChunkAt(crop) || !level.hasChunkAt(crop.below())) continue;
                BlockState state = level.getBlockState(crop);
                BlockState soil = level.getBlockState(crop.below());
                if (state.isAir() && soil.is(Blocks.FARMLAND)) {
                    if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) replanted++;
                    continue;
                }
                if (harvested >= room || !state.is(Blocks.WHEAT)
                        || !state.hasProperty(BlockStateProperties.AGE_7)
                        || state.getValue(BlockStateProperties.AGE_7) < 7) continue;
                if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;
            }
        }
        if (harvested > 0) {
            if (appendCargo(worker, new ItemStack(Items.WHEAT, harvested))) worker.swing(InteractionHand.MAIN_HAND);
            return;
        }
        if (!worker.getMainHandItem().isEmpty() && replanted == 0) {
            deliverToTownStorage(level, data, worker, worker.getMainHandItem());
        }
    }

    private static void workQuarry(ServerLevel level, SettlementData data,
                                   FrontierWorkerEntity worker, BuildingRecord quarry) {
        ItemStack carried = worker.getMainHandItem();
        Item expected = carried.isEmpty() ? null : carried.getItem();
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToTownStorage(level, data, worker, carried);
            return;
        }
        BlockPos target = findExposedStone(level, data, quarry.workCenter(), QUARRY_SEARCH_RADIUS, expected);
        if (target == null) {
            if (!carried.isEmpty()) deliverToTownStorage(level, data, worker, carried);
            else move(worker, quarry.workCenter(), 0.82D);
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 9.0D) {
            move(worker, target, 0.90D);
            return;
        }
        if (!workDue(level, quarry, QUARRY_WORK_PERIOD_TICKS)) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToTownStorage(level, data, worker, carried);
            return;
        }
        ItemStack stone = harvestStoneCluster(level, data, target, item, Math.min(MAX_STONE_PER_WORK, room));
        if (!stone.isEmpty() && appendCargo(worker, stone)) worker.swing(InteractionHand.MAIN_HAND);
    }

    private static void workMine(ServerLevel level, SettlementData data,
                                 FrontierWorkerEntity worker, BuildingRecord mine) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToTownStorage(level, data, worker, carried);
            return;
        }
        BlockPos work = mine.workCenter();
        if (worker.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > 16.0D) {
            move(worker, work, 0.86D);
            return;
        }
        if (!workDue(level, mine, MINING_WORK_PERIOD_TICKS)) return;
        Item expected = carried.isEmpty() ? null : carried.getItem();
        BlockPos ore = findOreBelow(level, data, work, expected);
        if (ore == null) {
            if (!carried.isEmpty()) deliverToTownStorage(level, data, worker, carried);
            return;
        }
        ItemStack preview = previewMineDrop(level.getBlockState(ore));
        if (preview.isEmpty()) return;
        int room = cargoRoom(worker, preview.getItem());
        if (room <= 0) {
            deliverToTownStorage(level, data, worker, carried);
            return;
        }
        ItemStack mined = mineOre(level, ore, room);
        if (!mined.isEmpty() && appendCargo(worker, mined)) worker.swing(InteractionHand.MAIN_HAND);
    }

    private static int cargoLimit(ItemStack stack) {
        return Math.min(PRODUCTION_HAUL_STACK, stack.getMaxStackSize());
    }

    private static int cargoRoom(FrontierWorkerEntity worker, Item item) {
        ItemStack carried = worker.getMainHandItem();
        if (carried.isEmpty()) {
            ItemStack probe = new ItemStack(item);
            return Math.min(PRODUCTION_HAUL_STACK, probe.getMaxStackSize());
        }
        if (carried.getItem() != item) return 0;
        return Math.max(0, cargoLimit(carried) - carried.getCount());
    }

    private static boolean appendCargo(FrontierWorkerEntity worker, ItemStack gained) {
        if (gained.isEmpty()) return false;
        ItemStack carried = worker.getMainHandItem();
        if (carried.isEmpty()) {
            int amount = Math.min(gained.getCount(), Math.min(PRODUCTION_HAUL_STACK, gained.getMaxStackSize()));
            if (amount <= 0) return false;
            worker.setItemSlot(EquipmentSlot.MAINHAND, gained.copyWithCount(amount));
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(carried, gained)) return false;
        int amount = Math.min(gained.getCount(), cargoLimit(carried) - carried.getCount());
        if (amount <= 0) return false;
        carried.grow(amount);
        worker.setItemSlot(EquipmentSlot.MAINHAND, carried);
        return true;
    }

'''
worker = sub_once(
    worker,
    r'    private static void workLumber\(.*?\n    private static boolean workDue\(',
    work_methods + '    private static boolean workDue(',
    "production work methods",
)

tree_search = '''    private static BlockPos findTree(ServerLevel level, SettlementData data, BlockPos center, Item expected) {
        for (int radius = 0; radius <= TREE_SEARCH_RADIUS; radius++) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    for (int y = center.getY() - 6; y <= center.getY() + 16; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!level.hasChunkAt(pos)) continue;
                        BlockState state = level.getBlockState(pos);
                        if (!state.is(BlockTags.LOGS) || isProtected(data, pos) || !hasLeavesAbove(level, pos)) continue;
                        Item item = state.getBlock().asItem();
                        if (item == Items.AIR || (expected != null && item != expected)) continue;
                        double distance = pos.distSqr(center);
                        if (distance < bestDistance) { best = pos; bestDistance = distance; }
                        break;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static boolean hasLeavesAbove'''
worker = sub_once(
    worker,
    r'    private static BlockPos findTree\(.*?\n    private static boolean hasLeavesAbove',
    tree_search,
    "tree search",
)

harvest_and_quarry = '''    private static ItemStack harvestVerticalTrunk(ServerLevel level, SettlementData data, BlockPos base,
                                                  Item expected, int maxCount) {
        if (maxCount <= 0 || !level.hasChunkAt(base)) return ItemStack.EMPTY;
        BlockState first = level.getBlockState(base);
        if (!first.is(BlockTags.LOGS)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR || (expected != null && item != expected)) return ItemStack.EMPTY;
        int count = 0;
        for (int y = 0; y < 16 && count < maxCount; y++) {
            BlockPos pos = base.above(y);
            if (!level.hasChunkAt(pos)) break;
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) || state.getBlock().asItem() != item || isProtected(data, pos)) {
                if (count > 0) break;
                continue;
            }
            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;
            count++;
        }
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static BlockPos findExposedStone(ServerLevel level, SettlementData data, BlockPos center,
                                             int radiusLimit, Item expected) {
        for (int radius = 0; radius <= radiusLimit; radius++) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int y = center.getY() - 8; y <= center.getY() + 6; y++) {
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) continue;
                        BlockState state = level.getBlockState(pos);
                        Item item = state.getBlock().asItem();
                        if (!isQuarryStone(state) || item == Items.AIR || (expected != null && item != expected)
                                || isProtected(data, pos) || !level.getBlockState(pos.above()).isAir()) continue;
                        double distance = pos.distSqr(center);
                        if (distance < bestDistance) { best = pos; bestDistance = distance; }
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static ItemStack harvestStoneCluster(ServerLevel level, SettlementData data, BlockPos base,
                                                 Item expected, int maxCount) {
        if (maxCount <= 0 || !level.hasChunkAt(base)) return ItemStack.EMPTY;
        BlockState first = level.getBlockState(base);
        if (!isQuarryStone(first)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR || (expected != null && item != expected)) return ItemStack.EMPTY;
        int count = 0;
        for (int dx = -2; dx <= 2 && count < maxCount; dx++) {
            for (int dz = -2; dz <= 2 && count < maxCount; dz++) {
                BlockPos pos = base.offset(dx, 0, dz);
                if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) continue;
                BlockState state = level.getBlockState(pos);
                if (state.getBlock().asItem() != item || !isQuarryStone(state) || isProtected(data, pos)
                        || !level.getBlockState(pos.above()).isAir()) continue;
                if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) count++;
            }
        }
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static boolean isQuarryStone'''
worker = sub_once(
    worker,
    r'    private static ItemStack harvestVerticalTrunk\(.*?\n    private static boolean isQuarryStone',
    harvest_and_quarry,
    "tree/quarry harvesting",
)

mine_methods = '''    private static BlockPos findOreBelow(ServerLevel level, SettlementData data, BlockPos center, Item expected) {
        for (int depth = 2; depth <= MINE_SEARCH_DEPTH; depth++) {
            int y = center.getY() - depth;
            for (int radius = 0; radius <= MINE_HORIZONTAL_SEARCH_RADIUS; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos)) continue;
                        BlockState state = level.getBlockState(pos);
                        if (!state.is(Tags.Blocks.ORES) || isProtected(data, pos)) continue;
                        ItemStack preview = previewMineDrop(state);
                        if (preview.isEmpty() || (expected != null && preview.getItem() != expected)) continue;
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ItemStack previewMineDrop(BlockState state) {
        if (!state.is(Tags.Blocks.ORES)) return ItemStack.EMPTY;
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) return new ItemStack(Items.RAW_IRON);
        if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) return new ItemStack(Items.RAW_COPPER, 2);
        if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) return new ItemStack(Items.RAW_GOLD);
        if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) return new ItemStack(Items.COAL);
        if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) return new ItemStack(Items.DIAMOND);
        if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) return new ItemStack(Items.EMERALD);
        if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) return new ItemStack(Items.REDSTONE, 4);
        if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) return new ItemStack(Items.LAPIS_LAZULI, 4);
        Item item = state.getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack mineOre(ServerLevel level, BlockPos pos, int maxCount) {
        if (maxCount <= 0 || !level.hasChunkAt(pos)) return ItemStack.EMPTY;
        BlockState state = level.getBlockState(pos);
        ItemStack result = previewMineDrop(state);
        if (result.isEmpty()) return ItemStack.EMPTY;
        if (result.getCount() > maxCount) result = result.copyWithCount(maxCount);
        if (!level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)) return ItemStack.EMPTY;
        return result;
    }

    private static boolean isProtected'''
worker = sub_once(
    worker,
    r'    private static BlockPos findOreBelow\(.*?\n    private static boolean isProtected',
    mine_methods,
    "mine search/harvest",
)
write(WORKER, worker)

construction = read(CONSTRUCTION)
finish = '''    private static boolean finishIfValid(MinecraftServer server, SettlementData data, BuildingType type,
                                         List<BuildingBlueprints.Placement> plan, FrontierWorkerEntity builder,
                                         Container crate, BlockPos supply) {
        ServerLevel level = server.overworld();
        for (BuildingBlueprints.Placement placement : plan) {
            if (!level.hasChunkAt(placement.pos())) return false;
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!current.isAir()) {
                builder.getNavigation().stop();
                return false;
            }
            if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;
            if (!moveBuilderToWorkPosition(level, data.construction(), type, placement, builder, supply)) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }

        // A valid, physically finished structure owns completion. Alpha.86 incorrectly made the
        // builder's return walk part of the commit condition, so one failed path could hold 99% forever.
        consolidateCompletionCargo(builder, crate, supply);
        boolean keepPhysicalLeftovers = !crateIsEmpty(crate) || !builder.getMainHandItem().isEmpty();
        if (!removeConstructionScaffolds(level, data.construction(), type, supply)) return false;
        if (!keepPhysicalLeftovers && level.getBlockState(supply).is(Blocks.BARREL)
                && !level.setBlock(supply, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return false;

        data.completeConstruction(type);
        builder.setInvulnerable(false);
        builder.setCustomName(Component.literal(BUILDER_NAME));

        // Best-effort physical return only. It can no longer block completion; no teleport/force-load.
        returnBuilderHome(level, data, builder);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return true;
    }

    private static void consolidateCompletionCargo(FrontierWorkerEntity builder, Container crate, BlockPos supply) {
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty()) return;
        if (builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) return;
        ItemStack remaining = SettlementInventory.insert(crate, carried);
        builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
    }

    private static boolean returnCrateExtrasPhysically'''
construction = sub_once(
    construction,
    r'    private static boolean finishIfValid\(.*?\n    private static boolean returnCrateExtrasPhysically',
    finish,
    "non-blocking completion",
)
write(CONSTRUCTION, construction)

context = read(CONTEXT)
context = replace_once(context, "Math.min(99,", "Math.min(100,", "100-percent progress")
write(CONTEXT, context)

lock = json.loads(read(LOCK))
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.86":
    raise SystemExit("Alpha.87 lock requires Alpha.86 target")
lock["generated_at"] = "2026-08-30"
lock["target"]["frontier_settlement"] = "0.1.0-alpha.87"
lock["notes"].append(
    "Alpha.87 keeps every Alpha.86 companion binary pin unchanged. Local production workers accumulate compatible real output up to one 64-item stack before ordinary return trips, search a wider loaded-only work envelope, farms automatically restore missing wheat on their own farmland, and a physically complete building no longer waits at 99% for the builder to path home. No force-loading, teleport logistics, periodic civilian upkeep, virtual resource ledger, or second construction authority is introduced."
)
write(LOCK, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

doc = '''# Frontier Settlement Alpha.87 — production logistics and finalization

Alpha.87 is a save-compatible runtime pass over Alpha.86. It adds no required settlement SavedData field.

## Production logistics
- Lumber, farm, quarry, and mine workers keep real output in MAINHAND and can accumulate one compatible 64-item stack before an ordinary storage return.
- Lumber search expands from 18 to 48 blocks, quarry search from 14 to 40 blocks, and mine search expands to 24 blocks horizontally / 48 blocks deep.
- Search is loaded-only. Frontier never force-loads a remote resource chunk and never teleports a worker.
- Tree and quarry search uses expanding rings so nearby targets are selected without always scanning the maximum radius first.
- The farm blueprint already starts planted with wheat. Harvest returns mature wheat to age 0, and Alpha.87 additionally restores an AIR crop cell over the farm's own FARMLAND to age-0 wheat. Players do not need to supply seeds.

## Population and ordinary civilian cost
- Founding starts at population 1.
- A HOUSE adds 4 housing capacity; capacity is a ceiling, not an instant resident spawn.
- Once every 600 server ticks (30 seconds), at most one vacant loaded worker assignment may attract a resident if population is below housing capacity.
- A successful ordinary arrival consumes 4 real food exactly once. Failed entity insertion does not charge food or population.
- There is no periodic ordinary-civilian food/tax/upkeep drain in Alpha.87.

## 99% construction recovery
Alpha.86 validated the finished blueprint and then required the shared builder to reach the town anchor before completeConstruction. A path failure could leave a physically complete building at 99% forever.

Alpha.87 makes structural validation the completion authority. Scaffolds are cleaned locally, exact leftover physical cargo remains in the site barrel or builder hand, an empty site barrel is removed, the building commits immediately, and builder return becomes a best-effort physical navigation order. Active progress can display 100% during final cleanup rather than being capped at 99%.

A pre-existing Alpha.86 save already stuck at 99% should finalize after loading Alpha.87 as soon as the completed site is loaded and validates.

## Update boundary
This is a same-world update, but not a JVM hot reload. Fully close Minecraft before replacing Alpha.86 / installing Alpha.87. Back up the world before its first Alpha.87 launch.
'''
write(ROOT / "PRODUCTION_AND_FINISH_ALPHA87.md", doc)

source_audit = '''#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A86 = ROOT / "tools/test_alpha86_source.py"
ALPHA86_SHA = "132b6f09715f1f8225cb5f5e581f163bf43fe949"
LEGACY_FILES = {
    "projects/frontier-settlement/gradle.properties",
    "projects/frontier-settlement/COMPANION_LOCK.json",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementContextService.java",
}
_real_read = Path.read_text
def alpha86_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in LEGACY_FILES:
        return subprocess.check_output(["git", "show", f"{ALPHA86_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha86_read
try:
    chain = _real_read(A86, encoding="utf-8").replace('print("Frontier Settlement alpha.23-86 cumulative source audit: PASS")', 'pass')
    ns = {"__file__": str(A86), "__name__": "__main__"}
    exec(compile(chain, str(A86), "exec"), ns, ns)
finally: Path.read_text = _real_read
def text(path): return Path(path).read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")
props = text(ROOT / "gradle.properties")
worker = text(JAVA / "settlement/SettlementWorkerService.java")
construction = text(JAVA / "settlement/SettlementConstructionService.java")
context = text(JAVA / "settlement/SettlementContextService.java")
data = text(JAVA / "settlement/SettlementData.java")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))
must(props, ("mod_version=0.1.0-alpha.87", "Alpha.87 production logistics"), "alpha.87 props")
must(worker, ("LOCAL_RESOURCE_ROUTE_MARGIN = 56", "TREE_SEARCH_RADIUS = 48", "QUARRY_SEARCH_RADIUS = 40", "MINE_HORIZONTAL_SEARCH_RADIUS = 24", "MINE_SEARCH_DEPTH = 48", "PRODUCTION_HAUL_STACK = 64", "state.isAir() && soil.is(Blocks.FARMLAND)", "findTree(level, data, camp.workCenter(), expected)", "findExposedStone(level, data, quarry.workCenter(), QUARRY_SEARCH_RADIUS, expected)", "findOreBelow(level, data, work, expected)"), "alpha.87 production")
forbid(worker, ("TREE_SEARCH_RADIUS = 18", "MAX_LOGS_PER_TRIP = 4", "MAX_CROPS_PER_TRIP = 4", "MAX_STONE_PER_TRIP = 3"), "alpha.87 obsolete production")
forbid(worker, ("setChunkForced", "forceChunk", "teleportTo("), "alpha.87 no resource shortcut")
must(construction, ("consolidateCompletionCargo(builder, crate, supply);", "boolean keepPhysicalLeftovers", "data.completeConstruction(type);", "returnBuilderHome(level, data, builder);", "can no longer block completion"), "alpha.87 completion")
forbid(construction, ("if (!returnBuilderHome(level, data, builder)) return false;",), "alpha.87 99-percent gate")
if "Math.min(100," not in context or "Math.min(99," in context: raise SystemExit("alpha.87 progress must reach 100")
if 'optionalFieldOf("population"' not in data or 'optionalFieldOf("housing_capacity"' not in data: raise SystemExit("alpha.87 save compatibility fields drifted")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.87": raise SystemExit("alpha.87 lock mismatch")
if not any("Alpha.87 keeps every Alpha.86 companion binary pin unchanged" in n for n in lock.get("notes", [])): raise SystemExit("alpha.87 lock rationale missing")
print("Frontier Settlement alpha.23-87 cumulative source audit: PASS")
'''
write(ROOT / "tools/test_alpha87_source.py", source_audit)

docs_audit = '''#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
A86 = ROOT / "tools/test_alpha86_docs.py"
ALPHA86_SHA = "132b6f09715f1f8225cb5f5e581f163bf43fe949"
_real_read = Path.read_text
def alpha86_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in {"projects/frontier-settlement/gradle.properties", "projects/frontier-settlement/COMPANION_LOCK.json"}:
        return subprocess.check_output(["git", "show", f"{ALPHA86_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha86_read
try:
    chain = _real_read(A86, encoding="utf-8").replace('print("Frontier Settlement alpha.86 canonical docs audit: PASS")', 'pass')
    ns = {"__file__": str(A86), "__name__": "__main__"}
    exec(compile(chain, str(A86), "exec"), ns, ns)
finally: Path.read_text = _real_read
note = (ROOT / "PRODUCTION_AND_FINISH_ALPHA87.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
lock = json.loads((ROOT / "COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.87", "64-item stack", "48 blocks", "40 blocks", "AIR crop cell", "population 1", "4 housing capacity", "600 server ticks", "4 real food", "no periodic ordinary-civilian", "99% forever", "best-effort", "same-world update"):
    if token not in note: raise SystemExit(f"alpha.87 note missing: {token}")
if "mod_version=0.1.0-alpha.87" not in props: raise SystemExit("alpha.87 version missing")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.87": raise SystemExit("alpha.87 lock mismatch")
print("Frontier Settlement alpha.87 canonical docs audit: PASS")
'''
write(ROOT / "tools/test_alpha87_docs.py", docs_audit)

print("Alpha.87 patch applied")
