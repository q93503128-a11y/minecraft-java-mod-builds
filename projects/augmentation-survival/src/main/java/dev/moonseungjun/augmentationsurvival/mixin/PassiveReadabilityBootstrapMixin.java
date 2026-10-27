package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveReadabilityService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Starts world-space readability tracking without routing passive states through the HUD. */
@Mixin(value = AugmentationSurvival.class, remap = false)
abstract class PassiveReadabilityBootstrapMixin {
    @Inject(method = "onInitialize", at = @At("TAIL"))
    private void augmentationSurvival$initializePassiveReadability(CallbackInfo ci) {
        PassiveReadabilityService.initialize();
    }
}
