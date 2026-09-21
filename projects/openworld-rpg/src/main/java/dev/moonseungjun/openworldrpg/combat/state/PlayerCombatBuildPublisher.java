package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Optional;
import net.minecraft.world.entity.player.Player;

/**
 * Rebuilds the transient combat build from persistent project-owned progression + equipped loadout.
 */
public final class PlayerCombatBuildPublisher {
    private PlayerCombatBuildPublisher() {
    }

    public static Optional<PlayerCombatBuildState> refresh(Player player) {
        if (player.level().isClientSide()) {
            return Optional.empty();
        }

        Optional<EquipmentCombatState> equipment =
                PlayerEquipmentService.state(player).aggregateCombatState();
        Optional<PlayerCombatBuildState> build = equipment.flatMap(
                combatEquipment -> PlayerProgressionService.state(player).buildWith(combatEquipment)
        );

        if (build.isPresent()) {
            CombatStateServices.combatBuilds().bindAuthoritative(player.getUUID(), build.get());
        } else {
            CombatStateServices.combatBuilds().remove(player.getUUID());
        }
        return build;
    }
}
