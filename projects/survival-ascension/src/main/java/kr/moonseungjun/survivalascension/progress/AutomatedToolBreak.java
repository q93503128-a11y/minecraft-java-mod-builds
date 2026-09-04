package kr.moonseungjun.survivalascension.progress;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps normal block-break hooks/drops for skill-expanded work while preventing
 * every automatically expanded block from charging another durability point.
 * The original player break remains vanilla-authoritative and pays normal wear.
 */
public final class AutomatedToolBreak {
    private AutomatedToolBreak() {}

    public static boolean destroyWithoutAdditionalWear(ServerPlayer player, BlockPos target) {
        ItemStack tool = player.getMainHandItem();
        if (player.isCreative() || tool.isEmpty() || !tool.isDamageableItem()) {
            return player.gameMode.destroyBlock(target);
        }
        int damageBefore = tool.getDamageValue();
        tool.setDamageValue(0);
        try {
            return player.gameMode.destroyBlock(target);
        } finally {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() == tool.getItem() && held.isDamageableItem()) {
                held.setDamageValue(damageBefore);
            }
        }
    }
}
