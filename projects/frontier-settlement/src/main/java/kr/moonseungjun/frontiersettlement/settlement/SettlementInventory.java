package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SettlementInventory {
    private SettlementInventory() {}

    public static long countWood(Container container) {
        return count(container, SettlementInventory::isWood);
    }

    public static long countStone(Container container) {
        return count(container, SettlementInventory::isStone);
    }

    public static long countFood(Container container) {
        return count(container, SettlementInventory::isFood);
    }

    public static boolean consume(Container container, long wood, long stone, long food) {
        if (countWood(container) < wood || countStone(container) < stone || countFood(container) < food) {
            return false;
        }
        consumeMatching(container, wood, SettlementInventory::isWood);
        consumeMatching(container, stone, SettlementInventory::isStone);
        consumeMatching(container, food, SettlementInventory::isFood);
        container.setChanged();
        return true;
    }

    public static ItemStack insert(Container container, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();

        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, remaining)) continue;
            int room = current.getMaxStackSize() - current.getCount();
            if (room <= 0) continue;
            int move = Math.min(room, remaining.getCount());
            current.grow(move);
            remaining.shrink(move);
        }

        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (!container.getItem(slot).isEmpty()) continue;
            int move = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            container.setItem(slot, remaining.copyWithCount(move));
            remaining.shrink(move);
        }

        container.setChanged();
        return remaining;
    }

    public static boolean isWood(ItemStack stack) {
        return stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS);
    }

    public static boolean isStone(ItemStack stack) {
        return stack.is(Items.STONE)
                || stack.is(Items.COBBLESTONE)
                || stack.is(Items.DEEPSLATE)
                || stack.is(Items.COBBLED_DEEPSLATE)
                || stack.is(Items.ANDESITE)
                || stack.is(Items.DIORITE)
                || stack.is(Items.GRANITE);
    }

    public static boolean isFood(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null;
    }

    private static long count(Container container, java.util.function.Predicate<ItemStack> predicate) {
        long total = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) total += stack.getCount();
        }
        return total;
    }

    private static void consumeMatching(Container container, long amount,
                                        java.util.function.Predicate<ItemStack> predicate) {
        long left = amount;
        for (int slot = 0; slot < container.getContainerSize() && left > 0L; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) continue;
            int take = (int) Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
    }
}
