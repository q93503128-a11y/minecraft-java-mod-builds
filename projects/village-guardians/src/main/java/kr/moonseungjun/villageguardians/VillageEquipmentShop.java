package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class VillageEquipmentShop {
    private VillageEquipmentShop() {}

    public static List<Offer> offers() { return List.of(Offer.values()); }

    public static String purchase(ServerPlayer player, String offerId) {
        Offer offer = Offer.parse(offerId).orElse(null);
        if (offer == null) return "알 수 없는 장비 상품입니다.";
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.STOREHOUSE)) {
            return "상점·보급소가 파괴되어 장비를 구매할 수 없습니다.";
        }
        int level = VillageCouncilState.levelOf(player.getUUID());
        int day = VillageCouncilState.currentDay();
        if (level < offer.requiredLevel()) return "레벨 " + offer.requiredLevel() + "부터 구매할 수 있습니다.";
        if (day < offer.requiredDay()) return "제 " + offer.requiredDay() + "일부터 판매됩니다.";
        if (!VillageProgressionSystem.spendCoins(player, offer.cost())) {
            return "수호 주화가 부족합니다. 필요 " + offer.cost();
        }
        ItemStack stack = offer.createStack();
        if (!player.addItem(stack)) player.drop(stack, false);
        return offer.displayName() + " 구매 완료 | 남은 주화 " + VillageProgressionSystem.coins(player);
    }

    public static String status(ServerPlayer player, Offer offer) {
        int level = VillageCouncilState.levelOf(player.getUUID());
        int day = VillageCouncilState.currentDay();
        if (level < offer.requiredLevel()) return "Lv." + offer.requiredLevel() + " 필요";
        if (day < offer.requiredDay()) return offer.requiredDay() + "일차 필요";
        if (VillageProgressionSystem.coins(player) < offer.cost()) return "주화 " + offer.cost() + " 필요";
        return "구매 가능";
    }

    public static float outgoingMultiplier(ServerPlayer player, boolean projectile) {
        float result = projectile ? bonusFor(player.getMainHandItem(), true) : bonusFor(player.getMainHandItem(), false);
        if (projectile) result = Math.max(result, bonusFor(player.getOffhandItem(), true));
        return result;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float reduction = 0.0f;
        if (hasEquipped(player, Offer.WARD_SHIELD)) reduction += 0.05f;
        if (hasEquipped(player, Offer.BASTION_CHEST)) reduction += 0.08f;
        if (hasEquipped(player, Offer.AEGIS_CHEST)) reduction += 0.10f;
        return Math.max(0.72f, 1.0f - reduction);
    }

    public static float roleSkillMultiplier(ServerPlayer player) {
        return hasEquipped(player, Offer.ARCANE_FOCUS) ? 1.18f : 1.0f;
    }

    private static float bonusFor(ItemStack stack, boolean projectile) {
        Offer offer = Arrays.stream(Offer.values()).filter(value -> value.matches(stack)).findFirst().orElse(null);
        if (offer == null) return 1.0f;
        return projectile ? offer.projectileMultiplier() : offer.meleeMultiplier();
    }

    private static boolean hasEquipped(ServerPlayer player, Offer offer) {
        if (offer.matches(player.getMainHandItem()) || offer.matches(player.getOffhandItem())) return true;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (offer.matches(player.getItemBySlot(slot))) return true;
        }
        return false;
    }

    public enum Offer {
        WATCH_SWORD("watch_sword", "파수대 철검", Items.IRON_SWORD, 2, 1, 90, "근접 공격 강화", 1.08f, 1.0f),
        HUNTER_BOW("hunter_bow", "성루 사냥활", Items.BOW, 4, 2, 140, "원거리 공격 강화", 1.0f, 1.10f),
        WARD_SHIELD("ward_shield", "수호 문양 방패", Items.SHIELD, 5, 2, 170, "받는 피해 감소", 1.0f, 1.0f),
        VETERAN_BLADE("veteran_blade", "노련한 수호검", Items.DIAMOND_SWORD, 10, 4, 330, "근접 공격 크게 강화", 1.17f, 1.0f),
        SIEGE_CROSSBOW("siege_crossbow", "공성 파쇄쇠뇌", Items.CROSSBOW, 12, 5, 390, "원거리 공격 크게 강화", 1.0f, 1.20f),
        ARCANE_FOCUS("arcane_focus", "비전 집중봉", Items.BLAZE_ROD, 13, 5, 420, "장착 기술 피해와 치유 강화", 1.0f, 1.0f),
        BASTION_CHEST("bastion_chest", "성채 수호 흉갑", Items.DIAMOND_CHESTPLATE, 16, 6, 560, "생존력 강화", 1.0f, 1.0f),
        AEGIS_CHEST("aegis_chest", "최후 방벽 흉갑", Items.NETHERITE_CHESTPLATE, 23, 9, 900, "후반 생존력 크게 강화", 1.0f, 1.0f),
        DAWN_BLADE("dawn_blade", "새벽 절단검", Items.NETHERITE_SWORD, 25, 10, 980, "최상급 근접 공격 강화", 1.28f, 1.0f),
        STAR_BOW("star_bow", "별빛 장궁", Items.BOW, 25, 10, 980, "최상급 원거리 공격 강화", 1.0f, 1.28f);

        private final String id;
        private final String displayName;
        private final Item item;
        private final int requiredLevel;
        private final int requiredDay;
        private final int cost;
        private final String effect;
        private final float meleeMultiplier;
        private final float projectileMultiplier;

        Offer(String id, String displayName, Item item, int requiredLevel, int requiredDay, int cost,
              String effect, float meleeMultiplier, float projectileMultiplier) {
            this.id = id;
            this.displayName = displayName;
            this.item = item;
            this.requiredLevel = requiredLevel;
            this.requiredDay = requiredDay;
            this.cost = cost;
            this.effect = effect;
            this.meleeMultiplier = meleeMultiplier;
            this.projectileMultiplier = projectileMultiplier;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public int requiredLevel() { return requiredLevel; }
        public int requiredDay() { return requiredDay; }
        public int cost() { return cost; }
        public String effect() { return effect; }
        public float meleeMultiplier() { return meleeMultiplier; }
        public float projectileMultiplier() { return projectileMultiplier; }

        public ItemStack createStack() {
            ItemStack stack = item.getDefaultInstance();
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName).withStyle(ChatFormatting.GOLD));
            return stack;
        }

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || stack.getItem() != item) return false;
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            return name != null && displayName.equals(ChatFormatting.stripFormatting(name.getString()));
        }

        public static Optional<Offer> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(offer -> offer.id.equals(normalized)).findFirst();
        }
    }
}
