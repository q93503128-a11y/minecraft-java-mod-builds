package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class VillageEquipmentShop {
    private VillageEquipmentShop() {}

    public static List<Offer> offers() { return List.of(Offer.values()); }

    public static List<Offer> offers(Category category) {
        return Arrays.stream(Offer.values()).filter(offer -> offer.category() == category).toList();
    }

    /** Deterministic daily inventory. Eligible items rotate instead of accumulating forever. */
    public static List<Offer> currentOffers(int day) {
        int safeDay = Math.max(1, day);
        List<Offer> result = new ArrayList<>();
        result.addAll(rotatingOffers(Category.EQUIPMENT, safeDay, 3));
        result.addAll(rotatingOffers(Category.ARMOR, safeDay, 2));
        return List.copyOf(result);
    }

    public static boolean isStocked(Offer offer, int day) {
        return offer != null && currentOffers(day).contains(offer);
    }

    private static List<Offer> rotatingOffers(Category category, int day, int maximum) {
        List<Offer> eligible = Arrays.stream(Offer.values())
                .filter(offer -> offer.category() == category && offer.requiredDay() <= day)
                .toList();
        if (eligible.isEmpty()) return List.of();
        int count = Math.min(maximum, eligible.size());
        int start = Math.floorMod(day - 1 + category.ordinal() * 2, eligible.size());
        List<Offer> selected = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            selected.add(eligible.get((start + index) % eligible.size()));
        }
        return List.copyOf(selected);
    }

    public static String purchase(ServerPlayer player, String offerId) {
        Offer offer = Offer.parse(offerId).orElse(null);
        if (offer == null) return "알 수 없는 장비 상품입니다.";
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.STOREHOUSE)) {
            return "상점이 파괴되어 장비를 구매할 수 없습니다.";
        }
        int day = VillageCouncilState.currentDay();
        if (!isStocked(offer, day)) return "오늘 입고된 상품이 아닙니다. 상점 목록을 다시 확인하세요.";
        if (!VillageProgressionSystem.spendCoins(player, offer.cost())) {
            return "수호 주화가 부족합니다. 필요 " + offer.cost();
        }
        ItemStack stack = offer.createStack();
        if (!player.addItem(stack)) player.drop(stack, false);
        return offer.displayName() + " 구매 완료 | 남은 주화 " + VillageProgressionSystem.coins(player);
    }

    public static String status(ServerPlayer player, Offer offer) {
        if (!isStocked(offer, VillageCouncilState.currentDay())) return "오늘 미입고";
        if (VillageProgressionSystem.coins(player) < offer.cost()) return "주화 " + offer.cost() + " 필요";
        return "available";
    }

    public static float outgoingMultiplier(ServerPlayer player, boolean projectile) {
        float result = projectile ? bonusFor(player.getMainHandItem(), true) : bonusFor(player.getMainHandItem(), false);
        if (projectile) result = Math.max(result, bonusFor(player.getOffhandItem(), true));
        float rarity = projectile
                ? Math.max(VillageEquipmentRaritySystem.projectileMultiplier(player.getMainHandItem()),
                VillageEquipmentRaritySystem.projectileMultiplier(player.getOffhandItem()))
                : VillageEquipmentRaritySystem.meleeMultiplier(player.getMainHandItem());
        float relic = projectile ? VillageRelicSystem.projectileMultiplier(player)
                : VillageRelicSystem.meleeMultiplier(player);
        return result * rarity * relic;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float reduction = 0.0f;
        if (hasEquipped(player, Offer.WARD_SHIELD)) reduction += 0.05f;
        if (hasEquipped(player, Offer.BASTION_CHEST)) reduction += 0.08f;
        if (hasEquipped(player, Offer.AEGIS_CHEST)) reduction += 0.10f;
        float rarityMultiplier = VillageEquipmentRaritySystem.incomingMultiplier(player);
        return Math.max(0.58f, (1.0f - reduction) * rarityMultiplier
                * VillageRelicSystem.incomingMultiplier(player));
    }

    public static float roleSkillMultiplier(ServerPlayer player) {
        float base = hasEquipped(player, Offer.ARCANE_FOCUS) ? 1.18f : 1.0f;
        return base * VillageEquipmentRaritySystem.skillMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player);
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

    public enum Category {
        EQUIPMENT("장비"), ARMOR("방어구"), OTHER("기타");
        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public enum Offer {
        WATCH_SWORD("watch_sword", "파수대 철검", Category.EQUIPMENT, Items.IRON_SWORD, 1, 90,
                "근접 공격 강화", 1.08f, 1.0f),
        HUNTER_BOW("hunter_bow", "성루 사냥활", Category.EQUIPMENT, Items.BOW, 2, 140,
                "원거리 공격 강화", 1.0f, 1.10f),
        WARD_SHIELD("ward_shield", "수호 문양 방패", Category.ARMOR, Items.SHIELD, 2, 170,
                "받는 피해 감소", 1.0f, 1.0f),
        VETERAN_BLADE("veteran_blade", "노련한 수호검", Category.EQUIPMENT, Items.DIAMOND_SWORD, 4, 330,
                "근접 공격 크게 강화", 1.17f, 1.0f),
        SIEGE_CROSSBOW("siege_crossbow", "공성 파쇄쇠뇌", Category.EQUIPMENT, Items.CROSSBOW, 5, 390,
                "원거리 공격 크게 강화", 1.0f, 1.20f),
        ARCANE_FOCUS("arcane_focus", "비전 집중봉", Category.EQUIPMENT, Items.BLAZE_ROD, 5, 420,
                "장착 기술 피해와 치유 강화", 1.0f, 1.0f),
        BASTION_CHEST("bastion_chest", "성채 수호 흉갑", Category.ARMOR, Items.DIAMOND_CHESTPLATE, 6, 560,
                "생존력 강화", 1.0f, 1.0f),
        AEGIS_CHEST("aegis_chest", "최후 방벽 흉갑", Category.ARMOR, Items.NETHERITE_CHESTPLATE, 9, 900,
                "후반 생존력 크게 강화", 1.0f, 1.0f),
        DAWN_BLADE("dawn_blade", "새벽 절단검", Category.EQUIPMENT, Items.NETHERITE_SWORD, 10, 980,
                "최상급 근접 공격 강화", 1.28f, 1.0f),
        STAR_BOW("star_bow", "별빛 장궁", Category.EQUIPMENT, Items.BOW, 10, 980,
                "최상급 원거리 공격 강화", 1.0f, 1.28f);

        private final String id;
        private final String displayName;
        private final Category category;
        private final Item item;
        private final int requiredDay;
        private final int cost;
        private final String effect;
        private final float meleeMultiplier;
        private final float projectileMultiplier;

        Offer(String id, String displayName, Category category, Item item, int requiredDay, int cost,
              String effect, float meleeMultiplier, float projectileMultiplier) {
            this.id = id;
            this.displayName = displayName;
            this.category = category;
            this.item = item;
            this.requiredDay = requiredDay;
            this.cost = cost;
            this.effect = effect;
            this.meleeMultiplier = meleeMultiplier;
            this.projectileMultiplier = projectileMultiplier;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public Category category() { return category; }
        public int requiredDay() { return requiredDay; }
        @Deprecated public int requiredLevel() { return 0; }
        public int cost() { return cost; }
        public String effect() { return effect; }
        public float meleeMultiplier() { return meleeMultiplier; }
        public float projectileMultiplier() { return projectileMultiplier; }

        public VillageEquipmentRaritySystem.Rarity rarity() {
            if (requiredDay >= 9) return VillageEquipmentRaritySystem.Rarity.LEGENDARY;
            if (requiredDay >= 6) return VillageEquipmentRaritySystem.Rarity.EPIC;
            if (requiredDay >= 4) return VillageEquipmentRaritySystem.Rarity.RARE;
            if (requiredDay >= 2) return VillageEquipmentRaritySystem.Rarity.UNCOMMON;
            return VillageEquipmentRaritySystem.Rarity.COMMON;
        }

        public ItemStack createStack() {
            return VillageEquipmentRaritySystem.createNamed(item, rarity(), displayName);
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == item
                    && displayName.equals(VillageEquipmentRaritySystem.baseDisplayName(stack));
        }

        public static Optional<Offer> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(offer -> offer.id.equals(normalized)).findFirst();
        }
    }
}
