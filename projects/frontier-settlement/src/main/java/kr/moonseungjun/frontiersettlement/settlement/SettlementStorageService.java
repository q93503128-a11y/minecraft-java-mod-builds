package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;
import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class SettlementStorageService {
    // Alpha.91 used these cells as a vanilla-barrel public-storage cluster. Keep the layout
    // only as a one-way save migration map. New settlements receive one 54-slot shared depot;
    // extra shared capacity is crafted and placed by the player.
    private static final int[][] LEGACY_PUBLIC_STOCKPILE_OFFSETS = {
            {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };

    private SettlementStorageService() {}

    /**
     * Construction-office material bays are intentionally first for extraction so the existing
     * builder automatically prefers staged physical material without gaining a second construction
     * authority. They remain part of the same ItemStack ledger.
     */
    public static List<BlockPos> storagePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(constructionOfficeSupplyPositions(data));
        positions.addAll(ordinaryStoragePositions(data));
        return new ArrayList<>(positions);
    }

    /** Shared depots are opt-in physical storage. Only currently loaded depots join the authoritative
     * town ledger, so an unloaded optional depot never blocks ordinary settlement costs. */
    private static List<BlockPos> activeStoragePositions(ServerLevel level, SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(SupplyDepotRegistryService.loadedPositions(level, data));
        positions.addAll(storagePositions(data));
        return new ArrayList<>(positions);
    }

    /** The persisted founding stockpile is the dedicated shared supply depot. */
    public static List<BlockPos> publicStockpilePositions(SettlementData data) {
        return List.of(data.stockpilePos());
    }

    public static BlockPos worksiteStoragePosition(BuildingRecord building) {
        BuildingType type = building.buildingType();
        if (type == BuildingType.LUMBER_CAMP) return building.localToWorld(5, 1, 6);
        if (type == BuildingType.FARM) return building.localToWorld(6, 1, 2);
        if (type == BuildingType.QUARRY || type == BuildingType.MINE) return building.localToWorld(5, 1, 2);
        return null;
    }

    public static List<BlockPos> worksiteStoragePositions(SettlementData data) {
        List<BlockPos> positions = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) {
            BlockPos pos = worksiteStoragePosition(building);
            if (pos != null) positions.add(pos);
        }
        return positions;
    }

    public static boolean isManagedStoragePosition(SettlementData data, BlockPos pos) {
        for (BlockPos candidate : publicStockpilePositions(data)) {
            if (candidate.equals(pos)) return true;
        }
        for (BlockPos candidate : worksiteStoragePositions(data)) {
            if (candidate.equals(pos)) return true;
        }
        return false;
    }

    /**
     * Save-compatible storage maintenance. The starter public stockpile is a 54-slot dedicated
     * shared supply depot. Alpha.91 public barrels are upgraded in-place with every ItemStack
     * preserved; profession worksite barrels remain local physical buffers.
     */
    public static void ensureManagedStorage(ServerLevel level, SettlementData data) {
        // One-way compatibility gate: only a world whose authoritative saved stockpile is still a
        // vanilla barrel can be an Alpha.91 public-barrel save. New settlements start with the
        // dedicated depot, so placing a cheap barrel near it can never mint a free shared depot.
        BlockPos stockpile = data.stockpilePos();
        boolean legacyPublicStorage = level.hasChunkAt(stockpile) && level.getBlockState(stockpile).is(Blocks.BARREL);
        if (legacyPublicStorage) upgradeLegacyPublicBarrels(level, data);
        ensureStarterSupplyDepot(level, stockpile);
        for (BlockPos pos : worksiteStoragePositions(data)) {
            if (!canSafelyCreateManagedBarrel(level, pos)) continue;
            level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
        }
    }

    private static void upgradeLegacyPublicBarrels(ServerLevel level, SettlementData data) {
        BlockPos origin = data.stockpilePos();
        for (int[] offset : LEGACY_PUBLIC_STOCKPILE_OFFSETS) {
            BlockPos pos = origin.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(Blocks.BARREL)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container oldContainer)) continue;
            replaceBarrelWithSupplyDepot(level, pos, oldContainer);
        }
    }

    private static void ensureStarterSupplyDepot(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) return;
        BlockState current = level.getBlockState(pos);
        if (current.is(FrontierContent.SUPPLY_DEPOT.get()) && level.getBlockEntity(pos) instanceof Container) {
            SupplyDepotRegistryService.tryRegister(level, pos);
            return;
        }
        if (current.is(Blocks.BARREL) && level.getBlockEntity(pos) instanceof Container oldContainer) {
            replaceBarrelWithSupplyDepot(level, pos, oldContainer);
            return;
        }
        BlockState below = level.getBlockState(pos.below());
        if (level.getBlockEntity(pos) != null) return;
        if (!current.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return;
        if (!current.isAir() && !current.canBeReplaced()) return;
        if (below.isAir() || below.canBeReplaced()) return;
        if (level.setBlock(pos, FrontierContent.SUPPLY_DEPOT.get().defaultBlockState(), 3)
                && level.getBlockEntity(pos) instanceof Container) {
            SupplyDepotRegistryService.tryRegister(level, pos);
        }
    }

    private static void replaceBarrelWithSupplyDepot(ServerLevel level, BlockPos pos, Container oldContainer) {
        List<ItemStack> preserved = new ArrayList<>(oldContainer.getContainerSize());
        for (int slot = 0; slot < oldContainer.getContainerSize(); slot++) {
            preserved.add(oldContainer.getItem(slot).copy());
            oldContainer.setItem(slot, ItemStack.EMPTY);
        }
        oldContainer.setChanged();
        BlockState oldState = level.getBlockState(pos);
        boolean placed = level.setBlock(pos, FrontierContent.SUPPLY_DEPOT.get().defaultBlockState(), 3);
        if (!placed || !(level.getBlockEntity(pos) instanceof Container replacement)) {
            level.setBlock(pos, oldState, 3);
            if (level.getBlockEntity(pos) instanceof Container rollback) restoreItems(rollback, preserved);
            return;
        }
        restoreItems(replacement, preserved);
        SupplyDepotRegistryService.tryRegister(level, pos);
    }

    private static void restoreItems(Container target, List<ItemStack> preserved) {
        int limit = Math.min(target.getContainerSize(), preserved.size());
        for (int slot = 0; slot < limit; slot++) target.setItem(slot, preserved.get(slot).copy());
        target.setChanged();
    }

    private static boolean canSafelyCreateManagedBarrel(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) return false;
        BlockState current = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        if (current.is(Blocks.BARREL) && level.getBlockEntity(pos) instanceof Container) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (!current.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if (!current.isAir() && !current.canBeReplaced()) return false;
        return !below.isAir() && !below.canBeReplaced();
    }

    private static List<BlockPos> generalStoragePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(publicStockpilePositions(data));
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.WAREHOUSE) {
                positions.addAll(WarehouseLayout.storagePositions(building));
            } else if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.freightPositions(building));
            }
        }
        return new ArrayList<>(positions);
    }

    public static List<BlockPos> ordinaryStoragePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(generalStoragePositions(data));
        positions.addAll(worksiteStoragePositions(data));
        return new ArrayList<>(positions);
    }

    public static List<BlockPos> constructionOfficeSupplyPositions(SettlementData data) {
        List<BlockPos> positions = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.CONSTRUCTION_OFFICE) {
                positions.addAll(ConstructionOfficeLayout.materialPositions(building));
            }
        }
        return positions;
    }

    public static List<BlockPos> cartStationFreightPositions(SettlementData data) {
        List<BlockPos> positions = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.freightPositions(building));
            }
        }
        return positions;
    }

    public static SettlementResources scan(ServerLevel level, SettlementData data) {
        List<BlockPos> positions = activeStoragePositions(level, data);
        // Never overwrite the confirmed ledger with a partial scan while players are exploring far
        // from the town. Costs are likewise blocked until all physical town storage is loaded.
        if (!allStorageChunksLoaded(level, positions)) return data.resources();

        long wood = 0L;
        long stone = 0L;
        long metal = 0L;
        long food = 0L;
        for (BlockPos pos : positions) {
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            wood += SettlementInventory.countWood(container);
            stone += SettlementInventory.countStone(container);
            food += SettlementInventory.countFood(container);
            metal += SettlementInventory.countMetal(container);
        }
        return new SettlementResources(wood, stone, metal, food);
    }

    public static boolean storageAvailable(ServerLevel level, SettlementData data) {
        return allStorageChunksLoaded(level, activeStoragePositions(level, data));
    }

    public static boolean consume(ServerLevel level, SettlementData data, long wood, long stone, long food) {
        List<BlockPos> positions = activeStoragePositions(level, data);
        if (!allStorageChunksLoaded(level, positions)) return false;
        SettlementResources resources = scan(level, data);
        if (resources.wood() < wood || resources.stone() < stone || resources.food() < food) return false;
        remove(level, positions, wood, SettlementInventory::isWood);
        remove(level, positions, stone, SettlementInventory::isStone);
        removeValue(level, positions, food, SettlementInventory::foodValue);
        return true;
    }

    public static boolean consumeMetal(ServerLevel level, SettlementData data, long amount) {
        if (amount <= 0L) return true;
        List<BlockPos> positions = activeStoragePositions(level, data);
        if (!allStorageChunksLoaded(level, positions)) return false;
        SettlementResources resources = scan(level, data);
        if (resources.metal() < amount) return false;
        removeValue(level, positions, amount, SettlementInventory::metalValue);
        return true;
    }

    /** Atomic physical recruitment cost: never eat food first and then fail on missing metal. */
    public static boolean consumeMetalAndFood(ServerLevel level, SettlementData data, long metal, long food) {
        if (metal < 0L || food < 0L) return false;
        List<BlockPos> positions = activeStoragePositions(level, data);
        if (!allStorageChunksLoaded(level, positions)) return false;
        SettlementResources resources = scan(level, data);
        if (resources.metal() < metal || resources.food() < food) return false;
        removeValue(level, positions, metal, SettlementInventory::metalValue);
        removeValue(level, positions, food, SettlementInventory::foodValue);
        return true;
    }

    public static ItemStack insert(ServerLevel level, SettlementData data, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();
        for (BlockPos pos : depositPositions(level, data, stack)) {
            if (remaining.isEmpty()) break;
            remaining = insertAt(level, pos, remaining);
        }
        return remaining;
    }

    private static List<BlockPos> depositPositions(ServerLevel level, SettlementData data, ItemStack stack) {
        // Generic delivery never steals another profession's local barrel. Local production workers
        // explicitly target their own worksite barrel first; shared overflow uses only public/warehouse/
        // cart storage (plus construction-office material bays for wood/stone).
        Set<BlockPos> positions = new LinkedHashSet<>();
        if (SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)) {
            positions.addAll(constructionOfficeSupplyPositions(data));
        }
        positions.addAll(SupplyDepotRegistryService.loadedPositions(level, data));
        positions.addAll(generalStoragePositions(data));
        return new ArrayList<>(positions);
    }

    public static ItemStack insertAt(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!level.hasChunkAt(pos)) return stack.copy();
        if (!(level.getBlockEntity(pos) instanceof Container container)) return stack.copy();
        return SettlementInventory.insert(container, stack.copy());
    }

    public static BlockPos findDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {
        BlockPos target = findDepositTargetExcluding(level, data, stack, Set.of());
        return target == null ? data.stockpilePos() : target;
    }

    public static BlockPos findDepositTargetExcluding(ServerLevel level, SettlementData data,
                                                      ItemStack stack, Set<BlockPos> excluded) {
        for (BlockPos pos : depositPositions(level, data, stack)) {
            if (excluded.contains(pos) || !level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            if (hasRoom(container, stack)) return pos;
        }
        BlockPos stockpile = data.stockpilePos();
        if (!excluded.contains(stockpile) && hasRoomAt(level, stockpile, stack)) return stockpile;
        return null;
    }

    /**
     * Ordinary profession output must enter the shared/public economy before construction staging.
     * Construction-office bays are intentionally excluded here: otherwise lumber/quarry output can
     * disappear into a builder-only staging buffer and never become visible to Survival Ascension's
     * shared-depot bridge until that buffer fills. No chunk is force-loaded and a full network simply
     * returns null so the worker keeps the physical cargo.
     */
    public static BlockPos findProductionDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {
        return findProductionDepositTargetExcluding(level, data, stack, Set.of());
    }

    /**
     * Production hauling must be able to skip a depot that this worker has already proved unreachable.
     * Otherwise the deterministic shared-depot-first ordering can make a worker retry the same blocked
     * container forever even while another loaded shared/general container has free space.
     */
    public static BlockPos findProductionDepositTargetExcluding(ServerLevel level, SettlementData data,
                                                                ItemStack stack, Set<BlockPos> excluded) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(SupplyDepotRegistryService.loadedPositions(level, data));
        positions.addAll(generalStoragePositions(data));
        for (BlockPos pos : positions) {
            if (excluded.contains(pos) || !level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            if (hasRoom(container, stack)) return pos;
        }
        return null;
    }

    /** Outpost deliveries prefer a visible cart-station freight bay before ordinary town storage. */
    public static BlockPos findLogisticsDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {
        BlockPos target = findLogisticsDepositTargetExcluding(level, data, stack, Set.of());
        return target == null ? data.stockpilePos() : target;
    }

    /**
     * Reachability-aware transport callers need to exclude a freight/storage target already proved
     * unusable by this worker. Unlike the legacy convenience wrapper, this method returns null when
     * no real loaded container with room remains; it never fabricates the stockpile as a fallback.
     */
    public static BlockPos findLogisticsDepositTargetExcluding(ServerLevel level, SettlementData data,
                                                               ItemStack stack, Set<BlockPos> excluded) {
        for (BlockPos pos : cartStationFreightPositions(data)) {
            if (excluded.contains(pos) || !level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            if (hasRoom(container, stack)) return pos;
        }
        return findDepositTargetExcluding(level, data, stack, excluded);
    }

    public static BlockPos findExtractionTarget(ServerLevel level, SettlementData data, Predicate<ItemStack> predicate) {
        return findExtractionTargetExcluding(level, data, predicate, Set.of());
    }

    public static BlockPos findExtractionTargetExcluding(ServerLevel level, SettlementData data,
                                                         Predicate<ItemStack> predicate, Set<BlockPos> excluded) {
        for (BlockPos pos : activeStoragePositions(level, data)) {
            if (excluded.contains(pos) || !level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty() && predicate.test(stack)) return pos;
            }
        }
        return null;
    }

    public static boolean hasRoomAt(ServerLevel level, BlockPos pos, ItemStack incoming) {
        if (!level.hasChunkAt(pos)) return false;
        if (!(level.getBlockEntity(pos) instanceof Container container)) return false;
        return hasRoom(container, incoming);
    }

    public static ItemStack extract(ServerLevel level, BlockPos source, Predicate<ItemStack> predicate, int maxCount) {
        if (maxCount <= 0 || !level.hasChunkAt(source)) return ItemStack.EMPTY;
        if (!(level.getBlockEntity(source) instanceof Container container)) return ItemStack.EMPTY;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty() || !predicate.test(current)) continue;
            int take = Math.min(maxCount, current.getCount());
            ItemStack result = current.copyWithCount(take);
            current.shrink(take);
            container.setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    public static boolean isMetalStack(ItemStack stack) {
        // Classification and value share one authority; remote extraction cannot drift from town costs.
        return SettlementInventory.metalValue(stack) > 0;
    }

    private static boolean allStorageChunksLoaded(ServerLevel level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) return false;
        }
        return true;
    }

    private static boolean hasRoom(Container container, ItemStack incoming) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(current, incoming) && current.getCount() < current.getMaxStackSize()) return true;
        }
        return false;
    }

    private static void removeValue(ServerLevel level, List<BlockPos> positions,
                                    long amount, ToIntFunction<ItemStack> valuation) {
        long left = amount;
        // Lower-value stock is consumed first; scarce gold or rare food remains a last resort.
        for (int unit = 1; unit <= 24 && left > 0L; unit++) {
            for (BlockPos pos : positions) {
                if (left <= 0L) break;
                if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
                boolean changed = false;
                for (int slot = 0; slot < container.getContainerSize() && left > 0L; slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (stack.isEmpty() || Math.max(0, valuation.applyAsInt(stack)) != unit) continue;
                    int take = (int)Math.min(stack.getCount(), (left + unit - 1L) / unit);
                    stack.shrink(take);
                    left -= (long)take * unit;
                    changed = true;
                }
                if (changed) container.setChanged();
            }
        }
    }

    private static void remove(ServerLevel level, List<BlockPos> positions,
                               long amount, Predicate<ItemStack> predicate) {
        long left = amount;
        for (BlockPos pos : positions) {
            if (left <= 0L) break;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            for (int slot = 0; slot < container.getContainerSize() && left > 0L; slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !predicate.test(stack)) continue;
                int take = (int) Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
            container.setChanged();
        }
    }
}
