package kr.moonseungjun.titanbreak.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * P0 rule: vanilla armor slots may still render in the stock inventory screen,
 * but armor pieces are immediately returned to the player's inventory and do not
 * participate in TITANBREAK combat. The final augmentation screen will replace
 * these slots entirely.
 */
public final class VanillaArmorLockout {
    private VanillaArmorLockout() {}

    public static void tick(ServerPlayer player) {
        unequip(player, EquipmentSlot.HEAD);
        unequip(player, EquipmentSlot.CHEST);
        unequip(player, EquipmentSlot.LEGS);
        unequip(player, EquipmentSlot.FEET);
    }

    private static void unequip(ServerPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) return;

        ItemStack copy = stack.copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }
}
