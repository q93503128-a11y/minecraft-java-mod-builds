#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
F = ROOT / "projects/frontier-settlement"
SETTLEMENT = F / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one target, found {count}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))


# Version --------------------------------------------------------------------
gradle = F / "gradle.properties"
replace_once(gradle, "mod_version=0.1.0-alpha.113", "mod_version=0.1.0-alpha.114")
with gradle.open("a", encoding="utf-8") as out:
    out.write("\n# Alpha.114 production ecology: farm harvest batches are bounded, lumber workers replant harvested natural trunks, and quarry workers can open a shallow physical quarry face through safe natural overburden instead of requiring pre-exposed stone.\n")

# Production efficiency -------------------------------------------------------
eff = SETTLEMENT / "SettlementProductionEfficiencyService.java"
replace_once(eff,
'''    public static int farmWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 120; case 2 -> 100; case 3 -> 80; default -> 80; };
    }

    /** Number of deterministic crop cohorts; one cohort receives +1 age each tending pass. */''',
'''    public static int farmWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 120; case 2 -> 100; case 3 -> 80; default -> 80; };
    }

    /** Maximum wheat harvested in one staffed-farm work pass. Prevents one mature field from dumping a full stack per pass. */
    public static int farmBatch(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 12; case 2 -> 16; case 3 -> 20; default -> 24; };
    }

    /** Number of deterministic crop cohorts; one cohort receives +1 age each tending pass. */''')
replace_once(eff,
'''            case FARM -> prefix + "자동 식량 생산 · 작물 성장 관리 "
                    + (farmGrowthModulo(grade) == 1 ? "전 구획" : farmGrowthModulo(grade) + "구획 순환");''',
'''            case FARM -> prefix + "자동 식량 생산 · 수확 묶음 " + farmBatch(grade) + " · 작물 성장 관리 "
                    + (farmGrowthModulo(grade) == 1 ? "전 구획" : farmGrowthModulo(grade) + "구획 순환");''')

# Main-settlement production --------------------------------------------------
worker = SETTLEMENT / "SettlementWorkerService.java"
replace_once(worker,
'''    private static final int QUARRY_SEARCH_RADIUS = 96;
    private static final int QUARRY_SEARCH_DOWN = 16;
    private static final int QUARRY_SEARCH_UP = 12;''',
'''    private static final int QUARRY_SEARCH_RADIUS = 96;
    private static final int QUARRY_SEARCH_DOWN = 16;
    private static final int QUARRY_SEARCH_UP = 12;
    private static final int MANAGED_QUARRY_FACE_RADIUS = 28;
    private static final int MANAGED_QUARRY_MAX_OVERBURDEN = 4;''')

replace_once(worker,
'''        int room = cargoRoom(worker, Items.WHEAT);
        int harvested = 0;''',
'''        int room = cargoRoom(worker, Items.WHEAT);
        int harvestLimit = Math.min(room, SettlementProductionEfficiencyService.farmBatch(efficiencyGrade));
        int harvested = 0;''')
replace_once(worker,
'''                if (harvested >= room) continue;
                if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;''',
'''                if (harvested >= harvestLimit) continue;
                if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;''')

replace_once(worker,
'''        BlockPos target = findQuarryTargetForWorker(level, data, worker, quarry.workCenter(), expected);
        if (target == null) {
            if (!carried.isEmpty()) deliverToWorksiteStorage(level, data, worker, quarry, carried);
            else moveNear(level, worker, quarry.workCenter(), 0.82D);
            return;
        }
        if (!withinResourceWorkReach(worker, target, QUARRY_REMOTE_WORK_REACH_SQR)) {
            moveNear(level, worker, target, 0.90D);
            return;
        }
        worker.getNavigation().stop();
        MOVEMENT_WATCHES.remove(worker.getUUID());
        int efficiencyGrade = SettlementProductionEfficiencyService.grade(data);
        if (!workDue(level, quarry, SettlementProductionEfficiencyService.quarryWorkPeriod(efficiencyGrade))) return;
        Item item = level.getBlockState(target).getBlock().asItem();''',
'''        BlockPos target = findQuarryTargetForWorker(level, data, worker, quarry.workCenter(), expected);
        if (target == null) {
            if (!carried.isEmpty()) deliverToWorksiteStorage(level, data, worker, quarry, carried);
            else moveNear(level, worker, quarry.workCenter(), 0.82D);
            return;
        }
        BlockPos approach = quarryApproach(level, data, target);
        if (approach == null) {
            clearResourceTarget(worker);
            return;
        }
        if (!withinResourceWorkReach(worker, approach, QUARRY_REMOTE_WORK_REACH_SQR)) {
            moveNear(level, worker, approach, 0.90D);
            return;
        }
        worker.getNavigation().stop();
        MOVEMENT_WATCHES.remove(worker.getUUID());
        int efficiencyGrade = SettlementProductionEfficiencyService.grade(data);
        int quarryPeriod = SettlementProductionEfficiencyService.quarryWorkPeriod(efficiencyGrade);
        if (!workDue(level, quarry, quarryPeriod)) return;
        if (!level.getBlockState(target.above()).isAir()) {
            if (clearTopQuarryOverburden(level, data, target)) worker.swing(InteractionHand.MAIN_HAND);
            return;
        }
        Item item = level.getBlockState(target).getBlock().asItem();''')

