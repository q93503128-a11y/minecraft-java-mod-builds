package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Server-owned projection of canonical equipment Movement Speed onto Minecraft movement.
 *
 * <p>The project loadout remains authoritative. A stable transient modifier preserves vanilla
 * movement handling and other attribute sources without rewriting the player's base speed.</p>
 */
public final class PlayerMovementRuntime {
    static final Identifier EQUIPMENT_MOVEMENT_SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "equipment_movement_speed"
            );

    private PlayerMovementRuntime() {
    }

    public static void synchronize(
            Player player,
            double equipmentMovementSpeedBonus
    ) {
        Objects.requireNonNull(player, "player");
        if (player.level().isClientSide()) {
            throw new IllegalStateException(
                    "Project movement authority is server-only."
            );
        }
        if (!Double.isFinite(equipmentMovementSpeedBonus)
                || equipmentMovementSpeedBonus < 0.0
                || equipmentMovementSpeedBonus
                        > PlayerEquipmentLoadoutState.MOVEMENT_SPEED_GEAR_CAP) {
            throw new IllegalArgumentException(
                    "equipmentMovementSpeedBonus must be inside the canonical gear cap [0, 0.15]."
            );
        }

        var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            throw new IllegalStateException(
                    "Server player has no MOVEMENT_SPEED attribute."
            );
        }

        movementSpeed.removeModifier(EQUIPMENT_MOVEMENT_SPEED_MODIFIER_ID);
        if (equipmentMovementSpeedBonus > 0.0) {
            movementSpeed.addOrUpdateTransientModifier(
                    new AttributeModifier(
                            EQUIPMENT_MOVEMENT_SPEED_MODIFIER_ID,
                            equipmentMovementSpeedBonus,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );
        }
    }
}
