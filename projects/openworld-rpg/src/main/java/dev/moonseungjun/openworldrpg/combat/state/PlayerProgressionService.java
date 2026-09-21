package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;

/**
 * Server-owned mutation/read API for persistent combat Lv and per-class Attribute allocations.
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

    public static Optional<PlayerCombatBuildState> buildWith(
            Player player,
            EquipmentCombatState equipment
    ) {
        return state(player).buildWith(equipment);
    }

    private static PlayerProgressionState replace(
            Player player,
            PlayerProgressionState state
    ) {
        player.setAttached(PlayerProgressionAttachments.COMBAT_PROGRESSION, state);
        PlayerCombatBuildPublisher.refresh(player);
        return state;
    }
}
