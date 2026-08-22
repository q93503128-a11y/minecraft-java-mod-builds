package kr.moonseungjun.survivalascension.equipment;

/*
 * Lightweight rarity + category-specific affix generation follows the core loot/affix separation used by Apotheosis.
 * Copyright (c) 2018-2025 Stormraven Studios, LLC, MIT License.
 * Survival Ascension uses its own compact 26.2 CustomData format, affix set, values and elite-drop integration.
 */

import kr.moonseungjun.survivalascension.elite.EliteMobSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AscensionAffixes {
    private static final String ROOT = "survivalascension_affix";
    private static final String RARITY = "rarity";
    private static final String CATEGORY = "category";
    private static final String PRIMARY = "primary";
    private static final String SCALE = "scale";
    private static final String MASTERY = "mastery";

    private AscensionAffixes() {}

    public static void onEliteDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        int rankId = EliteMobSystem.rankId(mob);
        if (rankId <= 0) return;
        double chance = switch (rankId) { case 1 -> 0.25D; case 2 -> 0.65D; default -> 1.0D; };
        if (level.getRandom().nextDouble() > chance) return;
        ItemStack drop = createEliteDrop(level.getRandom(), rankId);
        level.addFreshEntity(new ItemEntity(level, mob.getX(), mob.getY() + 0.5D, mob.getZ(), drop));
    }

    public static ItemStack createEliteDrop(RandomSource random, int rankId) {
        int rarity = Math.max(1, Math.min(3, rankId));
        Category category = Category.values()[random.nextInt(4)];
        ItemStack stack = new ItemStack(baseItem(category, rarity));
        String vanillaName = stack.getHoverName().getString();

        List<String> pool = new ArrayList<>(List.of(PRIMARY, SCALE, MASTERY));
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        List<String> chosen = pool.subList(0, rarity);

        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            CompoundTag root = new CompoundTag();
            root.putInt(RARITY, rarity);
            root.putString(CATEGORY, category.id);
            root.putBoolean(PRIMARY, chosen.contains(PRIMARY));
            root.putBoolean(SCALE, chosen.contains(SCALE));
            root.putBoolean(MASTERY, chosen.contains(MASTERY));
            tag.put(ROOT, root);
        }));

        String affixes = chosen.stream().map(key -> affixName(category, key)).reduce((a, b) -> a + "·" + b).orElse("");
        String prefix = switch (rarity) {
            case 1 -> "§b[정예] ";
            case 2 -> "§d[승천] ";
            default -> "§6[신화] ";
        };
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(prefix + affixes + " §f" + vanillaName));
        return stack;
    }

    public static double toolSpeedMultiplier(ItemStack stack) {
        int rarity = rarity(stack);
        if (rarity <= 0 || !has(stack, PRIMARY) || category(stack) == Category.WEAPON) return 1.0D;
        return switch (rarity) { case 1 -> 1.12D; case 2 -> 1.25D; default -> 1.40D; };
    }

    public static double damageMultiplier(ItemStack stack) {
        int rarity = rarity(stack);
        if (rarity <= 0 || category(stack) != Category.WEAPON || !has(stack, PRIMARY)) return 1.0D;
        return switch (rarity) { case 1 -> 1.08D; case 2 -> 1.15D; default -> 1.25D; };
    }

    public static double xpMultiplier(ItemStack stack) {
        int rarity = rarity(stack);
        if (rarity <= 0 || !has(stack, MASTERY)) return 1.0D;
        return switch (rarity) { case 1 -> 1.10D; case 2 -> 1.25D; default -> 1.50D; };
    }

    public static int adjustMiningArea(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.PICKAXE || !has(stack, SCALE)) return base;
        return base + scaleAreaBonus(rarity(stack));
    }

    public static int adjustMiningVeinLimit(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.PICKAXE || !has(stack, SCALE)) return base;
        return base + switch (rarity(stack)) { case 1 -> 12; case 2 -> 32; case 3 -> 64; default -> 0; };
    }

    public static int adjustWoodcuttingLimit(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.AXE || !has(stack, SCALE)) return base;
        return base + switch (rarity(stack)) { case 1 -> 16; case 2 -> 48; case 3 -> 96; default -> 0; };
    }

    public static int adjustHarvestArea(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.HOE || !has(stack, SCALE)) return base;
        return base + scaleAreaBonus(rarity(stack));
    }

    public static int adjustCleaveTargets(ItemStack stack, int base) {
        if (base <= 0 || category(stack) != Category.WEAPON || !has(stack, SCALE)) return base;
        return base + switch (rarity(stack)) { case 1 -> 1; case 2 -> 2; case 3 -> 4; default -> 0; };
    }

    public static double adjustCleaveFraction(ItemStack stack, double base) {
        if (base <= 0.0D || category(stack) != Category.WEAPON || !has(stack, SCALE)) return base;
        return Math.min(0.85D, base + switch (rarity(stack)) { case 1 -> 0.05D; case 2 -> 0.10D; case 3 -> 0.15D; default -> 0.0D; });
    }

    public static int rarity(ItemStack stack) {
        CompoundTag root = affixTag(stack);
        return root == null ? 0 : root.getIntOr(RARITY, 0);
    }

    private static boolean has(ItemStack stack, String key) {
        CompoundTag root = affixTag(stack);
        return root != null && root.getBooleanOr(key, false);
    }

    private static Category category(ItemStack stack) {
        CompoundTag root = affixTag(stack);
        if (root == null) return Category.NONE;
        String id = root.getStringOr(CATEGORY, "");
        for (Category category : Category.values()) if (category.id.equals(id)) return category;
        return Category.NONE;
    }

    private static CompoundTag affixTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag root = data.copyTag().getCompoundOrEmpty(ROOT);
        return root.isEmpty() ? null : root;
    }

    private static int scaleAreaBonus(int rarity) {
        return switch (rarity) { case 1, 2 -> 2; case 3 -> 4; default -> 0; };
    }

    private static Item baseItem(Category category, int rarity) {
        return switch (category) {
            case WEAPON -> switch (rarity) { case 1 -> Items.IRON_SWORD; case 2 -> Items.DIAMOND_SWORD; default -> Items.NETHERITE_SWORD; };
            case PICKAXE -> switch (rarity) { case 1 -> Items.IRON_PICKAXE; case 2 -> Items.DIAMOND_PICKAXE; default -> Items.NETHERITE_PICKAXE; };
            case AXE -> switch (rarity) { case 1 -> Items.IRON_AXE; case 2 -> Items.DIAMOND_AXE; default -> Items.NETHERITE_AXE; };
            case HOE -> switch (rarity) { case 1 -> Items.IRON_HOE; case 2 -> Items.DIAMOND_HOE; default -> Items.NETHERITE_HOE; };
            default -> Items.IRON_SWORD;
        };
    }

    private static String affixName(Category category, String key) {
        if (MASTERY.equals(key)) return "숙련";
        if (PRIMARY.equals(key)) return category == Category.WEAPON ? "파괴" : "가속";
        return switch (category) {
            case WEAPON -> "파급";
            case PICKAXE -> "굴착";
            case AXE -> "연쇄";
            case HOE -> "광역";
            default -> "증폭";
        };
    }

    private enum Category {
        WEAPON("weapon"), PICKAXE("pickaxe"), AXE("axe"), HOE("hoe"), NONE("none");
        final String id;
        Category(String id) { this.id = id; }
    }
}
