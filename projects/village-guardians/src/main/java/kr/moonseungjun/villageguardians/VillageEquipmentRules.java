package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class VillageEquipmentRules {
    private VillageEquipmentRules() {}

    public static void restoreDurability(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.isDamageableItem() && stack.getDamageValue() > 0) {
                    stack.setDamageValue(0);
                }
            }
        }
    }
}