replace_once(worker,
'''        int count = 0;
        for (int y = 0; y < 16 && count < maxCount; y++) {
            BlockPos pos = base.above(y);''',
'''        BlockState originalTrunk = first;
        int count = 0;
        for (int y = 0; y < 32 && count < maxCount; y++) {
            BlockPos pos = base.above(y);''')
replace_once(worker,
'''            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;
            count++;
        }
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static BlockPos findQuarryTargetForWorker''',
'''            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;
            count++;
        }
        if (count > 0) tryReplantHarvestedTree(level, data, base, originalTrunk, item);
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void tryReplantHarvestedTree(ServerLevel level, SettlementData data, BlockPos base,
                                                BlockState originalTrunk, Item item) {
        if (!level.hasChunkAt(base) || isProtected(data, base) || !level.getBlockState(base).isAir()) return;
        // Do not plant under a partially harvested tall trunk. Once the last vertical column is gone,
        // every natural trunk column gets its own sapling; this also naturally reconstructs 2x2 dark-oak stands.
        for (int y = 1; y <= 31; y++) {
            BlockPos above = base.above(y);
            if (!level.hasChunkAt(above)) return;
            BlockState state = level.getBlockState(above);
            if (state.is(BlockTags.LOGS) && state.getBlock().asItem() == item) return;
        }
        if (!isNaturalTreeBase(level, base)) return;
        BlockState sapling = saplingForNaturalLog(originalTrunk);
        if (sapling == null) return;
        level.setBlock(base, sapling, 3);
    }

    private static BlockState saplingForNaturalLog(BlockState trunk) {
        if (trunk.is(Blocks.OAK_LOG)) return Blocks.OAK_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.SPRUCE_LOG)) return Blocks.SPRUCE_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.BIRCH_LOG)) return Blocks.BIRCH_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.JUNGLE_LOG)) return Blocks.JUNGLE_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.ACACIA_LOG)) return Blocks.ACACIA_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.DARK_OAK_LOG)) return Blocks.DARK_OAK_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.MANGROVE_LOG)) return Blocks.MANGROVE_PROPAGULE.defaultBlockState();
        if (trunk.is(Blocks.CHERRY_LOG)) return Blocks.CHERRY_SAPLING.defaultBlockState();
        return null;
    }

    private static BlockPos findQuarryTargetForWorker''')

replace_once(worker,
'''            BlockState state = level.getBlockState(cached.pos());
            Item item = state.getBlock().asItem();
            if (isQuarryStone(state) && item != Items.AIR && (expected == null || item == expected)
                    && level.getBlockState(cached.pos().above()).isAir()
                    && canWorkOrApproach(level, worker, cached.pos(), QUARRY_REMOTE_WORK_REACH_SQR)) {
                return cached.pos();
            }''',
'''            BlockState state = level.getBlockState(cached.pos());
            Item item = state.getBlock().asItem();
            BlockPos approach = quarryApproach(level, data, cached.pos());
            if (isQuarryStone(state) && item != Items.AIR && (expected == null || item == expected)
                    && approach != null
                    && canWorkOrApproach(level, worker, approach, QUARRY_REMOTE_WORK_REACH_SQR)) {
                return cached.pos();
            }''')
replace_once(worker,
'''        BlockPos target = findExposedStone(level, data, worker, center, QUARRY_SEARCH_RADIUS, expected);
        if (target == null) {
            RESOURCE_SEARCH_RETRY_AFTER.put(id, now + RESOURCE_SEARCH_RETRY_TICKS);
            return null;
        }''',
'''        BlockPos target = findExposedStone(level, data, worker, center, QUARRY_SEARCH_RADIUS, expected);
        if (target == null) target = findManagedQuarryStone(level, data, worker, center, expected);
        if (target == null) {
            RESOURCE_SEARCH_RETRY_AFTER.put(id, now + RESOURCE_SEARCH_RETRY_TICKS);
            return null;
        }''')

