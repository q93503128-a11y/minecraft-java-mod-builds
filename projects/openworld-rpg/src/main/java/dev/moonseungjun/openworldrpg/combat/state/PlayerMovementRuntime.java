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
    static final Identifier ACTION_MOVEMENT_SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "action_movement_speed"
            );
    static final Identifier CLASS_MOVEMENT_SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "class_movement_speed"
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

    public static void synchronizeClassMovementSpeedBonus(
            Player player,
            double classMovementSpeedBonus
    ) {
        Objects.requireNonNull(player, "player");
        if (player.level().isClientSide()) {
            throw new IllegalStateException(
                    "Project movement authority is server-only."
            );
        }
        if (!Double.isFinite(classMovementSpeedBonus)
                || classMovementSpeedBonus < 0.0
                || classMovementSpeedBonus > 1.0) {
            throw new IllegalArgumentException(
                    "Class movement-speed bonus must be inside [0, 1]."
            );
        }
        var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            throw new IllegalStateException(
                    "Server player has no MOVEMENT_SPEED attribute."
            );
        }
        movementSpeed.removeModifier(CLASS_MOVEMENT_SPEED_MODIFIER_ID);
        if (classMovementSpeedBonus > 0.0) {
            movementSpeed.addOrUpdateTransientModifier(
                    new AttributeModifier(
                            CLASS_MOVEMENT_SPEED_MODIFIER_ID,
                            classMovementSpeedBonus,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );
        }
    }

    public static void synchronizeActionMovementMultiplier(
            Player player,
            double multiplier
    ) {
        Objects.requireNonNull(player, "player");
        if (player.level().isClientSide()) {
            throw new IllegalStateException(
                    "Project action movement authority is server-only."
            );
        }
        if (!Double.isFinite(multiplier)
                || multiplier <= 0.0
                || multiplier > 1.0) {
            throw new IllegalArgumentException(
                    "Action movement multiplier must be inside (0, 1]."
            );
        }

        var movementSpeed = player.getAttribute(
                Attributes.MOVEMENT_SPEED
        );
        if (movementSpeed == null) {
            throw new IllegalStateException(
                    "Server player has no MOVEMENT_SPEED attribute."
            );
        }

        movementSpeed.removeModifier(
                ACTION_MOVEMENT_SPEED_MODIFIER_ID
        );
        if (multiplier < 1.0) {
            movementSpeed.addOrUpdateTransientModifier(
                    new AttributeModifier(
                            ACTION_MOVEMENT_SPEED_MODIFIER_ID,
                            multiplier - 1.0,
                            AttributeModifier.Operation
                                    .ADD_MULTIPLIED_TOTAL
                    )
            );
        }
    }
}
