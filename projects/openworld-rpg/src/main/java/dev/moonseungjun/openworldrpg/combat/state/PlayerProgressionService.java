package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;

/**
 * Server-owned mutation/read API for persistent combat Lv, XP, Class Rank/XP and Attributes.
 */
public final class PlayerProgressionService {
    private PlayerProgressionService() {
    }

    public static PlayerProgressionState state(Player player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerProgressionAttachments.COMBAT_PROGRESSION,
                PlayerProgressionState.initial()
        );
    }

    public static PlayerProgressionState setCombatLevel(Player player, int combatLevel) {
        return replace(player, state(player).withCombatLevel(combatLevel));
    }

    public static PlayerProgressionState selectClass(Player player, RootClass rootClass) {
        return replace(player, state(player).withActiveClass(rootClass));
    }

    public static PlayerProgressionState setAllocation(
            Player player,
            RootClass rootClass,
            AttributeAllocation allocation
    ) {
        return replace(player, state(player).withAllocation(rootClass, allocation));
    }

    public static PlayerProgressionState creditCombatXpOnce(
            Player player,
            String transactionId,
            long amount
    ) {
        return replace(
                player,
                state(player).grantCombatXpOnce(transactionId, amount)
        );
    }

    public static PlayerProgressionState creditClassXpOnce(
            Player player,
            String transactionId,
            RootClass rewardClass,
            long amount
    ) {
        return replace(
                player,
                state(player).grantClassXpOnce(transactionId, rewardClass, amount)
        );
    }

    public static Optional<PlayerCombatBuildState> buildWith(
            Player player,
            EquipmentCombatState equipment
    ) {
        return state(player).buildWith(equipment);
    }

    private static PlayerProgressionState replace(
            Player player,
            PlayerProgressionState next
    ) {
        PlayerProgressionState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(PlayerProgressionAttachments.COMBAT_PROGRESSION, next);
        PlayerCombatBuildPublisher.refresh(player);
        return next;
    }
}
