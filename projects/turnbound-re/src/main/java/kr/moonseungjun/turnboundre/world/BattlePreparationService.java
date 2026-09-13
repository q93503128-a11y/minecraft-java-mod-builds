package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.battle.BattlePreparationBonus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Maps one explicitly selected vanilla offhand material to one battle-local preparation.
 * It never searches the inventory and never converts materials into TURNBOUND currencies.
 */
public final class BattlePreparationService {
    public static final Selection NONE = new Selection("", 0, BattlePreparationBonus.NONE);
    public static final Selection IRON_REINFORCEMENT = new Selection(
            "iron_reinforcement", 1,
            new BattlePreparationBonus("iron_reinforcement", 0, 0, 8, 8));
    public static final Selection GOLDEN_PROVISION = new Selection(
            "golden_provision", 1,
            new BattlePreparationBonus("golden_provision", 10, 0, 0, 0));
    public static final Selection COOKED_COD_RATION = new Selection(
            "cooked_cod_ration", 1,
            new BattlePreparationBonus("cooked_cod_ration", 0, 8, 0, 0));
    public static final Selection COOKED_SALMON_RATION = new Selection(
            "cooked_salmon_ration", 1,
            new BattlePreparationBonus("cooked_salmon_ration", 0, 8, 0, 0));

    private BattlePreparationService() {}

    public record Selection(String id, int consumeCount, BattlePreparationBonus bonus) {
        public Selection {
            if (id == null) throw new IllegalArgumentException("selection id must not be null");
            if (consumeCount < 0) throw new IllegalArgumentException("consumeCount must be >= 0");
            if (bonus == null) throw new IllegalArgumentException("bonus required");
            if (id.isBlank()) {
                if (consumeCount != 0 || bonus.active()) {
                    throw new IllegalArgumentException("NONE preparation cannot consume or apply a bonus");
                }
            } else {
                if (consumeCount <= 0) throw new IllegalArgumentException("active preparation must consume an item");
                if (!id.equals(bonus.id())) throw new IllegalArgumentException("selection and bonus ids must match");
            }
        }

        public boolean active() {
            return !id.isBlank();
        }
    }

    public static Selection preview(ServerPlayer player) {
        if (player == null) return NONE;
        return resolve(player.getOffhandItem());
    }

    public static Selection resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return NONE;
        return resolveItem(stack.getItem(), stack.getCount());
    }

    static Selection resolveItem(Item item, int count) {
        if (item == null || count <= 0) return NONE;
        if (item == Items.IRON_INGOT) return IRON_REINFORCEMENT;
        if (item == Items.GOLDEN_CARROT) return GOLDEN_PROVISION;
        if (item == Items.COOKED_COD) return COOKED_COD_RATION;
        if (item == Items.COOKED_SALMON) return COOKED_SALMON_RATION;
        return NONE;
    }

    public static boolean matchesExpected(String expectedId, Selection current) {
        String expected = expectedId == null ? "" : expectedId;
        Selection safeCurrent = current == null ? NONE : current;
        return expected.equals(safeCurrent.id());
    }

    /**
     * Consumes only the currently selected offhand stack. Call this only after all launch validation succeeds.
     */
    public static boolean consumeOffhand(ServerPlayer player, Selection expected) {
        if (player == null || expected == null) return false;
        ItemStack stack = player.getOffhandItem();
        Selection current = resolve(stack);
        if (!current.id().equals(expected.id())) return false;
        if (!expected.active()) return true;
        if (stack.getCount() < expected.consumeCount()) return false;
        stack.shrink(expected.consumeCount());
        return true;
    }
}
