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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Graded game equipment, three-item fusion and per-item smithy enhancement. */
public final class VillageEquipmentRaritySystem {
    private static final int MAIN_INVENTORY_SLOTS = 36;
    private static final int MAX_ENHANCEMENT = 30;
    private static final List<Item> EARLY_ITEMS = List.of(
            Items.IRON_SWORD, Items.BOW, Items.SHIELD, Items.IRON_CHESTPLATE, Items.CROSSBOW);
    private static final List<Item> LATE_ITEMS = List.of(
            Items.DIAMOND_SWORD, Items.BOW, Items.SHIELD, Items.DIAMOND_CHESTPLATE, Items.CROSSBOW,
            Items.DIAMOND_AXE, Items.DIAMOND_HELMET);

    private VillageEquipmentRaritySystem() {}

    public static ItemStack createRaidDrop(int day, boolean boss, RandomSource random) {
        return createRaidDrop(day, boss, null, random);
    }

    public static ItemStack createRaidDrop(int day, boolean boss,
            VillageEnemyArchetypeSystem.Archetype archetype, RandomSource random) {
        List<Item> pool;
        if (boss || archetype == null) pool = day >= 6 ? LATE_ITEMS : EARLY_ITEMS;
        else pool = switch (archetype) {
            case RUSHER, GRUNT, SHIELDBREAKER -> List.of(
                    day >= 6 ? Items.DIAMOND_SWORD : Items.IRON_SWORD,
                    day >= 6 ? Items.DIAMOND_AXE : Items.IRON_AXE, Items.SHIELD);
            case BULWARK, SIEGE_BEAST, IRON_WARLORD, DREAD_KNIGHT -> List.of(Items.SHIELD,
                    day >= 6 ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE,
                    day >= 6 ? Items.DIAMOND_HELMET : Items.IRON_HELMET);
            case MARKSMAN, TOWER_HUNTER, WAR_CHANTER -> List.of(Items.BOW, Items.CROSSBOW,
                    day >= 6 ? Items.DIAMOND_HELMET : Items.IRON_HELMET);
            case SAPPER -> List.of(Items.CROSSBOW, Items.IRON_AXE, Items.SHIELD);
            case HEXER, NECROMANCER, PLAGUE_ARCHON -> List.of(Items.BLAZE_ROD, Items.BOW,
                    day >= 6 ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE);
        };
        Item item = pool.get(random.nextInt(pool.size()));
        return createNamed(item, rollRarity(day, boss, random), displayName(item));
    }

    public static ItemStack createNamed(Item item, Rarity rarity, String name) {
        ItemStack stack = item.getDefaultInstance();
        applyName(stack, rarity, name, 0);
        return stack;
    }

