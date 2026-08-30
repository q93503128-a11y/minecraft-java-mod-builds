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
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class AscensionAffixes {
    private static final String ROOT = "survivalascension_affix";
    private static final String RARITY = "rarity";
    private static final String CATEGORY = "category";
    private static final String BASE_NAME = "base_name";
    private static final String PRIMARY = "primary";
    private static final String SCALE = "scale";
    private static final String MASTERY = "mastery";
    private static final String SECONDARY = "secondary";
    private static final String UTILITY = "utility";
    private static final String AWAKENED = "awakened";
    private static final String RANGED_PROJECTILE = "survivalascension_ranged_projectile";
    private static final String RANGED_OWNER = "survivalascension_ranged_owner";
    private static final String RANGED_PRECISION = "survivalascension_ranged_precision";
    private static final String RANGED_DAMAGE_PERMILLE = "survivalascension_ranged_damage_permille";
    private static final String RANGED_XP_PERMILLE = "survivalascension_ranged_xp_permille";
    private static final String RANGED_RADIUS_TENTHS = "survivalascension_ranged_radius_tenths";
    private static final String RANGED_TARGET_BONUS = "survivalascension_ranged_target_bonus";
    private static final String RANGED_FRACTION_PERMILLE = "survivalascension_ranged_fraction_permille";
    private static final List<String> AFFIX_POOL = List.of(PRIMARY, SCALE, MASTERY, SECONDARY, UTILITY);
    private static final List<Category> GEAR_CATEGORIES = List.of(Category.WEAPON, Category.RANGED, Category.PICKAXE, Category.AXE, Category.SHOVEL, Category.HOE, Category.SHIELD, Category.ARMOR);
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private AscensionAffixes() {}

    public static void onEliteDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                ? sourcePlayer : rangedProjectileOwner(event.getSource().getDirectEntity(), level);
        if (killer == null) return;
        int rankId = EliteMobSystem.rankId(mob);
        if (rankId <= 0) return;
        double chance = switch (rankId) { case 1 -> 0.25D; case 2 -> 0.65D; default -> 1.0D; };
        if (level.getRandom().nextDouble() > chance) return;
        ItemStack drop = createEliteDrop(level.getRandom(), rankId);
        level.addFreshEntity(new ItemEntity(level, mob.getX(), mob.getY() + 0.5D, mob.getZ(), drop));
    }

    public static ItemStack createEliteDrop(RandomSource random, int rankId) {
        int rarity = Math.max(1, Math.min(3, rankId));
        Category category = GEAR_CATEGORIES.get(random.nextInt(GEAR_CATEGORIES.size()));
        ItemStack stack = new ItemStack(baseItem(category, rarity, random));
        rollAffixes(stack, random, rarity, category, false);
        return stack;
    }

    /**
     * Content-pack bridge: any single-stack sword/spear/mace/bow/crossbow/pickaxe/axe/shovel/hoe/shield or humanoid armor item that
     * participates in standard Minecraft/NeoForge item tags can receive Survival Ascension affixes without
     * linking the optional mod's Java classes. Existing item components remain intact; only our nested
     * CustomData key and display name are changed.
     */
    public static boolean canImprint(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1 && rarity(stack) <= 0 && categoryForItem(stack) != Category.NONE;
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return categoryForItem(stack) == Category.RANGED;
    }

    public static String imprintCategoryName(ItemStack stack) {
        return switch (categoryForItem(stack)) {
            case WEAPON -> "무기";
            case SPEAR -> "스피어";
            case RANGED -> "원거리";
            case PICKAXE -> "곡괭이";
            case AXE -> "도끼";
            case SHOVEL -> "삽";
            case HOE -> "괭이";
            case MACE -> "메이스";
            case SHIELD -> "방패";
            case ARMOR -> "방어구";
            default -> "대상 아님";
        };
    }

    public static boolean imprint(ItemStack stack, RandomSource random, int requestedRarity) {
        if (!canImprint(stack)) return false;
        Category category = categoryForItem(stack);
        int rarity = Math.max(1, Math.min(3, requestedRarity));
        rollAffixes(stack, random, rarity, category, false);
        return true;
    }

    public static boolean reroll(ItemStack stack, RandomSource random) {
        int rarity = rarity(stack);
        Category category = category(stack);
        if (rarity <= 0 || category == Category.NONE) return false;
        boolean awakened = isAwakened(stack);
        rollAffixes(stack, random, awakened ? 4 : rarity, category, awakened);
        return true;
    }

    public static boolean canAwaken(ItemStack stack) {
        return rarity(stack) == 3
                && !isAwakened(stack)
                && category(stack) != Category.NONE
                && currentAffixes(stack).size() == 3;
    }

    public static boolean awaken(ItemStack stack, RandomSource random) {
        if (!canAwaken(stack)) return false;
        Category category = category(stack);
        List<String> chosen = currentAffixes(stack);
        List<String> missing = new ArrayList<>();
        for (String key : AFFIX_POOL) if (!chosen.contains(key)) missing.add(key);
        if (missing.size() != 2) return false;
        chosen.add(missing.get(random.nextInt(missing.size())));
        writeAffixes(stack, 3, category, chosen, true);
        return true;
    }

    private static void rollAffixes(ItemStack stack, RandomSource random, int count, Category category, boolean awakened) {
        List<String> pool = new ArrayList<>(AFFIX_POOL);
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        int affixCount = Math.max(1, Math.min(count, pool.size()));
        List<String> chosen = new ArrayList<>(pool.subList(0, affixCount));
        writeAffixes(stack, Math.max(1, Math.min(3, count)), category, chosen, awakened);
    }

    private static void writeAffixes(ItemStack stack, int rarity, Category category, List<String> chosen, boolean awakened) {
        String baseName = baseName(stack);
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            CompoundTag root = new CompoundTag();
            root.putInt(RARITY, rarity);
            root.putString(CATEGORY, category.id);
            root.putString(BASE_NAME, baseName);
            root.putBoolean(AWAKENED, awakened);
            for (String key : AFFIX_POOL) root.putBoolean(key, chosen.contains(key));
            tag.put(ROOT, root);
        }));
        updateDisplayName(stack, rarity, category, chosen, awakened, baseName);
    }

    private static void updateDisplayName(ItemStack stack, int rarity, Category category, List<String> chosen, boolean awakened, String baseName) {
        String affixes = chosen.stream().map(key -> affixName(category, key)).reduce((a, b) -> a + "·" + b).orElse("");
        String prefix = awakened ? "§5[각성 신화] " : switch (rarity) {
            case 1 -> "§b[정예] ";
            case 2 -> "§d[승천] ";
            default -> "§6[신화] ";
        };
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(prefix + affixes + " §f" + baseName));
    }

    public static String effectSummary(ItemStack stack) {
        int rarity = rarity(stack);
        Category category = category(stack);
        if (rarity <= 0 || category == Category.NONE) return "승천 옵션 없음";
        List<String> out = new ArrayList<>();
        for (String key : currentAffixes(stack)) out.add(affixName(category, key) + " " + effectText(category, key, rarity));
        return String.join(" · ", out);
    }

    private static String tier(int rarity, String one, String two, String three) {
        return rarity <= 1 ? one : rarity == 2 ? two : three;
    }

    private static String effectText(Category category, String key, int rarity) {
        if (MASTERY.equals(key)) {
            if (category == Category.SHIELD) return "파동 쿨 -" + tier(rarity, "4", "8", "12") + "틱";
            if (category == Category.ARMOR) return "전투 XP +" + tier(rarity, "8", "12", "20") + "%";
            return "숙련 XP ×" + tier(rarity, "1.5", "2.0", "3.0");
        }
        if (PRIMARY.equals(key)) return switch (category) {
            case WEAPON -> "직접 피해 ×" + tier(rarity, "1.40", "1.75", "2.20");
            case SPEAR -> "직접 피해 ×" + tier(rarity, "1.35", "1.65", "2.00");
            case RANGED -> "직접 피해 ×" + tier(rarity, "1.40", "1.80", "2.40");
            case MACE, SHIELD -> "밀치기 +" + tier(rarity, "0.25", "0.50", "0.75");
            case ARMOR -> "상시 피해 -" + tier(rarity, "5", "7", "10") + "%";
            case PICKAXE, AXE, SHOVEL, HOE -> "작업 속도 ×" + tier(rarity, "1.60", "2.00", "2.60");
            default -> "강화";
        };
        if (SCALE.equals(key)) return switch (category) {
            case WEAPON -> "광역 대상 +" + tier(rarity, "4", "8", "14");
            case SPEAR -> "돌파 거리 +" + tier(rarity, "2", "4", "6") + "블록";
            case RANGED -> "파급 반경 +" + tier(rarity, "1.5", "3", "5") + "블록";
            case PICKAXE, SHOVEL, HOE -> "작업 폭 +" + tier(rarity, "4", "6", "8");
            case AXE -> "연쇄 벌목 +" + tier(rarity, "64", "192", "384");
            case MACE, SHIELD -> "영향 반경 +" + tier(rarity, "2", "4", "6") + "블록";
            case ARMOR -> "체력 절반 이하 추가 -" + tier(rarity, "4", "6", "8") + "%";
            default -> "범위 강화";
        };
        if (SECONDARY.equals(key)) return switch (category) {
            case WEAPON -> "정예 피해 ×" + tier(rarity, "1.40", "1.80", "2.50");
            case SPEAR -> "돌파 대상 +" + tier(rarity, "4", "8", "12");
            case RANGED -> "파급 대상 +" + tier(rarity, "2", "5", "10");
            case PICKAXE -> "광맥 한도 +" + tier(rarity, "48", "128", "256");
            case AXE -> "추가 벌채 +" + tier(rarity, "32", "96", "256");
            case SHOVEL, HOE -> "작업 폭 추가 +" + tier(rarity, "4", "6", "8");
            case MACE -> "충격 대상 +" + tier(rarity, "8", "16", "24");
            case SHIELD -> "파동 대상 +" + tier(rarity, "4", "8", "14");
            case ARMOR -> "큰 피해 추가 -" + tier(rarity, "4", "6", "9") + "%";
            default -> "특화 강화";
        };
        return switch (category) {
            case WEAPON, RANGED -> "광역 피해 +" + tier(rarity, "15", "30", "50") + "%p";
            case SPEAR -> "밀치기 +" + tier(rarity, "0.20", "0.40", "0.60");
            case PICKAXE, AXE, SHOVEL, HOE -> "추가 작업 속도 ×" + tier(rarity, "1.25", "1.45", "1.75");
            case MACE, SHIELD -> "띄우기 +" + tier(rarity, "0.10", "0.20", "0.30");
            case ARMOR -> "환경 피해 추가 -" + tier(rarity, "6", "9", "12") + "%";
            default -> "보조 강화";
        };
    }

    private static String baseName(ItemStack stack) {
        CompoundTag root = affixTag(stack);
        if (root != null) {
            String stored = root.getStringOr(BASE_NAME, "");
            if (!stored.isBlank()) return stored;
        }
        return stack.getHoverName().getString();
    }

    public static double toolSpeedMultiplier(ItemStack stack) {
        int rarity = rarity(stack);
        Category category = category(stack);
        if (rarity <= 0 || category == Category.WEAPON || category == Category.SPEAR || category == Category.RANGED || category == Category.MACE || category == Category.SHIELD || category == Category.ARMOR || category == Category.NONE) return 1.0D;
        double result = 1.0D;
        if (has(stack, PRIMARY)) result *= switch (rarity) { case 1 -> 1.60D; case 2 -> 2.00D; default -> 2.60D; };
        if (has(stack, UTILITY)) result *= switch (rarity) { case 1 -> 1.25D; case 2 -> 1.45D; default -> 1.75D; };
        return result;
    }

    public static double damageMultiplier(ItemStack stack) {
        int rarity = rarity(stack);
        Category category = category(stack);
        if (rarity <= 0 || !has(stack, PRIMARY)) return 1.0D;
        if (category == Category.WEAPON) return switch (rarity) { case 1 -> 1.40D; case 2 -> 1.75D; default -> 2.20D; };
        if (category == Category.SPEAR) return switch (rarity) { case 1 -> 1.35D; case 2 -> 1.65D; default -> 2.00D; };
        return 1.0D;
    }

    public static double xpMultiplier(ItemStack stack) {
        int rarity = rarity(stack);
        Category category = category(stack);
        if (rarity <= 0 || category == Category.ARMOR || category == Category.RANGED || category == Category.SHIELD || !has(stack, MASTERY)) return 1.0D;
        return switch (rarity) { case 1 -> 1.50D; case 2 -> 2.00D; default -> 3.00D; };
    }

    public static void snapshotRangedProjectile(Projectile projectile, ServerPlayer player, ItemStack weapon, boolean precision) {
        if (!isRangedWeapon(weapon)) return;
        CompoundTag data = projectile.getPersistentData();
        data.putBoolean(RANGED_PROJECTILE, true);
        data.putString(RANGED_OWNER, player.getUUID().toString());
        data.putBoolean(RANGED_PRECISION, precision);
        int rarity = rarity(weapon);
        int damage = 1000;
        int xp = 1000;
        int radiusTenths = 0;
        int targets = 0;
        int fraction = 0;
        if (rarity > 0) {
            if (has(weapon, PRIMARY)) damage = switch (rarity) { case 1 -> 1400; case 2 -> 1800; default -> 2400; };
            if (has(weapon, MASTERY)) xp = switch (rarity) { case 1 -> 1500; case 2 -> 2000; default -> 3000; };
            if (has(weapon, SCALE)) radiusTenths = switch (rarity) { case 1 -> 15; case 2 -> 30; default -> 50; };
            if (has(weapon, SECONDARY)) targets = switch (rarity) { case 1 -> 2; case 2 -> 5; default -> 10; };
            if (has(weapon, UTILITY)) fraction = switch (rarity) { case 1 -> 150; case 2 -> 300; default -> 500; };
        }
        data.putInt(RANGED_DAMAGE_PERMILLE, damage);
        data.putInt(RANGED_XP_PERMILLE, xp);
        data.putInt(RANGED_RADIUS_TENTHS, radiusTenths);
        data.putInt(RANGED_TARGET_BONUS, targets);
        data.putInt(RANGED_FRACTION_PERMILLE, fraction);
    }

    public static boolean isRangedProjectile(Entity direct) {
        return direct != null && direct.getPersistentData().getBooleanOr(RANGED_PROJECTILE, false);
    }

    public static ServerPlayer rangedProjectileOwner(Entity direct, ServerLevel level) {
        if (!isRangedProjectile(direct)) return null;
        String raw = direct.getPersistentData().getStringOr(RANGED_OWNER, "");
        if (raw.isBlank()) return null;
        try {
            return level.getServer().getPlayerList().getPlayer(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean isPrecisionRangedProjectile(Entity direct) {
        return isRangedProjectile(direct) && direct.getPersistentData().getBooleanOr(RANGED_PRECISION, false);
    }

    public static double projectileDamageMultiplier(Entity direct) {
        if (!isRangedProjectile(direct)) return 1.0D;
        return Math.min(2.40D, Math.max(1.0D, direct.getPersistentData().getIntOr(RANGED_DAMAGE_PERMILLE, 1000) / 1000.0D));
    }

    public static double projectileXpMultiplier(Entity direct) {
        if (!isRangedProjectile(direct)) return 1.0D;
        return Math.min(3.00D, Math.max(1.0D, direct.getPersistentData().getIntOr(RANGED_XP_PERMILLE, 1000) / 1000.0D));
    }

    public static double projectileBurstRadiusBonus(Entity direct) {
        if (!isRangedProjectile(direct)) return 0.0D;
        return Math.min(5.0D, Math.max(0, direct.getPersistentData().getIntOr(RANGED_RADIUS_TENTHS, 0)) / 10.0D);
    }

    public static int projectileBurstTargetBonus(Entity direct) {
        if (!isRangedProjectile(direct)) return 0;
        return Math.min(10, Math.max(0, direct.getPersistentData().getIntOr(RANGED_TARGET_BONUS, 0)));
    }

    public static double projectileBurstFractionBonus(Entity direct) {
        if (!isRangedProjectile(direct)) return 0.0D;
        return Math.min(0.50D, Math.max(0, direct.getPersistentData().getIntOr(RANGED_FRACTION_PERMILLE, 0)) / 1000.0D);
    }

    public static double armorDamageMultiplier(ServerPlayer player, float incomingAmount, boolean environmental) {
        double reduction = 0.0D;
        boolean lowHealth = player.getHealth() <= player.getMaxHealth() * 0.50F;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (category(armor) != Category.ARMOR) continue;
            int rarity = rarity(armor);
            if (rarity <= 0) continue;
            if (has(armor, PRIMARY)) reduction += switch (rarity) { case 1 -> 0.05D; case 2 -> 0.07D; default -> 0.10D; };
            if (lowHealth && has(armor, SCALE)) reduction += switch (rarity) { case 1 -> 0.04D; case 2 -> 0.06D; default -> 0.08D; };
            if (incomingAmount >= 8.0F && has(armor, SECONDARY)) reduction += switch (rarity) { case 1 -> 0.04D; case 2 -> 0.06D; default -> 0.09D; };
            if (environmental && has(armor, UTILITY)) reduction += switch (rarity) { case 1 -> 0.06D; case 2 -> 0.09D; default -> 0.12D; };
        }
        return 1.0D - Math.min(0.70D, reduction);
    }

    public static double armorXpMultiplier(ServerPlayer player) {
        double bonus = 0.0D;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (category(armor) != Category.ARMOR || !has(armor, MASTERY)) continue;
            bonus += switch (rarity(armor)) { case 1 -> 0.08D; case 2 -> 0.12D; case 3 -> 0.20D; default -> 0.0D; };
        }
        return Math.min(2.00D, 1.0D + bonus);
    }

    public static int adjustMiningArea(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.PICKAXE || !has(stack, SCALE)) return base;
        return base + scaleAreaBonus(rarity(stack));
    }

    public static int adjustShovelArea(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.SHOVEL) return base;
        int bonus = 0;
        if (has(stack, SCALE)) bonus += scaleAreaBonus(rarity(stack));
        if (has(stack, SECONDARY)) bonus += switch (rarity(stack)) { case 1 -> 4; case 2 -> 6; case 3 -> 8; default -> 0; };
        return Math.min(21, base + bonus);
    }

    public static int adjustMiningVeinLimit(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.PICKAXE || !has(stack, SECONDARY)) return base;
        return base + switch (rarity(stack)) { case 1 -> 48; case 2 -> 128; case 3 -> 256; default -> 0; };
    }

    public static int adjustWoodcuttingLimit(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.AXE) return base;
        int bonus = 0;
        if (has(stack, SCALE)) bonus += switch (rarity(stack)) { case 1 -> 64; case 2 -> 192; case 3 -> 384; default -> 0; };
        if (has(stack, SECONDARY)) bonus += switch (rarity(stack)) { case 1 -> 32; case 2 -> 96; case 3 -> 256; default -> 0; };
        return base + bonus;
    }

    public static int adjustHarvestArea(ItemStack stack, int base) {
        if (base <= 1 || category(stack) != Category.HOE) return base;
        int bonus = 0;
        if (has(stack, SCALE)) bonus += scaleAreaBonus(rarity(stack));
        if (has(stack, SECONDARY)) bonus += switch (rarity(stack)) { case 1 -> 4; case 2 -> 6; case 3 -> 8; default -> 0; };
        return base + bonus;
    }

    public static int adjustCleaveTargets(ItemStack stack, int base) {
        if (base <= 0 || category(stack) != Category.WEAPON || !has(stack, SCALE)) return base;
        return base + switch (rarity(stack)) { case 1 -> 4; case 2 -> 8; case 3 -> 14; default -> 0; };
    }

    public static double adjustCleaveFraction(ItemStack stack, double base) {
        if (base <= 0.0D || category(stack) != Category.WEAPON || !has(stack, UTILITY)) return base;
        return Math.min(1.25D, base + switch (rarity(stack)) { case 1 -> 0.15D; case 2 -> 0.30D; case 3 -> 0.50D; default -> 0.0D; });
    }

    public static boolean isSpear(ItemStack stack) {
        return categoryForItem(stack) == Category.SPEAR;
    }

    public static double spearLineReachBonus(ItemStack stack) {
        if (category(stack) != Category.SPEAR || !has(stack, SCALE)) return 0.0D;
        return switch (rarity(stack)) { case 1 -> 2.0D; case 2 -> 4.0D; case 3 -> 6.0D; default -> 0.0D; };
    }

    public static int spearLineTargetBonus(ItemStack stack) {
        if (category(stack) != Category.SPEAR || !has(stack, SECONDARY)) return 0;
        return switch (rarity(stack)) { case 1 -> 4; case 2 -> 8; case 3 -> 12; default -> 0; };
    }

    public static double spearLineKnockbackBonus(ItemStack stack) {
        if (category(stack) != Category.SPEAR || !has(stack, UTILITY)) return 0.0D;
        return switch (rarity(stack)) { case 1 -> 0.20D; case 2 -> 0.40D; case 3 -> 0.60D; default -> 0.0D; };
    }

    public static boolean isMace(ItemStack stack) {
        return categoryForItem(stack) == Category.MACE;
    }

    public static double maceImpactRadiusBonus(ItemStack stack) {
        if (category(stack) != Category.MACE || !has(stack, SCALE)) return 0.0D;
        return switch (rarity(stack)) { case 1 -> 2.0D; case 2 -> 4.0D; case 3 -> 6.0D; default -> 0.0D; };
    }

    public static int maceImpactTargetBonus(ItemStack stack) {
        if (category(stack) != Category.MACE || !has(stack, SECONDARY)) return 0;
        return switch (rarity(stack)) { case 1 -> 8; case 2 -> 16; case 3 -> 24; default -> 0; };
    }

    public static double maceImpactKnockbackBonus(ItemStack stack) {
        if (category(stack) != Category.MACE || !has(stack, PRIMARY)) return 0.0D;
        return switch (rarity(stack)) { case 1 -> 0.25D; case 2 -> 0.50D; case 3 -> 0.75D; default -> 0.0D; };
    }

    public static double maceImpactLiftBonus(ItemStack stack) {
        if (category(stack) != Category.MACE || !has(stack, UTILITY)) return 0.0D;
        return switch (rarity(stack)) { case 1 -> 0.10D; case 2 -> 0.20D; case 3 -> 0.30D; default -> 0.0D; };
    }

    public static boolean isShield(ItemStack stack) {
    return categoryForItem(stack) == Category.SHIELD;
}

