package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Objects;
import net.minecraft.world.entity.player.Player;

/**
 * Server-owned mutation/read API for the persistent 12-slot equipped loadout.
 */
public final class PlayerEquipmentService {
    private PlayerEquipmentService() {
    }

    public static PlayerEquipmentLoadoutState state(Player player) {
        requireServer(player);
        return player.getAttachedOrSet(
                PlayerEquipmentAttachments.EQUIPPED_LOADOUT,
                PlayerEquipmentLoadoutState.empty()
        );
    }

    public static PlayerEquipmentLoadoutState equip(
            Player player,
            EquippedCombatItem item
    ) {
        Objects.requireNonNull(item, "item");
        return replace(player, state(player).withEquipped(item));
    }

    public static PlayerEquipmentLoadoutState unequip(
            Player player,
            ProjectEquipmentSlot slot
    ) {
        Objects.requireNonNull(slot, "slot");
        return replace(player, state(player).without(slot));
    }

    private static PlayerEquipmentLoadoutState replace(
            Player player,
            PlayerEquipmentLoadoutState state
    ) {
        requireServer(player);
        player.setAttached(PlayerEquipmentAttachments.EQUIPPED_LOADOUT, state);
        PlayerCombatBuildPublisher.refresh(player);
        return state;
    }

    private static void requireServer(Player player) {
        Objects.requireNonNull(player, "player");
        if (player.level().isClientSide()) {
            throw new IllegalStateException("Project equipment authority is server-only.");
        }
    }
}
