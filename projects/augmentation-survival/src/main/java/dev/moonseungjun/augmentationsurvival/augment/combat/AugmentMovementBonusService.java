package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Reads only movement-speed bonuses authored by augmentation runtimes. */
public final class AugmentMovementBonusService {
    private static final List<Identifier> MOVEMENT_MODIFIERS = List.of(
            id("augment_stat/movement_speed/add_multiplied_base"),
            id("augment_movement/conditional_speed"),
            id("augment_dynamic/movement_speed"),
            id("augment_mobility/ride_current"),
            id("time_acceleration_movement")
    );

    private AugmentMovementBonusService() { }

    /**
     * Returns the current positive augmentation-owned movement bonus as a base multiplier fraction.
     * Vanilla sprint, potion effects, equipment and unrelated external modifiers are excluded.
     */
    public static double positiveMovementBonus(ServerPlayer player) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) return 0.0D;

        double total = 0.0D;
        for (Identifier modifierId : MOVEMENT_MODIFIERS) {
            AttributeModifier modifier = movement.getModifier(modifierId);
            if (modifier == null || modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                continue;
            }
            double amount = modifier.amount();
            if (Double.isFinite(amount) && amount > 0.0D) {
                total += amount;
            }
        }
        return Math.max(0.0D, total);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, path);
    }
}
