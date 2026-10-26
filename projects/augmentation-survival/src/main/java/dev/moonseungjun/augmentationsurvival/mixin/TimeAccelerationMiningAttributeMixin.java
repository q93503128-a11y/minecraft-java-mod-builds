package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps Time Acceleration off the raw block-break-speed attribute so all Augmentation Survival
 * mining bonuses can be combined additively at the shared destroy-speed boundary.
 */
@Mixin(value = AdvancedActiveAugmentService.class, remap = false)
abstract class TimeAccelerationMiningAttributeMixin {
    private static final Identifier LEGACY_TIME_MINING_MODIFIER = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "active/time_acceleration_mining"
    );

    @Inject(method = "applyTimeAccelerationModifiers", at = @At("TAIL"))
    private static void augmentationSurvival$removeLegacyMiningAttribute(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        AttributeInstance instance = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (instance != null) {
            instance.removeModifier(LEGACY_TIME_MINING_MODIFIER);
        }
    }
}
