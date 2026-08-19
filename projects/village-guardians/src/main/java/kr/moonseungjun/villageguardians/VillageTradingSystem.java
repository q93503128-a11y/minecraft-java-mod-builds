package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VillageTradingSystem {
    private static final int MAIN_INVENTORY_SLOTS = 36;
    private static final String SALE_ONLY_PREFIX = "[판매용]";
    private static final Map<Item, Integer> LEGACY_PRICES = new LinkedHashMap<>();
    private static final Map<String, Integer> NAMED_PRICES = new LinkedHashMap<>();

    static {
        LEGACY_PRICES.put(Items.ROTTEN_FLESH, 1);
        LEGACY_PRICES.put(Items.BONE, 2);
        LEGACY_PRICES.put(Items.STRING, 2);
        LEGACY_PRICES.put(Items.SPIDER_EYE, 3);
        LEGACY_PRICES.put(Items.GUNPOWDER, 4);
        LEGACY_PRICES.put(Items.SLIME_BALL, 4);
        LEGACY_PRICES.put(Items.ENDER_PEARL, 10);
        LEGACY_PRICES.put(Items.BLAZE_ROD, 12);

        NAMED_PRICES.put("[판매용] 금 간 오크 송곳니", 3);
        NAMED_PRICES.put("[판매용] 찢긴 전투 끈", 3);
        NAMED_PRICES.put("[판매용] 응고된 마력낭", 5);
        NAMED_PRICES.put("[판매용] 폭파병 화약 주머니", 7);
        NAMED_PRICES.put("[판매용] 뒤틀린 지휘핵", 18);
        NAMED_PRICES.put("[판매용] 전쟁 주술봉 파편", 22);
        // Useful supplies keep a manual resale value, but must never be swept by the bulk junk action.
        NAMED_PRICES.put("수호 화살", 1);
        NAMED_PRICES.put("마을 배급 식량", 2);
    }

    private VillageTradingSystem() {}

    public static List<SellCandidate> sellCandidates(ServerPlayer player) {
        List<SellCandidate> result = new ArrayList<>();
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            int unit = unitValue(stack);
            if (stack.isEmpty() || unit <= 0) continue;
            result.add(new SellCandidate(slot, displayName(stack), stack.getCount(), unit, unit * stack.getCount()));
        }
        return List.copyOf(result);
    }

    public static String sellSelected(ServerPlayer player, int slot) {
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        if (slot < 0 || slot >= limit) return "판매할 아이템 슬롯이 올바르지 않습니다.";
        ItemStack stack = player.getInventory().getItem(slot);
        int unit = unitValue(stack);
        if (stack.isEmpty() || unit <= 0) return "이 아이템은 상점에서 판매할 수 없습니다.";
        String name = displayName(stack);
        int count = stack.getCount();
        int payout = adjustedPayout(player, unit * count);
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.getInventory().setChanged();
        VillageProgressionSystem.addCoins(player, payout, name + " 판매");
        return name + " " + count + "개 판매 완료 | 주화 +" + payout;
    }

    public static String sellMonsterDrops(ServerPlayer player) {
        int soldItems = 0;
        int value = 0;
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !isSaleOnlyLoot(stack)) continue;
            int unit = unitValue(stack);
            int count = stack.getCount();
            soldItems += count;
            value += count * unit;
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        if (soldItems <= 0) return "판매용 전리품이 없습니다.";
        player.getInventory().setChanged();
        int payout = adjustedPayout(player, value);
        VillageProgressionSystem.addCoins(player, payout, "판매용 전리품 정산");
        return "판매용 전리품 " + soldItems + "개 판매 완료 | 주화 +" + payout;
    }

    private static boolean isSaleOnlyLoot(ItemStack stack) {
        if (VillageRaidLootSystem.saleValue(stack) > 0) return true;
        String name = plainName(stack);
        if (!name.isBlank()) return name.startsWith(SALE_ONLY_PREFIX);
        return stack.get(DataComponents.CUSTOM_NAME) == null && LEGACY_PRICES.containsKey(stack.getItem());
    }

    private static int unitValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int raidValue = VillageRaidLootSystem.saleValue(stack);
        if (raidValue > 0) return raidValue;
        String name = plainName(stack);
        Integer named = NAMED_PRICES.get(name);
        if (named != null) return named;
        VillageEquipmentRaritySystem.Rarity rarity = VillageEquipmentRaritySystem.rarityOf(stack);
        if (rarity != null) {
            return 18 + rarity.powerStep() * 18 + VillageEquipmentRaritySystem.enhancementLevel(stack) * 14;
        }
        if (stack.get(DataComponents.CUSTOM_NAME) == null) {
            return LEGACY_PRICES.getOrDefault(stack.getItem(), 0);
        }
        for (VillageEquipmentShop.Offer offer : VillageEquipmentShop.offers()) {
            if (offer.matches(stack)) return Math.max(1, offer.cost() / 3);
        }
        return 0;
    }

    private static int adjustedPayout(ServerPlayer player, int value) {
        float multiplier = VillageSkillTreeSystem.coinRewardMultiplier(player)
                * VillageDefenseResearchSystem.lootValueMultiplier();
        return Math.max(1, Math.round(value * multiplier));
    }

    private static String displayName(ItemStack stack) {
        String name = plainName(stack);
        return name.isBlank() ? stack.getHoverName().getString() : name;
    }

    private static String plainName(ItemStack stack) {
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        return custom == null ? "" : ChatFormatting.stripFormatting(custom.getString());
    }

    public record SellCandidate(int slot, String name, int count, int unitValue, int totalValue) {}
}
