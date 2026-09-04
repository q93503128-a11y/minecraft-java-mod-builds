package kr.moonseungjun.survivalascension.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SharedEconomyCompat {
    public enum ResourceCategory {
        WOOD("목재"), STONE("석재"), METAL("금속"), FOOD("식량");
        private final String koreanName;
        ResourceCategory(String koreanName) { this.koreanName = koreanName; }
        public String koreanName() { return koreanName; }
    }

    public static final TagKey<Block> SHARED_SUPPLY_DEPOTS = blockTag("shared_supply_depots");
    private static final TagKey<Item> SETTLEMENT_WOOD = itemTag("settlement_wood");
    private static final TagKey<Item> SETTLEMENT_STONE = itemTag("settlement_stone");
    private static final TagKey<Item> SETTLEMENT_METAL = itemTag("settlement_metal");
    private static final TagKey<Item> SETTLEMENT_FOOD = itemTag("settlement_food");
    private static final TagKey<Item> EXPEDITION_RELICS = itemTag("expedition_relics");
    private static final TagKey<Item> C_INGOTS = common("ingots");
    private static final TagKey<Item> C_RAW_MATERIALS = common("raw_materials");
    private static final TagKey<Item> C_STONES = common("stones");
    private static final TagKey<Item> C_COBBLESTONES = common("cobblestones");
    private static final TagKey<Item> C_FOODS = common("foods");

    private static final int WOOD = 1;
    private static final int STONE = 2;
    private static final int METAL = 4;
    private static final int FOOD = 8;

    private SharedEconomyCompat() {}

    public static boolean matches(ResourceCategory category, ItemStack stack) {
        if (stack.isEmpty() || stack.is(EXPEDITION_RELICS) || ContentPackCompatibility.isProtectedEquipment(stack)) return false;
        int mask = 0;
        if (rawWood(stack)) mask |= WOOD;
        if (rawStone(stack)) mask |= STONE;
        if (rawMetal(stack)) mask |= METAL;
        if (rawFood(stack)) mask |= FOOD;
        int expected = switch (category) {
            case WOOD -> WOOD;
            case STONE -> STONE;
            case METAL -> METAL;
            case FOOD -> FOOD;
        };
        return mask == expected;
    }

    public static int resourceValue(ResourceCategory category, ItemStack stack) {
        if (!matches(category, stack)) return 0;
        return switch (category) {
            case WOOD, STONE -> 1;
            case METAL -> metalValue(stack);
            case FOOD -> foodValue(stack);
        };
    }

    private static int metalValue(ItemStack stack) {
        if (stack.is(Items.COPPER_INGOT) || stack.is(Items.RAW_COPPER)) return 1;
        if (stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON)) return 2;
        if (stack.is(Items.GOLD_INGOT) || stack.is(Items.RAW_GOLD)) return 3;
        return 2;
    }

    private static int foodValue(ItemStack stack) {
        if (stack.is(Items.WHEAT)) return 1;
        if (stack.is(Items.GOLDEN_APPLE)) return 12;
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 24;
        var food = stack.get(DataComponents.FOOD);
        if (food != null) return Math.max(1, Math.min(12, food.nutrition()));
        return 1;
    }

    public static boolean isLogisticsContainerBlock(BlockState state) {
        return state.is(Blocks.BARREL) || isSharedSupplyDepot(state);
    }

    public static boolean isSharedSupplyDepot(BlockState state) {
        return state.is(SHARED_SUPPLY_DEPOTS);
    }

    private static boolean rawWood(ItemStack stack) {
        return stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(SETTLEMENT_WOOD);
    }

    private static boolean rawStone(ItemStack stack) {
        return stack.is(Items.STONE) || stack.is(Items.COBBLESTONE) || stack.is(Items.DEEPSLATE)
                || stack.is(Items.COBBLED_DEEPSLATE) || stack.is(Items.ANDESITE) || stack.is(Items.DIORITE)
                || stack.is(Items.GRANITE) || stack.is(Items.TUFF) || stack.is(C_STONES)
                || stack.is(C_COBBLESTONES) || stack.is(SETTLEMENT_STONE);
    }

    private static boolean rawMetal(ItemStack stack) {
        return stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON) || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.RAW_COPPER) || stack.is(Items.GOLD_INGOT) || stack.is(Items.RAW_GOLD)
                || stack.is(C_INGOTS) || stack.is(C_RAW_MATERIALS) || stack.is(SETTLEMENT_METAL);
    }

    private static boolean rawFood(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null || stack.is(Items.WHEAT) || stack.is(Items.CARROT)
                || stack.is(Items.POTATO) || stack.is(Items.BEETROOT) || stack.is(C_FOODS) || stack.is(SETTLEMENT_FOOD);
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("frontier_settlement", path));
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("frontier_settlement", path));
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}
