package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SettlementInventory {
    public record ResourceCounts(long wood, long stone, long metal, long food) {}

    private static final int RESOURCE_WOOD = 1;
    private static final int RESOURCE_STONE = 2;
    private static final int RESOURCE_METAL = 4;
    private static final int RESOURCE_FOOD = 8;

    private SettlementInventory() {}

    public static long countWood(Container container) { return count(container, SettlementInventory::isWood); }
    public static long countStone(Container container) { return count(container, SettlementInventory::isStone); }
    public static long countMetal(Container container) { return countValue(container, SettlementInventory::metalValue); }
    public static long countFood(Container container) { return countValue(container, SettlementInventory::foodValue); }

    /**
     * One physical container pass for the shared settlement ledger. The old scan called four public
     * counters independently, which re-read every slot and re-ran tag classification four times every
     * refresh. This preserves the exact exclusive-resource rules while doing the classification once.
     */
    public static ResourceCounts countResources(Container container) {
        long wood = 0L;
        long stone = 0L;
        long metal = 0L;
        long food = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            int mask = resourceMask(stack);
            int amount = stack.getCount();
            if (mask == RESOURCE_WOOD) wood += amount;
            else if (mask == RESOURCE_STONE) stone += amount;
            else if (mask == RESOURCE_METAL) metal += (long) rawMetalUnitValue(stack) * amount;
            else if (mask == RESOURCE_FOOD) food += (long) rawFoodUnitValue(stack) * amount;
        }
        return new ResourceCounts(wood, stone, metal, food);
    }

    /** Atomic local-container cost using the same value authority as the shared settlement ledger. */
    public static long countCommonMilitaryMetal(Container container) {
        return count(container, SettlementEquipmentUpgradeService::isBlacksmithMetal);
    }

    public static long countMilitaryFood(Container container) {
        return countValue(container, SettlementInventory::militaryFoodValue);
    }

    /** Combat recovery never silently spends gold/diamond or golden apples. */
    public static boolean consumeCommonMilitarySupply(Container container, long commonMetalItems, long foodValue) {
        if (commonMetalItems < 0L || foodValue < 0L) return false;
        if (countCommonMilitaryMetal(container) < commonMetalItems || countMilitaryFood(container) < foodValue) return false;
        consumeMatching(container, commonMetalItems, SettlementEquipmentUpgradeService::isBlacksmithMetal);
        consumeValue(container, foodValue, SettlementInventory::militaryFoodValue);
        container.setChanged();
        return true;
    }

    public static boolean consumeMetalAndFood(Container container, long metal, long food) {
        if (metal < 0L || food < 0L) return false;
        if (countMetal(container) < metal || countFood(container) < food) return false;
        consumeValue(container, metal, SettlementInventory::metalValue);
        consumeValue(container, food, SettlementInventory::foodValue);
        container.setChanged();
        return true;
    }

    public static boolean consume(Container container, long wood, long stone, long food) {
        if (countWood(container) < wood || countStone(container) < stone || countFood(container) < food) return false;
        consumeMatching(container, wood, SettlementInventory::isWood);
        consumeMatching(container, stone, SettlementInventory::isStone);
        consumeValue(container, food, SettlementInventory::foodValue);
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
        return exclusiveResource(stack, RESOURCE_WOOD);
    }

    public static boolean isStone(ItemStack stack) {
        return exclusiveResource(stack, RESOURCE_STONE);
    }

    public static boolean isFood(ItemStack stack) {
        return exclusiveResource(stack, RESOURCE_FOOD);
    }

    /** Metal participates in the same exclusive physical-resource classification as wood/stone/food. */
    public static boolean isMetalResource(ItemStack stack) {
        return exclusiveResource(stack, RESOURCE_METAL);
    }

    /** Physical-resource unit values shared with Survival Ascension. */
    public static int metalValue(ItemStack stack) {
        return resourceMask(stack) == RESOURCE_METAL ? rawMetalUnitValue(stack) : 0;
    }

    public static int foodValue(ItemStack stack) {
        return resourceMask(stack) == RESOURCE_FOOD ? rawFoodUnitValue(stack) : 0;
    }

    private static int militaryFoodValue(ItemStack stack) {
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 0;
        return foodValue(stack);
    }

    private static int rawMetalUnitValue(ItemStack stack) {
        if (stack.is(Items.COPPER_INGOT) || stack.is(Items.RAW_COPPER)) return 1;
        if (stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON)) return 2;
        if (stack.is(Items.GOLD_INGOT) || stack.is(Items.RAW_GOLD)) return 3;
        if (stack.is(Items.DIAMOND)) return 6;
        return 2;
    }

    private static int rawFoodUnitValue(ItemStack stack) {
        if (stack.is(Items.WHEAT)) return 1;
        if (stack.is(Items.GOLDEN_APPLE)) return 12;
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 24;
        var food = stack.get(DataComponents.FOOD);
        if (food != null) return Math.max(1, Math.min(12, food.nutrition()));
        return 1;
    }

    /**
     * A physical stack may fund exactly one settlement resource ledger. Mis-tagged companion/datapack
     * items that match several categories fail closed instead of being counted twice and then only
     * partly removed. Expedition relics and recognized external weapons are never construction,
     * recruitment or upkeep material even if an external datapack accidentally gives them a resource tag.
     */
    private static boolean exclusiveResource(ItemStack stack, int expected) {
        return resourceMask(stack) == expected;
    }

    private static int resourceMask(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ExternalContentTags.EXPEDITION_RELICS)
                || SettlementExternalContentService.isExternalWeapon(stack)) return 0;
        int mask = 0;
        if (rawWood(stack)) mask |= RESOURCE_WOOD;
        if (rawStone(stack)) mask |= RESOURCE_STONE;
        if (rawMetal(stack)) mask |= RESOURCE_METAL;
        if (rawFood(stack)) mask |= RESOURCE_FOOD;
        return mask;
    }

    private static boolean rawWood(ItemStack stack) {
        return stack.is(ItemTags.LOGS)
                || stack.is(ItemTags.PLANKS)
                || stack.is(ExternalContentTags.SETTLEMENT_WOOD);
    }

    private static boolean rawStone(ItemStack stack) {
        return stack.is(Items.STONE)
                || stack.is(Items.COBBLESTONE)
                || stack.is(Items.DEEPSLATE)
                || stack.is(Items.COBBLED_DEEPSLATE)
                || stack.is(Items.ANDESITE)
                || stack.is(Items.DIORITE)
                || stack.is(Items.GRANITE)
                || stack.is(Items.TUFF)
                || stack.is(ExternalContentTags.C_STONES)
                || stack.is(ExternalContentTags.C_COBBLESTONES)
                || stack.is(ExternalContentTags.SETTLEMENT_STONE);
    }

    private static boolean rawMetal(ItemStack stack) {
        return stack.is(Items.IRON_INGOT)
                || stack.is(Items.RAW_IRON)
                || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.RAW_COPPER)
                || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.RAW_GOLD)
                || stack.is(Items.DIAMOND)
                || stack.is(ExternalContentTags.C_INGOTS)
                || stack.is(ExternalContentTags.C_RAW_MATERIALS)
                || stack.is(ExternalContentTags.SETTLEMENT_METAL);
    }

    private static boolean rawFood(ItemStack stack) {
        if (stack.get(DataComponents.FOOD) != null) return true;
        // Settlement food represents staple reserves, not only items the player can eat directly.
        // Wheat is the important bridge: the starter farm can sustain population growth without
        // forcing the player to manually craft every loaf. Common crop staples are grouped with it.
        return stack.is(Items.WHEAT)
                || stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT)
                || stack.is(ExternalContentTags.C_FOODS)
                || stack.is(ExternalContentTags.SETTLEMENT_FOOD);
    }

    private static long count(Container container, java.util.function.Predicate<ItemStack> predicate) {
        long total = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) total += stack.getCount();
        }
        return total;
    }

    private static long countValue(Container container, java.util.function.ToIntFunction<ItemStack> valuation) {
        long total = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            total += (long)Math.max(0, valuation.applyAsInt(stack)) * stack.getCount();
        }
        return total;
    }

    private static void consumeValue(Container container, long amount,
                                     java.util.function.ToIntFunction<ItemStack> valuation) {
        long left = amount;
        for (int unit = 1; unit <= 24 && left > 0L; unit++) {
            for (int slot = 0; slot < container.getContainerSize() && left > 0L; slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || Math.max(0, valuation.applyAsInt(stack)) != unit) continue;
                int take = (int)Math.min(stack.getCount(), (left + unit - 1L) / unit);
                stack.shrink(take);
                left -= (long)take * unit;
            }
        }
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
