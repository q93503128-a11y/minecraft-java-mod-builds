package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class VillageEquipmentShop {
    private VillageEquipmentShop() {}

    public static List<Offer> offers() { return List.of(Offer.values()); }

    public static List<Offer> offers(Category category) {
        return Arrays.stream(Offer.values()).filter(offer -> offer.category() == category).toList();
    }

    public static List<Offer> currentOffers(int day) {
        int safeDay = Math.max(1, day);
        List<Offer> result = new ArrayList<>();
        result.addAll(rotatingOffers(Category.EQUIPMENT, safeDay, 4));
        result.addAll(rotatingOffers(Category.ARMOR, safeDay, 3));
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
        int start = Math.floorMod(day * 3 - 3 + category.ordinal() * 5, eligible.size());
        List<Offer> selected = new ArrayList<>();
        for (int index = 0; index < count; index++) selected.add(eligible.get((start + index) % eligible.size()));
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
        float named = projectile ? bonusFor(player.getMainHandItem(), true) : bonusFor(player.getMainHandItem(), false);
        if (projectile) named = Math.max(named, bonusFor(player.getOffhandItem(), true));
        float rarity = projectile
                ? Math.max(VillageEquipmentRaritySystem.projectileMultiplier(player.getMainHandItem()),
                VillageEquipmentRaritySystem.projectileMultiplier(player.getOffhandItem()))
                : VillageEquipmentRaritySystem.meleeMultiplier(player.getMainHandItem());
        float relic = projectile ? VillageRelicSystem.projectileMultiplier(player)
                : VillageRelicSystem.meleeMultiplier(player);
        return named * rarity * relic;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float reduction = equippedOffers(player).stream()
                .map(Offer::damageReduction)
                .reduce(0.0f, Float::sum);
        float rarityMultiplier = VillageEquipmentRaritySystem.incomingMultiplier(player);
        return Math.max(0.52f, (1.0f - Math.min(0.42f, reduction)) * rarityMultiplier
                * VillageRelicSystem.incomingMultiplier(player));
    }

    public static float roleSkillMultiplier(ServerPlayer player) {
        float named = 1.0f;
        for (Offer offer : equippedOffers(player)) named *= offer.skillMultiplier();
        return named * VillageEquipmentRaritySystem.skillMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player);
    }

    public static int cooldownReductionSeconds(ServerPlayer player) {
        int result = 0;
        for (Offer offer : equippedOffers(player)) result += offer.cooldownReductionSeconds();
        return Math.min(4, result);
    }

    private static Set<Offer> equippedOffers(ServerPlayer player) {
        EnumSet<Offer> result = EnumSet.noneOf(Offer.class);
        collect(result, player.getMainHandItem());
        collect(result, player.getOffhandItem());
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            collect(result, player.getItemBySlot(slot));
        }
        return result;
    }

    private static void collect(Set<Offer> output, ItemStack stack) {
        for (Offer offer : Offer.values()) if (offer.matches(stack)) output.add(offer);
    }

    private static float bonusFor(ItemStack stack, boolean projectile) {
        Offer offer = Arrays.stream(Offer.values()).filter(value -> value.matches(stack)).findFirst().orElse(null);
        if (offer == null) return 1.0f;
        return projectile ? offer.projectileMultiplier() : offer.meleeMultiplier();
    }

    public enum Category {
        EQUIPMENT("장비"), ARMOR("방어구"), OTHER("기타");
        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public enum Offer {
        WATCH_SWORD("watch_sword", "파수대 철검", Category.EQUIPMENT, Items.IRON_SWORD, 1, 90,
                "근접 피해 +8%", 1.08f, 1.00f, 1.00f, 0.00f, 0),
        HUNTER_BOW("hunter_bow", "성루 사냥활", Category.EQUIPMENT, Items.BOW, 2, 140,
                "원거리 피해 +10%", 1.00f, 1.10f, 1.00f, 0.00f, 0),
        WARD_SHIELD("ward_shield", "수호 문양 방패", Category.ARMOR, Items.SHIELD, 2, 170,
                "받는 피해 5% 감소", 1.00f, 1.00f, 1.00f, 0.05f, 0),
        SENTINEL_AXE("sentinel_axe", "문지기 파쇄도끼", Category.EQUIPMENT, Items.IRON_AXE, 3, 230,
                "근접 피해 +13%", 1.13f, 1.00f, 1.00f, 0.00f, 0),
        TWINSTRING_BOW("twinstring_bow", "쌍현 전투궁", Category.EQUIPMENT, Items.BOW, 4, 315,
                "원거리 피해 +15% · 기술 재사용 -1초", 1.00f, 1.15f, 1.00f, 0.00f, 1),
        BULWARK_HELM("bulwark_helm", "성루 방벽투구", Category.ARMOR, Items.DIAMOND_HELMET, 4, 350,
                "받는 피해 6% 감소", 1.00f, 1.00f, 1.00f, 0.06f, 0),
        VETERAN_BLADE("veteran_blade", "노련한 수호검", Category.EQUIPMENT, Items.DIAMOND_SWORD, 4, 330,
                "근접 피해 +17%", 1.17f, 1.00f, 1.00f, 0.00f, 0),
        SIEGE_CROSSBOW("siege_crossbow", "공성 파쇄쇠뇌", Category.EQUIPMENT, Items.CROSSBOW, 5, 390,
                "원거리 피해 +20%", 1.00f, 1.20f, 1.00f, 0.00f, 0),
        WIND_BLADE("wind_blade", "질풍 호위검", Category.EQUIPMENT, Items.DIAMOND_SWORD, 5, 430,
                "근접 피해 +15% · 기술 효과 +5% · 재사용 -1초", 1.15f, 1.00f, 1.05f, 0.00f, 1),
        MARCH_BOOTS("march_boots", "진군자의 전투화", Category.ARMOR, Items.DIAMOND_BOOTS, 5, 420,
                "받는 피해 4% 감소 · 기술 재사용 -1초", 1.00f, 1.00f, 1.00f, 0.04f, 1),
        ARCANE_FOCUS("arcane_focus", "비전 집중봉", Category.EQUIPMENT, Items.BLAZE_ROD, 5, 420,
                "직업 기술 피해·치유 +18%", 1.00f, 1.00f, 1.18f, 0.00f, 0),
        BASTION_CHEST("bastion_chest", "성채 수호 흉갑", Category.ARMOR, Items.DIAMOND_CHESTPLATE, 6, 560,
                "받는 피해 8% 감소", 1.00f, 1.00f, 1.00f, 0.08f, 0),
        EAGLE_CROSSBOW("eagle_crossbow", "독수리 추격쇠뇌", Category.EQUIPMENT, Items.CROSSBOW, 7, 650,
                "원거리 피해 +23% · 기술 재사용 -1초", 1.00f, 1.23f, 1.00f, 0.00f, 1),
        FROST_FOCUS("frost_focus", "서리결정 지휘봉", Category.EQUIPMENT, Items.BLAZE_ROD, 7, 680,
                "직업 기술 효과 +22% · 재사용 -1초", 1.00f, 1.00f, 1.22f, 0.00f, 1),
        RUNE_LEGGINGS("rune_leggings", "룬각인 전투각반", Category.ARMOR, Items.DIAMOND_LEGGINGS, 7, 710,
                "받는 피해 7% 감소 · 기술 효과 +8%", 1.00f, 1.00f, 1.08f, 0.07f, 0),
        EXECUTIONER_AXE("executioner_axe", "처형대장의 흑도끼", Category.EQUIPMENT, Items.NETHERITE_AXE, 8, 840,
                "근접 피해 +25%", 1.25f, 1.00f, 1.00f, 0.00f, 0),
        TITAN_SHIELD("titan_shield", "거신의 성문방패", Category.ARMOR, Items.SHIELD, 8, 820,
                "받는 피해 11% 감소", 1.00f, 1.00f, 1.00f, 0.11f, 0),
        AEGIS_CHEST("aegis_chest", "최후 방벽 흉갑", Category.ARMOR, Items.NETHERITE_CHESTPLATE, 9, 900,
                "받는 피해 10% 감소", 1.00f, 1.00f, 1.00f, 0.10f, 0),
        DAWN_BLADE("dawn_blade", "새벽 절단검", Category.EQUIPMENT, Items.NETHERITE_SWORD, 10, 980,
                "근접 피해 +28%", 1.28f, 1.00f, 1.00f, 0.00f, 0),
        STAR_BOW("star_bow", "별빛 장궁", Category.EQUIPMENT, Items.BOW, 10, 980,
                "원거리 피해 +28%", 1.00f, 1.28f, 1.00f, 0.00f, 0),
        DAWN_SCEPTER("dawn_scepter", "여명 성광홀", Category.EQUIPMENT, Items.BLAZE_ROD, 11, 1160,
                "직업 기술 효과 +30% · 재사용 -2초", 1.00f, 1.00f, 1.30f, 0.00f, 2),
        RIFT_LONGBOW("rift_longbow", "균열 관통장궁", Category.EQUIPMENT, Items.BOW, 12, 1250,
                "원거리 피해 +33% · 기술 효과 +5%", 1.00f, 1.33f, 1.05f, 0.00f, 0),
        PHOENIX_CHEST("phoenix_chest", "불사조 수호흉갑", Category.ARMOR, Items.NETHERITE_CHESTPLATE, 12, 1320,
                "받는 피해 13% 감소 · 기술 효과 +8%", 1.00f, 1.00f, 1.08f, 0.13f, 0),
        WAR_CROWN("war_crown", "끝없는 전쟁왕관", Category.ARMOR, Items.NETHERITE_HELMET, 14, 1580,
                "받는 피해 10% 감소 · 기술 효과 +12% · 재사용 -1초", 1.00f, 1.00f, 1.12f, 0.10f, 1);

        private final String id;
        private final String displayName;
        private final Category category;
        private final Item item;
        private final int requiredDay;
        private final int cost;
        private final String effect;
        private final float meleeMultiplier;
        private final float projectileMultiplier;
        private final float skillMultiplier;
        private final float damageReduction;
        private final int cooldownReductionSeconds;

        Offer(String id, String displayName, Category category, Item item, int requiredDay, int cost,
              String effect, float meleeMultiplier, float projectileMultiplier,
              float skillMultiplier, float damageReduction, int cooldownReductionSeconds) {
            this.id = id;
            this.displayName = displayName;
            this.category = category;
            this.item = item;
            this.requiredDay = requiredDay;
            this.cost = cost;
            this.effect = effect;
            this.meleeMultiplier = meleeMultiplier;
            this.projectileMultiplier = projectileMultiplier;
            this.skillMultiplier = skillMultiplier;
            this.damageReduction = damageReduction;
            this.cooldownReductionSeconds = cooldownReductionSeconds;
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
        public float skillMultiplier() { return skillMultiplier; }
        public float damageReduction() { return damageReduction; }
        public int cooldownReductionSeconds() { return cooldownReductionSeconds; }

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
