package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class SettlementStorageService {
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

    public static List<BlockPos> ordinaryStoragePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(data.stockpilePos());
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.WAREHOUSE) {
                positions.addAll(WarehouseLayout.storagePositions(building));
            } else if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.freightPositions(building));
            }
        }
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
        List<BlockPos> positions = storagePositions(data);
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
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty() && isMetalStack(stack)) metal += stack.getCount();
            }
        }
        return new SettlementResources(wood, stone, metal, food);
    }

    public static boolean storageAvailable(ServerLevel level, SettlementData data) {
        return allStorageChunksLoaded(level, storagePositions(data));
    }

    public static boolean consume(ServerLevel level, SettlementData data, long wood, long stone, long food) {
        List<BlockPos> positions = storagePositions(data);
        if (!allStorageChunksLoaded(level, positions)) return false;
        SettlementResources resources = scan(level, data);
        if (resources.wood() < wood || resources.stone() < stone || resources.food() < food) return false;
        remove(level, positions, wood, SettlementInventory::isWood);
        remove(level, positions, stone, SettlementInventory::isStone);
        remove(level, positions, food, SettlementInventory::isFood);
        return true;
    }

    public static boolean consumeMetal(ServerLevel level, SettlementData data, long amount) {
        if (amount <= 0L) return true;
        List<BlockPos> positions = storagePositions(data);
        if (!allStorageChunksLoaded(level, positions)) return false;
        SettlementResources resources = scan(level, data);
        if (resources.metal() < amount) return false;
        remove(level, positions, amount, SettlementStorageService::isMetalStack);
        return true;
    }

    /** Atomic physical recruitment cost: never eat food first and then fail on missing metal. */
    public static boolean consumeMetalAndFood(ServerLevel level, SettlementData data, long metal, long food) {
        if (metal < 0L || food < 0L) return false;
        List<BlockPos> positions = storagePositions(data);
        if (!allStorageChunksLoaded(level, positions)) return false;
        SettlementResources resources = scan(level, data);
        if (resources.metal() < metal || resources.food() < food) return false;
        remove(level, positions, metal, SettlementStorageService::isMetalStack);
        remove(level, positions, food, SettlementInventory::isFood);
        return true;
    }

    public static ItemStack insert(ServerLevel level, SettlementData data, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();
        for (BlockPos pos : depositPositions(data, stack)) {
            if (remaining.isEmpty()) break;
            remaining = insertAt(level, pos, remaining);
        }
        return remaining;
    }

    private static List<BlockPos> depositPositions(SettlementData data, ItemStack stack) {
        // Wood/stone production can naturally feed the construction office. Food, metal and random
        // loot stay out of its dedicated material bays unless a player deliberately puts them there.
        if (SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)) return storagePositions(data);
        return ordinaryStoragePositions(data);
    }

    public static ItemStack insertAt(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!level.hasChunkAt(pos)) return stack.copy();
        if (!(level.getBlockEntity(pos) instanceof Container container)) return stack.copy();
        return SettlementInventory.insert(container, stack.copy());
    }

    public static BlockPos findDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {
        for (BlockPos pos : depositPositions(data, stack)) {
            if (!level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            if (hasRoom(container, stack)) return pos;
        }
        return data.stockpilePos();
    }

    /** Outpost deliveries prefer a visible cart-station freight bay before ordinary town storage. */
    public static BlockPos findLogisticsDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {
        for (BlockPos pos : cartStationFreightPositions(data)) {
            if (!level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            if (hasRoom(container, stack)) return pos;
        }
        return findDepositTarget(level, data, stack);
    }

    public static BlockPos findExtractionTarget(ServerLevel level, SettlementData data, Predicate<ItemStack> predicate) {
        return findExtractionTargetExcluding(level, data, predicate, Set.of());
    }

    public static BlockPos findExtractionTargetExcluding(ServerLevel level, SettlementData data,
                                                         Predicate<ItemStack> predicate, Set<BlockPos> excluded) {
        for (BlockPos pos : storagePositions(data)) {
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
        if (!SettlementInventory.isMetalResource(stack)) return false;
        return stack.is(Items.IRON_INGOT)
                || stack.is(Items.RAW_IRON)
                || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.RAW_COPPER)
                || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.RAW_GOLD)
                || stack.is(ExternalContentTags.C_INGOTS)
                || stack.is(ExternalContentTags.C_RAW_MATERIALS)
                || stack.is(ExternalContentTags.SETTLEMENT_METAL);
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