    public static List<FusionCandidate> fusionCandidates(ServerPlayer player) {
        List<FusionCandidate> result = new ArrayList<>();
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            Rarity rarity = rarityOf(stack);
            if (rarity == null || rarity == Rarity.LEGENDARY) continue;
            int enhancement = enhancementLevel(stack);
            String group = stack.getItem() + "@" + rarity.name() + "@" + enhancement;
            result.add(new FusionCandidate(slot, group, baseDisplayName(stack),
                    rarity.displayName() + (enhancement > 0 ? " · 강화 +" + enhancement : ""),
                    stack.getItem().toString()));
        }
        return List.copyOf(result);
    }

    public static String combineSelected(ServerPlayer player, int firstSlot, int secondSlot, int thirdSlot) {
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        Set<Integer> unique = new HashSet<>(List.of(firstSlot, secondSlot, thirdSlot));
        if (unique.size() != 3 || unique.stream().anyMatch(slot -> slot < 0 || slot >= limit)) {
            return "서로 다른 인벤토리 장비 세 개를 선택해야 합니다.";
        }
        ItemStack first = player.getInventory().getItem(firstSlot);
        ItemStack second = player.getInventory().getItem(secondSlot);
        ItemStack third = player.getInventory().getItem(thirdSlot);
        Rarity rarity = rarityOf(first);
        int enhancement = enhancementLevel(first);
        if (rarity == null || rarity == Rarity.LEGENDARY
                || rarityOf(second) != rarity || rarityOf(third) != rarity
                || enhancementLevel(second) != enhancement || enhancementLevel(third) != enhancement
                || second.getItem() != first.getItem() || third.getItem() != first.getItem()) {
            return "같은 종류·같은 등급·같은 강화 단계 장비 세 개를 선택해야 합니다.";
        }
        Item item = first.getItem();
        String name = baseDisplayName(first);
        first.shrink(1);
        second.shrink(1);
        third.shrink(1);
        ItemStack result = createNamed(item, rarity.next(), name);
        applyName(result, rarity.next(), name, enhancement);
        if (!player.addItem(result)) player.drop(result, false);
        player.getInventory().setChanged();
        return name + " 세 개를 " + rarity.next().displayName() + " 등급 하나로 합성했습니다."
                + (enhancement > 0 ? " 강화 +" + enhancement + "는 유지됩니다." : "");
    }

    public static List<EnhancementCandidate> enhancementCandidates(ServerPlayer player) {
        List<EnhancementCandidate> result = new ArrayList<>();
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            Rarity rarity = rarityOf(stack);
            if (rarity == null || !isUpgradeable(stack.getItem())) continue;
            int current = enhancementLevel(stack);
            int maximum = maximumEnhancement(stack);
            result.add(new EnhancementCandidate(slot, baseDisplayName(stack), rarity.displayName(),
                    current, maximum, enhancementCost(stack), stack.getItem().toString(),
                    enhancementEffectSummary(stack, current),
                    current >= maximum ? "최대 강화" : enhancementEffectSummary(stack, current + 1)));
        }
        return List.copyOf(result);
    }

    public static String enhanceSelected(ServerPlayer player, int slot) {
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.SMITHY)) {
            return "대장간이 파괴되어 장비를 강화할 수 없습니다.";
        }
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        if (slot < 0 || slot >= limit) return "강화할 장비 슬롯이 올바르지 않습니다.";
        ItemStack stack = player.getInventory().getItem(slot);
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isUpgradeable(stack.getItem())) return "게임 전용 등급 장비만 강화할 수 있습니다.";
        int current = enhancementLevel(stack);
        int maximum = maximumEnhancement(stack);
        if (current >= maximum) {
            return "현재 대장간에서는 " + baseDisplayName(stack) + "을(를) 더 강화할 수 없습니다. 최대 +" + maximum;
        }
        int cost = enhancementCost(stack);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        applyName(stack, rarity, baseDisplayName(stack), current + 1);
        player.getInventory().setChanged();
        return baseDisplayName(stack) + " 강화 성공 | +" + (current + 1)
                + " / +" + maximum + " | 남은 주화 " + VillageProgressionSystem.coins(player);
    }

    public static String enhancementEffectSummary(ItemStack stack, int enhancement) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null) return "등급 효과 없음";
        int safe = Math.max(0, enhancement);
        Item item = stack.getItem();
        if (isMelee(item) || isProjectile(item)) {
            float value = 1.0f + rarity.powerStep() * 0.055f + enhancementAttackBonus(safe);
            String type = isMelee(item) ? "근접" : "원거리";
            return String.format(java.util.Locale.ROOT, "%s 피해 x%.3f (+%.1f%%)",
                    type, value, (value - 1.0f) * 100.0f);
        }
        if (isArmor(item) || item == Items.SHIELD) {
            float reduction = rarity.powerStep() * 0.012f + enhancementDefenseBonus(safe);
            return String.format(java.util.Locale.ROOT, "장비 단독 피해 감소 %.1f%%", reduction * 100.0f);
        }
        if (item == Items.BLAZE_ROD) {
            float value = 1.0f + rarity.powerStep() * 0.035f + enhancementSkillBonus(safe);
            return String.format(java.util.Locale.ROOT, "직업 기술 효과 x%.3f (+%.1f%%)",
                    value, (value - 1.0f) * 100.0f);
        }
        return "강화 단계 +" + safe;
    }

    public static int maximumEnhancement() {
        return MAX_ENHANCEMENT;
    }

    public static int maximumEnhancement(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isUpgradeable(stack.getItem())) return 0;
        int smithy = VillageProgressionSystem.smithyLevel();
        int forgeCap = switch (smithy) {
            case 0 -> 1;
            case 1 -> 4;
            case 2 -> 7;
            case 3 -> 10;
            case 4 -> 14;
            default -> 18;
        };
        int endlessBonus = Math.max(0, (VillageCouncilState.currentDay() - 10) / 2);
        return Math.min(hardEnhancementCap(stack.getItem()), forgeCap + endlessBonus);
    }

    public static int enhancementCost(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        int current = enhancementLevel(stack);
        int masterwork = Math.max(0, current - 10);
        return 70 + current * 90 + masterwork * masterwork * 9
                + (rarity == null ? 0 : rarity.powerStep() * 30);
    }

    public static float meleeMultiplier(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isMelee(stack.getItem())) return 1.0f;
        return 1.0f + rarity.powerStep() * 0.055f + enhancementAttackBonus(enhancementLevel(stack));
    }

    public static float projectileMultiplier(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isProjectile(stack.getItem())) return 1.0f;
        return 1.0f + rarity.powerStep() * 0.055f + enhancementAttackBonus(enhancementLevel(stack));
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float reduction = rarityReduction(player.getOffhandItem());
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            reduction += rarityReduction(player.getItemBySlot(slot));
        }
        return Math.max(0.58f, 1.0f - reduction);
    }

    public static float skillMultiplier(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        float mainBonus = rarityStep(main) * 0.035f + enhancementSkillBonus(enhancementLevel(main));
        float offBonus = rarityStep(off) * 0.035f + enhancementSkillBonus(enhancementLevel(off));
        return 1.0f + Math.max(mainBonus, offBonus);
    }

    public static int bestEquippedEnhancement(ServerPlayer player) {
        int best = Math.max(enhancementLevel(player.getMainHandItem()), enhancementLevel(player.getOffhandItem()));
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            best = Math.max(best, enhancementLevel(player.getItemBySlot(slot)));
        }
        return best;
    }

    public static Rarity rarityOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String stamped = VillageEquipmentIdentity.rarity(stack);
        if (!stamped.isBlank()) {
            try { return Rarity.valueOf(stamped); }
            catch (IllegalArgumentException ignored) { return null; }
        }
        if (!VillageEquipmentIdentity.canReadLegacyName(stack)) return null;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) return null;
        String plain = ChatFormatting.stripFormatting(name.getString());
        for (Rarity rarity : Rarity.values()) {
            if (plain.startsWith("[" + rarity.displayName() + "] ")) return rarity;
        }
        return null;
    }

    public static int enhancementLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int stamped = VillageEquipmentIdentity.enhancement(stack);
        if (stamped >= 0) return stamped;
        if (!VillageEquipmentIdentity.canReadLegacyName(stack)) return 0;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) return 0;
        String plain = ChatFormatting.stripFormatting(name.getString());
        int marker = plain.lastIndexOf(" +");
        if (marker < 0 || marker + 2 >= plain.length()) return 0;
        String raw = plain.substring(marker + 2);
        for (int index = 0; index < raw.length(); index++) {
            if (!Character.isDigit(raw.charAt(index))) return 0;
        }
        try { return Math.max(0, Integer.parseInt(raw)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    public static String baseDisplayName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "장비";
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        String plain = name == null ? displayName(stack.getItem())
                : ChatFormatting.stripFormatting(name.getString());
        for (Rarity rarity : Rarity.values()) {
            String prefix = "[" + rarity.displayName() + "] ";
            if (plain.startsWith(prefix)) {
                plain = plain.substring(prefix.length());
                break;
            }
        }
        int marker = plain.lastIndexOf(" +");
        if (marker >= 0 && marker + 2 < plain.length()) {
            String raw = plain.substring(marker + 2);
            boolean digits = !raw.isEmpty();
            for (int index = 0; index < raw.length(); index++) digits &= Character.isDigit(raw.charAt(index));
            if (digits) plain = plain.substring(0, marker);
        }
        return plain;
    }

    private static void applyName(ItemStack stack, Rarity rarity, String name, int enhancement) {
        String suffix = enhancement > 0 ? " +" + enhancement : "";
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("[" + rarity.displayName() + "] " + name + suffix)
                        .withStyle(rarity.formatting()));
        VillageEquipmentIdentity.stampRarity(stack, rarity.name(), enhancement);
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
        return rarity.powerStep() * 0.012f + enhancementDefenseBonus(enhancementLevel(stack));
    }

    private static float enhancementAttackBonus(int level) {
        int safe = Math.max(0, level);
        int early = Math.min(10, safe);
        int master = Math.min(10, Math.max(0, safe - 10));
        int apex = Math.max(0, safe - 20);
        return early * 0.040f + master * 0.0225f + apex * 0.0125f;
    }

    private static float enhancementSkillBonus(int level) {
        int safe = Math.max(0, level);
        int early = Math.min(10, safe);
        int master = Math.min(10, Math.max(0, safe - 10));
        int apex = Math.max(0, safe - 20);
        return early * 0.030f + master * 0.0175f + apex * 0.010f;
    }

    private static float enhancementDefenseBonus(int level) {
        int safe = Math.max(0, level);
        int early = Math.min(10, safe);
        int master = Math.min(10, Math.max(0, safe - 10));
        return early * 0.0060f + master * 0.0035f;
    }

    private static int hardEnhancementCap(Item item) {
        if (item == Items.MACE || item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD) return 30;
        if (item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE) return 28;
        if (item == Items.TRIDENT || item == Items.BLAZE_ROD) return 26;
        if (item == Items.BOW) return 25;
        if (item == Items.CROSSBOW) return 24;
        if (item == Items.SHIELD) return 22;
        if (isArmor(item)) return 20;
        return 0;
    }

    private static int rarityStep(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        return rarity == null ? 0 : rarity.powerStep();
    }

    private static boolean isUpgradeable(Item item) {
        return isMelee(item) || isProjectile(item) || isArmor(item)
                || item == Items.SHIELD || item == Items.BLAZE_ROD;
    }

    private static boolean isMelee(Item item) {
        return item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD
                || item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE
                || item == Items.MACE;
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

    public static String displayName(Item item) {
        if (item == Items.BOW) return "수호 장궁";
        if (item == Items.CROSSBOW) return "수호 쇠뇌";
        if (item == Items.TRIDENT) return "성문 수호창";
        if (item == Items.MACE) return "공성 전투망치";
        if (item == Items.SHIELD) return "수호 방패";
        if (item == Items.BLAZE_ROD) return "비전 집중봉";
        if (item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD) return "수호검";
        if (item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE) return "전투 도끼";
        if (item == Items.IRON_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE) return "수호 흉갑";
        if (item == Items.IRON_HELMET || item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET) return "수호 투구";
        if (item == Items.IRON_LEGGINGS || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS) return "수호 각반";
        if (item == Items.IRON_BOOTS || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS) return "수호 장화";
        return "습격 장비";
    }

    public record FusionCandidate(int slot, String group, String name, String rarity, String itemId) {}
    public record EnhancementCandidate(int slot, String name, String rarity, int current, int maximum,
                                       int cost, String itemId, String currentEffect, String nextEffect) {}

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