replace_once(worker,
'''        return null;
    }

    private static ItemStack harvestStoneCluster(ServerLevel level, SettlementData data, BlockPos base,''',
'''        return null;
    }

    /**
     * A quarry is a physical excavation, not a requirement that the player first expose stone by hand.
     * If no natural exposed face exists, find a nearby real stone column hidden by at most four safe
     * natural cover blocks. The worker removes that cover one real block per work pass; only after the
     * stone is physically exposed can the normal quarry harvest add stone cargo.
     */
    private static BlockPos findManagedQuarryStone(ServerLevel level, SettlementData data,
                                                   FrontierWorkerEntity worker, BlockPos center, Item expected) {
        for (int radius = 6; radius <= MANAGED_QUARRY_FACE_RADIUS; radius++) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int y = center.getY() - 8; y <= center.getY() + 2; y++) {
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos) || isProtected(data, pos)) continue;
                        BlockState state = level.getBlockState(pos);
                        Item item = state.getBlock().asItem();
                        if (!isQuarryStone(state) || item == Items.AIR || (expected != null && item != expected)) continue;
                        int cover = quarryOverburdenDepth(level, data, pos);
                        if (cover <= 0) continue;
                        BlockPos approach = pos.above(cover + 1);
                        if (isBlockedOutsideWorkReach(level, worker, approach, QUARRY_REMOTE_WORK_REACH_SQR)
                                || !canWorkOrApproach(level, worker, approach, QUARRY_REMOTE_WORK_REACH_SQR)) continue;
                        double distance = pos.distSqr(center);
                        if (distance < bestDistance) { best = pos; bestDistance = distance; }
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static BlockPos quarryApproach(ServerLevel level, SettlementData data, BlockPos target) {
        int cover = quarryOverburdenDepth(level, data, target);
        if (cover < 0) return null;
        return cover == 0 ? target : target.above(cover + 1);
    }

    /** 0 = already exposed, 1..N = safe natural cover, -1 = not a managed quarry candidate. */
    private static int quarryOverburdenDepth(ServerLevel level, SettlementData data, BlockPos stone) {
        if (!level.hasChunkAt(stone) || !isQuarryStone(level.getBlockState(stone)) || isProtected(data, stone)) return -1;
        BlockPos first = stone.above();
        if (!level.hasChunkAt(first)) return -1;
        if (level.getBlockState(first).isAir()) return 0;
        for (int depth = 1; depth <= MANAGED_QUARRY_MAX_OVERBURDEN; depth++) {
            BlockPos pos = stone.above(depth);
            if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null || isProtected(data, pos)) return -1;
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || !isSafeQuarryOverburden(state)) return -1;
            BlockPos above = pos.above();
            if (!level.hasChunkAt(above)) return -1;
            if (level.getBlockState(above).isAir()) return depth;
        }
        return -1;
    }

    private static boolean clearTopQuarryOverburden(ServerLevel level, SettlementData data, BlockPos stone) {
        int depth = quarryOverburdenDepth(level, data, stone);
        if (depth <= 0) return false;
        BlockPos top = stone.above(depth);
        if (!level.hasChunkAt(top) || level.getBlockEntity(top) != null || isProtected(data, top)) return false;
        BlockState state = level.getBlockState(top);
        if (!state.getFluidState().isEmpty() || !isSafeQuarryOverburden(state)) return false;
        return level.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
    }

    private static boolean isSafeQuarryOverburden(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK);
    }

    private static ItemStack harvestStoneCluster(ServerLevel level, SettlementData data, BlockPos base,''')

# Specialized outposts --------------------------------------------------------
outpost = SETTLEMENT / "SettlementOutpostProductionService.java"
replace_once(outpost,
'''    private static final int TREE_RADIUS = 18;
    private static final int QUARRY_RADIUS = 16;
    private static final int MAX_LOGS = 4;
    private static final int MAX_STONE = 3;''',
'''    private static final int TREE_RADIUS = 18;
    private static final int QUARRY_RADIUS = 16;
    private static final int MANAGED_QUARRY_FACE_RADIUS = 14;
    private static final int MANAGED_QUARRY_MAX_OVERBURDEN = 4;
    private static final int MAX_LOGS = 8;
    private static final int MAX_STONE = 8;''')

