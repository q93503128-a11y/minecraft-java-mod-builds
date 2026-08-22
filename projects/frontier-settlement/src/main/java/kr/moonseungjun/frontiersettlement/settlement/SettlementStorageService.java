package kr.moonseungjun.frontiersettlement.settlement;

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

    public static List<BlockPos> storagePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(data.stockpilePos());
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() != BuildingType.WAREHOUSE) continue;
            positions.addAll(WarehouseLayout.storagePositions(building));
        }
        return new ArrayList<>(positions);
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
                if (!stack.isEmpty() && isMetal(stack)) metal += stack.getCount();
            }
        }
        return new SettlementResources(wood, stone, metal, food);
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
        remove(level, positions, amount, SettlementStorageService::isMetal);
        return true;
    }

    public static ItemStack insert(ServerLevel level, SettlementData data, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();
        for (BlockPos pos : storagePositions(data)) {
            if (remaining.isEmpty()) break;
            if (!level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            remaining = SettlementInventory.insert(container, remaining);
        }
        return remaining;
    }

    public static BlockPos findDepositTarget(ServerLevel level, SettlementData data, ItemStack stack) {
        for (BlockPos pos : storagePositions(data)) {
            if (!level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            if (hasRoom(container, stack)) return pos;
        }
        return data.stockpilePos();
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

    private static boolean isMetal(ItemStack stack) {
        return stack.is(Items.IRON_INGOT)
                || stack.is(Items.RAW_IRON)
                || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.RAW_COPPER)
                || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.RAW_GOLD);
    }
}
