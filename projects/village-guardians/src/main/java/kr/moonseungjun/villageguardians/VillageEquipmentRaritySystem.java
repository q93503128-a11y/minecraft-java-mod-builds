package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Curated raid equipment drops and cost-free two-to-one rarity fusion at the smithy. */
public final class VillageEquipmentRaritySystem {
    private static final int MAIN_INVENTORY_SLOTS = 36;
    private static final List<Item> EARLY_ITEMS = List.of(
            Items.IRON_SWORD, Items.BOW, Items.SHIELD, Items.IRON_CHESTPLATE, Items.CROSSBOW);
    private static final List<Item> LATE_ITEMS = List.of(
            Items.DIAMOND_SWORD, Items.BOW, Items.SHIELD, Items.DIAMOND_CHESTPLATE, Items.CROSSBOW,
            Items.DIAMOND_AXE, Items.DIAMOND_HELMET);

    private VillageEquipmentRaritySystem() {}

    public static ItemStack createRaidDrop(int day, boolean boss, RandomSource random) {
        List<Item> pool = day >= 6 ? LATE_ITEMS : EARLY_ITEMS;
        Item item = pool.get(random.nextInt(pool.size()));
        Rarity rarity = rollRarity(day, boss, random);
        return create(item, rarity);
    }

    public static String combineFirstPair(ServerPlayer player) {
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        for (int firstSlot = 0; firstSlot < limit; firstSlot++) {
            ItemStack first = player.getInventory().getItem(firstSlot);
            Rarity rarity = rarityOf(first);
            if (rarity == null || rarity == Rarity.LEGENDARY) continue;
            for (int secondSlot = firstSlot + 1; secondSlot < limit; secondSlot++) {
                ItemStack second = player.getInventory().getItem(secondSlot);
                if (second.getItem() != first.getItem() || rarityOf(second) != rarity) continue;
                first.shrink(1);
                second.shrink(1);
                ItemStack result = create(first.getItem(), rarity.next());
                if (!player.addItem(result)) player.drop(result, false);
                player.getInventory().setChanged();
                return displayName(first.getItem()) + " 두 개를 " + rarity.next().displayName()
                        + " 등급으로 합성했습니다. 재화는 소모되지 않았습니다.";
            }
        }
        return "같은 종류·같은 등급의 습격 장비 두 개가 필요합니다.";
    }

    public static float meleeMultiplier(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isMelee(stack.getItem())) return 1.0f;
        return 1.0f + rarity.powerStep() * 0.055f;
    }

    public static float projectileMultiplier(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isProjectile(stack.getItem())) return 1.0f;
        return 1.0f + rarity.powerStep() * 0.055f;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float reduction = rarityReduction(player.getOffhandItem());
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            reduction += rarityReduction(player.getItemBySlot(slot));
        }
        return Math.max(0.72f, 1.0f - reduction);
    }

    public static float skillMultiplier(ServerPlayer player) {
        int step = Math.max(rarityStep(player.getMainHandItem()), rarityStep(player.getOffhandItem()));
        return 1.0f + step * 0.035f;
    }

    public static Rarity rarityOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) return null;
        String plain = ChatFormatting.stripFormatting(name.getString());
        for (Rarity rarity : Rarity.values()) {
            if (plain.startsWith("[" + rarity.displayName() + "] ")) return rarity;
        }
        return null;
    }

    private static ItemStack create(Item item, Rarity rarity) {
        ItemStack stack = item.getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("[" + rarity.displayName() + "] " + displayName(item))
                        .withStyle(rarity.formatting()));
        return stack;
    }

    private static Rarity rollRarity(int day, boolean boss, RandomSource random) {
        int roll = random.nextInt(1000) + Math.min(260, Math.max(0, day - 1) * 18) + (boss ? 260 : 0);
        if (roll >= 1080) return Rarity.LEGENDARY;
        if (roll >= 880) return Rarity.EPIC;
        if (roll >= 610) return Rarity.RARE;
        if (roll >= 330) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }

    private static float rarityReduction(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !(isArmor(stack.getItem()) || stack.getItem() == Items.SHIELD)) return 0.0f;
        return rarity.powerStep() * 0.012f;
    }

    private static int rarityStep(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        return rarity == null ? 0 : rarity.powerStep();
    }

    private static boolean isMelee(Item item) {
        return item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD
                || item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE;
    }

    private static boolean isProjectile(Item item) {
        return item == Items.BOW || item == Items.CROSSBOW || item == Items.TRIDENT;
    }

    private static boolean isArmor(Item item) {
        return item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE || item == Items.IRON_LEGGINGS
                || item == Items.IRON_BOOTS || item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE
                || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_HELMET
                || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS;
    }

    private static String displayName(Item item) {
        if (item == Items.BOW) return "수호 장궁";
        if (item == Items.CROSSBOW) return "수호 쇠뇌";
        if (item == Items.SHIELD) return "수호 방패";
        if (item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD) return "수호검";
        if (item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE) return "전투 도끼";
        if (item == Items.IRON_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE) return "수호 흉갑";
        if (item == Items.IRON_HELMET || item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET) return "수호 투구";
        return "습격 장비";
    }

    public enum Rarity {
        COMMON("일반", ChatFormatting.GRAY, 1),
        UNCOMMON("고급", ChatFormatting.GREEN, 2),
        RARE("희귀", ChatFormatting.AQUA, 3),
        EPIC("영웅", ChatFormatting.LIGHT_PURPLE, 4),
        LEGENDARY("전설", ChatFormatting.GOLD, 5);

        private final String displayName;
        private final ChatFormatting formatting;
        private final int powerStep;

        Rarity(String displayName, ChatFormatting formatting, int powerStep) {
            this.displayName = displayName;
            this.formatting = formatting;
            this.powerStep = powerStep;
        }

        public String displayName() { return displayName; }
        public ChatFormatting formatting() { return formatting; }
        public int powerStep() { return powerStep; }
        public Rarity next() { return values()[Math.min(values().length - 1, ordinal() + 1)]; }
    }
}