replace_once(outpost,
'''    private static void workQuarry(ServerLevel level, SettlementData data, OutpostRecord outpost, FrontierWorkerEntity worker) {
        BlockPos target = findExposedStone(level, data, outpost.center(), QUARRY_RADIUS);
        if (target == null) {
            move(worker, outpost.center().above(), 0.65D);
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 9.0D) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, target, 0.78D, 9.0D);
            return;
        }
        if (!workDue(level, outpost, QUARRY_WORK_PERIOD_TICKS)) return;
        ItemStack harvested = harvestStoneCluster(level, data, target);''',
'''    private static void workQuarry(ServerLevel level, SettlementData data, OutpostRecord outpost, FrontierWorkerEntity worker) {
        BlockPos target = findExposedStone(level, data, outpost.center(), QUARRY_RADIUS);
        if (target == null) target = findManagedQuarryStone(level, data, outpost.center());
        if (target == null) {
            move(worker, outpost.center().above(), 0.65D);
            return;
        }
        BlockPos approach = quarryApproach(level, data, target);
        if (approach == null) return;
        if (worker.distanceToSqr(approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D) > 9.0D) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, approach, 0.78D, 9.0D);
            return;
        }
        if (!workDue(level, outpost, QUARRY_WORK_PERIOD_TICKS)) return;
        if (!level.getBlockState(target.above()).isAir()) {
            if (clearTopQuarryOverburden(level, data, target)) worker.swing(InteractionHand.MAIN_HAND);
            return;
        }
        ItemStack harvested = harvestStoneCluster(level, data, target);''')

replace_once(outpost,
'''        int count = 0;
        for (int y = 0; y < 16 && count < MAX_LOGS; y++) {''',
'''        BlockState originalTrunk = first;
        int count = 0;
        for (int y = 0; y < 32 && count < MAX_LOGS; y++) {''')
replace_once(outpost,
'''            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;
            count++;
        }
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static BlockPos findExposedStone''',
'''            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;
            count++;
        }
        if (count > 0) tryReplantHarvestedTree(level, data, base, originalTrunk, item);
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void tryReplantHarvestedTree(ServerLevel level, SettlementData data, BlockPos base,
                                                BlockState originalTrunk, Item item) {
        if (!level.hasChunkAt(base) || isProtected(data, base) || !level.getBlockState(base).isAir()) return;
        for (int y = 1; y <= 31; y++) {
            BlockPos above = base.above(y);
            if (!level.hasChunkAt(above)) return;
            BlockState state = level.getBlockState(above);
            if (state.is(BlockTags.LOGS) && state.getBlock().asItem() == item) return;
        }
        if (!isNaturalTreeGround(level.getBlockState(base.below()))) return;
        BlockState sapling = saplingForNaturalLog(originalTrunk);
        if (sapling != null) level.setBlock(base, sapling, 3);
    }

    private static boolean isNaturalTreeGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.MUD);
    }

    private static BlockState saplingForNaturalLog(BlockState trunk) {
        if (trunk.is(Blocks.OAK_LOG)) return Blocks.OAK_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.SPRUCE_LOG)) return Blocks.SPRUCE_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.BIRCH_LOG)) return Blocks.BIRCH_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.JUNGLE_LOG)) return Blocks.JUNGLE_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.ACACIA_LOG)) return Blocks.ACACIA_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.DARK_OAK_LOG)) return Blocks.DARK_OAK_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.MANGROVE_LOG)) return Blocks.MANGROVE_PROPAGULE.defaultBlockState();
        if (trunk.is(Blocks.CHERRY_LOG)) return Blocks.CHERRY_SAPLING.defaultBlockState();
        return null;
    }

    private static BlockPos findExposedStone''')

