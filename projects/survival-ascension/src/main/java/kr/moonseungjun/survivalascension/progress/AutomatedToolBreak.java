package kr.moonseungjun.survivalascension.progress;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Skill-expanded work keeps vanilla block-break hooks and still wears tools, but automatic
 * extra blocks only pay one normal vanilla durability roll per four successful extra blocks.
 * The player's original manual break always remains vanilla-authoritative.
 */
public final class AutomatedToolBreak {
    private static final String WEAR_BANK_KEY = "survivalascension_bulk_tool_wear_bank";
    private static final int AUTOMATIC_BLOCKS_PER_WEAR = 4;

    private AutomatedToolBreak() {}

    public static boolean destroyWithReducedWear(ServerPlayer player, BlockPos target) {
        ItemStack tool = player.getMainHandItem();
        if (player.isCreative() || tool.isEmpty() || !tool.isDamageableItem()) {
            return player.gameMode.destroyBlock(target);
        }

        int bank = Math.max(0, player.getPersistentData().getIntOr(WEAR_BANK_KEY, 0));
        if (bank >= AUTOMATIC_BLOCKS_PER_WEAR - 1) {
            boolean broken = player.gameMode.destroyBlock(target);
            if (broken) player.getPersistentData().putInt(WEAR_BANK_KEY, 0);
            return broken;
        }

        int damageBefore = tool.getDamageValue();
        tool.setDamageValue(0);
        boolean broken;
        try {
            broken = player.gameMode.destroyBlock(target);
        } finally {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() == tool.getItem() && held.isDamageableItem()) {
                held.setDamageValue(damageBefore);
            }
        }
        if (broken) player.getPersistentData().putInt(WEAR_BANK_KEY, bank + 1);
        return broken;
    }
}
