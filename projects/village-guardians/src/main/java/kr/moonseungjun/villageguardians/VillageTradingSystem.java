package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VillageTradingSystem {
    private static final Map<Item, Integer> PRICES = new LinkedHashMap<>();

    static {
        PRICES.put(Items.ROTTEN_FLESH, 1);
        PRICES.put(Items.BONE, 2);
        PRICES.put(Items.STRING, 2);
        PRICES.put(Items.SPIDER_EYE, 3);
        PRICES.put(Items.GUNPOWDER, 4);
        PRICES.put(Items.SLIME_BALL, 4);
        PRICES.put(Items.ENDER_PEARL, 10);
        PRICES.put(Items.BLAZE_ROD, 12);
    }

    private VillageTradingSystem() {
    }

    public static String sellMonsterDrops(ServerPlayer player) {
        int soldItems = 0;
        int value = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            Integer unitPrice = PRICES.get(stack.getItem());
            if (unitPrice == null || stack.isEmpty()) {
                continue;
            }
            int count = stack.getCount();
            soldItems += count;
            value += count * unitPrice;
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        if (soldItems <= 0) {
            return "판매 가능한 몬스터 전리품이 없습니다.";
        }
        int payout = Math.max(1, Math.round(value * VillageSkillTreeSystem.coinRewardMultiplier(player)));
        VillageProgressionSystem.addCoins(player, payout, "전리품 판매");
        return "몬스터 전리품 " + soldItems + "개 판매 완료 | 주화 +" + payout;
    }
}