replace_once(outpost,
'''        return best;
    }

    private static ItemStack harvestStoneCluster(ServerLevel level, SettlementData data, BlockPos base) {''',
'''        return best;
    }

    private static BlockPos findManagedQuarryStone(ServerLevel level, SettlementData data, BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = 7; radius <= MANAGED_QUARRY_FACE_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int y = center.getY() - 7; y <= center.getY() + 2; y++) {
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos) || isProtected(data, pos) || !isQuarryStone(level.getBlockState(pos))) continue;
                        int cover = quarryOverburdenDepth(level, data, pos);
                        if (cover <= 0) continue;
                        double distance = pos.distSqr(center);
                        if (distance < bestDistance) { best = pos; bestDistance = distance; }
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static BlockPos quarryApproach(ServerLevel level, SettlementData data, BlockPos target) {
        int cover = quarryOverburdenDepth(level, data, target);
        if (cover < 0) return null;
        return cover == 0 ? target : target.above(cover + 1);
    }

    private static int quarryOverburdenDepth(ServerLevel level, SettlementData data, BlockPos stone) {
        if (!level.hasChunkAt(stone) || !isQuarryStone(level.getBlockState(stone)) || isProtected(data, stone)) return -1;
        BlockPos first = stone.above();
        if (!level.hasChunkAt(first)) return -1;
        if (level.getBlockState(first).isAir()) return 0;
        for (int depth = 1; depth <= MANAGED_QUARRY_MAX_OVERBURDEN; depth++) {
            BlockPos pos = stone.above(depth);
            if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null || isProtected(data, pos)) return -1;
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || !isSafeQuarryOverburden(state)) return -1;
            BlockPos above = pos.above();
            if (!level.hasChunkAt(above)) return -1;
            if (level.getBlockState(above).isAir()) return depth;
        }
        return -1;
    }

    private static boolean clearTopQuarryOverburden(ServerLevel level, SettlementData data, BlockPos stone) {
        int depth = quarryOverburdenDepth(level, data, stone);
        if (depth <= 0) return false;
        BlockPos top = stone.above(depth);
        if (!level.hasChunkAt(top) || level.getBlockEntity(top) != null || isProtected(data, top)) return false;
        BlockState state = level.getBlockState(top);
        if (!state.getFluidState().isEmpty() || !isSafeQuarryOverburden(state)) return false;
        return level.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
    }

    private static boolean isSafeQuarryOverburden(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK);
    }

    private static ItemStack harvestStoneCluster(ServerLevel level, SettlementData data, BlockPos base) {''')

# Current-source verification -------------------------------------------------
verify = F / "tools/test_current_source.py"
replace_once(verify, 'require("mod_version=0.1.0-alpha.113" in gradle, "current verifier/version drift")',
                    'require("mod_version=0.1.0-alpha.114" in gradle, "current verifier/version drift")')
replace_once(verify,
'''require("SettlementProductionEfficiencyService.farmWorkPeriod" in worker, "farm still uses fixed work cadence")
require("state.setValue(BlockStateProperties.AGE_7, Math.min(7, age + 1))" in worker, "staffed farm does not actively tend crop growth")''',
'''require("SettlementProductionEfficiencyService.farmWorkPeriod" in worker, "farm still uses fixed work cadence")
require("SettlementProductionEfficiencyService.farmBatch" in worker and "harvestLimit" in worker,
        "staffed farm harvest is not bounded against full-stack-per-pass runaway")
require("state.setValue(BlockStateProperties.AGE_7, Math.min(7, age + 1))" in worker, "staffed farm does not actively tend crop growth")
require("tryReplantHarvestedTree" in worker and "saplingForNaturalLog" in worker,
        "town lumber worker no longer restores a physical managed forestry cycle")
require("findManagedQuarryStone" in worker and "MANAGED_QUARRY_MAX_OVERBURDEN = 4" in worker
        and "clearTopQuarryOverburden" in worker,
        "town quarry still requires player-pre-exposed stone")''')
replace_once(verify,
'''production_efficiency = text(SETTLEMENT / "SettlementProductionEfficiencyService.java")
require("SettlementTier.current(data)" in production_efficiency, "production upgrades are not derived from canonical settlement tier")''',
'''production_efficiency = text(SETTLEMENT / "SettlementProductionEfficiencyService.java")
require("SettlementTier.current(data)" in production_efficiency, "production upgrades are not derived from canonical settlement tier")
require("farmBatch" in production_efficiency and "case 1 -> 12" in production_efficiency and "default -> 24" in production_efficiency,
        "farm harvest batch ladder missing or drifted")''')
replace_once(verify,
'''for stale in ("FARM_WORK_PERIOD_TICKS", "LUMBER_WORK_PERIOD_TICKS", "QUARRY_WORK_PERIOD_TICKS", "MINING_WORK_PERIOD_TICKS"):
    require(stale not in worker, f"stale fixed production pacing authority returned: {stale}")''',
'''for stale in ("FARM_WORK_PERIOD_TICKS", "LUMBER_WORK_PERIOD_TICKS", "QUARRY_WORK_PERIOD_TICKS", "MINING_WORK_PERIOD_TICKS"):
    require(stale not in worker, f"stale fixed production pacing authority returned: {stale}")
outpost_production = text(SETTLEMENT / "SettlementOutpostProductionService.java")
require("MAX_LOGS = 8" in outpost_production and "MAX_STONE = 8" in outpost_production,
        "specialized outpost lumber/quarry batches did not receive the physical throughput correction")
require("findManagedQuarryStone" in outpost_production and "clearTopQuarryOverburden" in outpost_production,
        "specialized quarry outpost still requires pre-exposed stone")
require("tryReplantHarvestedTree" in outpost_production,
        "specialized lumber outpost lacks managed physical replanting")''')

print("PATCH APPLIED: Frontier alpha114 production ecology/balance")
