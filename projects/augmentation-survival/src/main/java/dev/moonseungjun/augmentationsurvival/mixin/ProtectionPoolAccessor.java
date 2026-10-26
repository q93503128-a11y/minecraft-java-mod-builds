package dev.moonseungjun.augmentationsurvival.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Narrow accessor for one server protection pool used by HUD and Overtime duration extension. */
@Mixin(targets = "dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService$ProtectionPool")
public interface ProtectionPoolAccessor {
    @Accessor("amount")
    float augmentationSurvival$getAmount();

    @Accessor("effectiveAmount")
    float augmentationSurvival$getEffectiveAmount();

    @Accessor("expiresAtTick")
    long augmentationSurvival$getExpiresAtTick();

    @Mutable
    @Accessor("expiresAtTick")
    void augmentationSurvival$setExpiresAtTick(long expiresAtTick);
}
