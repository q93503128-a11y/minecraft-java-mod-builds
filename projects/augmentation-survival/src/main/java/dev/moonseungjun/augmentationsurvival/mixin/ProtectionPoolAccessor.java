package dev.moonseungjun.augmentationsurvival.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only accessor for one server protection pool. */
@Mixin(targets = "dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService$ProtectionPool")
public interface ProtectionPoolAccessor {
    @Accessor("amount")
    float augmentationSurvival$getAmount();

    @Accessor("effectiveAmount")
    float augmentationSurvival$getEffectiveAmount();
}