public static double shieldWaveRadiusBonus(ItemStack stack) {
    if (category(stack) != Category.SHIELD || !has(stack, SCALE)) return 0.0D;
    return switch (rarity(stack)) { case 1 -> 2.0D; case 2 -> 4.0D; case 3 -> 6.0D; default -> 0.0D; };
}

public static int shieldWaveTargetBonus(ItemStack stack) {
    if (category(stack) != Category.SHIELD || !has(stack, SECONDARY)) return 0;
    return switch (rarity(stack)) { case 1 -> 4; case 2 -> 8; case 3 -> 14; default -> 0; };
}

public static double shieldWaveKnockbackBonus(ItemStack stack) {
    if (category(stack) != Category.SHIELD || !has(stack, PRIMARY)) return 0.0D;
    return switch (rarity(stack)) { case 1 -> 0.25D; case 2 -> 0.50D; case 3 -> 0.75D; default -> 0.0D; };
}

public static int shieldWaveCooldownReduction(ItemStack stack) {
    if (category(stack) != Category.SHIELD || !has(stack, MASTERY)) return 0;
    return switch (rarity(stack)) { case 1 -> 4; case 2 -> 8; case 3 -> 12; default -> 0; };
}

public static double shieldWaveLiftBonus(ItemStack stack) {
    if (category(stack) != Category.SHIELD || !has(stack, UTILITY)) return 0.0D;
    return switch (rarity(stack)) { case 1 -> 0.10D; case 2 -> 0.20D; case 3 -> 0.30D; default -> 0.0D; };
}

    public static double eliteDamageMultiplier(ItemStack stack) {
        int rarity = rarity(stack);
        if (rarity <= 0 || category(stack) != Category.WEAPON || !has(stack, SECONDARY)) return 1.0D;
        return switch (rarity) { case 1 -> 1.40D; case 2 -> 1.80D; default -> 2.50D; };
    }

    public static boolean isAffixGear(ItemStack stack) { return rarity(stack) > 0; }

    public static boolean isAwakened(ItemStack stack) {
        CompoundTag root = affixTag(stack);
        return root != null && root.getIntOr(RARITY, 0) == 3 && root.getBooleanOr(AWAKENED, false);
    }

    public static String rarityName(ItemStack stack) {
        if (isAwakened(stack)) return "각성 신화";
        return switch (rarity(stack)) { case 1 -> "정예"; case 2 -> "승천"; case 3 -> "신화"; default -> "일반"; };
    }

    public static String affixSummary(ItemStack stack) {
        Category category = category(stack);
        if (category == Category.NONE) return "affix 없음";
        List<String> names = new ArrayList<>();
        for (String key : AFFIX_POOL) if (has(stack, key)) names.add(affixName(category, key));
        return names.isEmpty() ? "affix 없음" : String.join(" · ", names);
    }

    public static int rarity(ItemStack stack) {
        CompoundTag root = affixTag(stack);
        return root == null ? 0 : root.getIntOr(RARITY, 0);
    }

    private static List<String> currentAffixes(ItemStack stack) {
        List<String> chosen = new ArrayList<>();
        for (String key : AFFIX_POOL) if (has(stack, key)) chosen.add(key);
        return chosen;
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

    private static Category categoryForItem(ItemStack stack) {
        if (stack.is(ItemTags.SPEARS)) return Category.SPEAR;
        if (stack.is(ItemTags.SWORDS)) return Category.WEAPON;
        if (stack.is(Tags.Items.TOOLS_BOW) || stack.is(Tags.Items.TOOLS_CROSSBOW)) return Category.RANGED;
        if (stack.is(ItemTags.PICKAXES)) return Category.PICKAXE;
        if (stack.is(ItemTags.AXES)) return Category.AXE;
        if (stack.is(ItemTags.SHOVELS)) return Category.SHOVEL;
        if (stack.is(ItemTags.HOES)) return Category.HOE;
        if (stack.is(Tags.Items.TOOLS_MACE)) return Category.MACE;
        if (stack.is(Tags.Items.TOOLS_SHIELD)) return Category.SHIELD;
        if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
                || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR)) return Category.ARMOR;
        return Category.NONE;
    }

    private static CompoundTag affixTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag root = data.copyTag().getCompoundOrEmpty(ROOT);
        return root.isEmpty() ? null : root;
    }

    private static int scaleAreaBonus(int rarity) {
        return switch (rarity) { case 1 -> 4; case 2 -> 6; case 3 -> 8; default -> 0; };
    }

    private static Item baseItem(Category category, int rarity, RandomSource random) {
        return switch (category) {
            case WEAPON -> switch (rarity) { case 1 -> Items.IRON_SWORD; case 2 -> Items.DIAMOND_SWORD; default -> Items.NETHERITE_SWORD; };
            case RANGED -> random.nextBoolean() ? Items.BOW : Items.CROSSBOW;
            case PICKAXE -> switch (rarity) { case 1 -> Items.IRON_PICKAXE; case 2 -> Items.DIAMOND_PICKAXE; default -> Items.NETHERITE_PICKAXE; };
            case AXE -> switch (rarity) { case 1 -> Items.IRON_AXE; case 2 -> Items.DIAMOND_AXE; default -> Items.NETHERITE_AXE; };
            case SHOVEL -> switch (rarity) { case 1 -> Items.IRON_SHOVEL; case 2 -> Items.DIAMOND_SHOVEL; default -> Items.NETHERITE_SHOVEL; };
            case HOE -> switch (rarity) { case 1 -> Items.IRON_HOE; case 2 -> Items.DIAMOND_HOE; default -> Items.NETHERITE_HOE; };
            case MACE -> Items.MACE;
            case SHIELD -> Items.SHIELD;
            case ARMOR -> switch (random.nextInt(4)) {
                case 0 -> switch (rarity) { case 1 -> Items.IRON_HELMET; case 2 -> Items.DIAMOND_HELMET; default -> Items.NETHERITE_HELMET; };
                case 1 -> switch (rarity) { case 1 -> Items.IRON_CHESTPLATE; case 2 -> Items.DIAMOND_CHESTPLATE; default -> Items.NETHERITE_CHESTPLATE; };
                case 2 -> switch (rarity) { case 1 -> Items.IRON_LEGGINGS; case 2 -> Items.DIAMOND_LEGGINGS; default -> Items.NETHERITE_LEGGINGS; };
                default -> switch (rarity) { case 1 -> Items.IRON_BOOTS; case 2 -> Items.DIAMOND_BOOTS; default -> Items.NETHERITE_BOOTS; };
            };
            default -> Items.IRON_SWORD;
        };
    }

    private static String affixName(Category category, String key) {
        if (MASTERY.equals(key)) return category == Category.SHIELD ? "대응" : "숙련";
        if (PRIMARY.equals(key)) return category == Category.WEAPON ? "파괴" : category == Category.SPEAR ? "관통" : category == Category.RANGED ? "강궁" : category == Category.MACE ? "충각" : category == Category.SHIELD ? "압력" : category == Category.ARMOR ? "수호" : "가속";
        if (SCALE.equals(key)) return switch (category) {
            case WEAPON -> "파급"; case SPEAR -> "돌파"; case RANGED -> "산개"; case PICKAXE -> "굴착"; case AXE -> "연쇄"; case SHOVEL -> "토공"; case HOE -> "광역"; case MACE -> "진동"; case SHIELD -> "파동"; case ARMOR -> "불굴"; default -> "증폭";
        };
        if (SECONDARY.equals(key)) return switch (category) {
            case WEAPON -> "사냥"; case SPEAR -> "대열"; case RANGED -> "연쇄"; case PICKAXE -> "광맥"; case AXE -> "벌채"; case SHOVEL -> "개착"; case HOE -> "풍작"; case MACE -> "분쇄"; case SHIELD -> "진압"; case ARMOR -> "완강"; default -> "특화";
        };
        return switch (category) {
            case WEAPON -> "충격"; case SPEAR -> "충압"; case RANGED -> "충격"; case PICKAXE, AXE, SHOVEL, HOE -> "정교"; case MACE -> "격퇴"; case SHIELD -> "반동"; case ARMOR -> "보호"; default -> "보조";
        };
    }

    private enum Category {
        WEAPON("weapon"), SPEAR("spear"), RANGED("ranged"), PICKAXE("pickaxe"), AXE("axe"), SHOVEL("shovel"), HOE("hoe"), MACE("mace"), SHIELD("shield"), ARMOR("armor"), NONE("none");
        final String id;
        Category(String id) { this.id = id; }
    }
}
