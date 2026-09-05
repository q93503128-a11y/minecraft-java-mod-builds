package kr.moonseungjun.survivalascension.progress;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Skill-expanded work keeps vanilla block-break hooks and still wears tools, but automatic
 * extra blocks only pay one normal vanilla durability roll per four successful extra blocks.
 * The player's original manual break always remains vanilla-authoritative.
 */
public final class AutomatedToolBreak {
    private static final String WEAR_BANK_KEY = "survivalascension_bulk_tool_wear_bank";
    private static final String WEAR_TOOL_KEY = "survivalascension_bulk_tool_wear_tool";
    private static final int AUTOMATIC_BLOCKS_PER_WEAR = 4;

    private AutomatedToolBreak() {}

    public record TimedBreakResult(boolean broken, long bookkeepingNanos, long destroyPipelineNanos) {}

    public static boolean destroyWithReducedWear(ServerPlayer player, BlockPos target) {
        return destroyWithReducedWearTimed(player, target).broken();
    }

    /**
     * Same mutation path as destroyWithReducedWear, with nanosecond accounting around the one
     * ServerPlayerGameMode.destroyBlock call. The destroy bucket intentionally contains all vanilla
     * and NeoForge break semantics rather than bypassing them for a synthetic fast path.
     */
    public static TimedBreakResult destroyWithReducedWearTimed(ServerPlayer player, BlockPos target) {
        long start = System.nanoTime();
        long destroyNanos = 0L;
        ItemStack tool = player.getMainHandItem();
        if (player.isCreative() || tool.isEmpty() || !tool.isDamageableItem()) {
            long destroyStart = System.nanoTime();
            boolean broken = player.gameMode.destroyBlock(target);
            destroyNanos = Math.max(0L, System.nanoTime() - destroyStart);
            return timed(broken, start, destroyNanos);
        }

        String toolId = BuiltInRegistries.ITEM.getKey(tool.getItem()).toString();
        String bankToolId = player.getPersistentData().getStringOr(WEAR_TOOL_KEY, "");
        if (!toolId.equals(bankToolId)) {
            player.getPersistentData().putString(WEAR_TOOL_KEY, toolId);
            player.getPersistentData().putInt(WEAR_BANK_KEY, 0);
        }
        int bank = Math.max(0, player.getPersistentData().getIntOr(WEAR_BANK_KEY, 0));
        if (bank >= AUTOMATIC_BLOCKS_PER_WEAR - 1) {
            long destroyStart = System.nanoTime();
            boolean broken = player.gameMode.destroyBlock(target);
            destroyNanos = Math.max(0L, System.nanoTime() - destroyStart);
            if (broken) player.getPersistentData().putInt(WEAR_BANK_KEY, 0);
            return timed(broken, start, destroyNanos);
        }

        int damageBefore = tool.getDamageValue();
        tool.setDamageValue(0);
        boolean broken;
        try {
            long destroyStart = System.nanoTime();
            broken = player.gameMode.destroyBlock(target);
            destroyNanos = Math.max(0L, System.nanoTime() - destroyStart);
        } finally {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() == tool.getItem() && held.isDamageableItem()) {
                held.setDamageValue(damageBefore);
            }
        }
        if (broken) player.getPersistentData().putInt(WEAR_BANK_KEY, bank + 1);
        return timed(broken, start, destroyNanos);
    }

    private static TimedBreakResult timed(boolean broken, long start, long destroyNanos) {
        long total = Math.max(0L, System.nanoTime() - start);
        return new TimedBreakResult(broken, Math.max(0L, total - destroyNanos), destroyNanos);
    }
}
