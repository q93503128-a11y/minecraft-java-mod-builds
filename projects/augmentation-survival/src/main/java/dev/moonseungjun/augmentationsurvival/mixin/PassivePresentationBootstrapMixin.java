package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassivePresentationService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Starts the passive proc presentation queue with the normal mod bootstrap. */
@Mixin(value = AugmentationSurvival.class, remap = false)
abstract class PassivePresentationBootstrapMixin {
    @Inject(method = "onInitialize", at = @At("TAIL"))
    private void augmentationSurvival$initializePassivePresentation(CallbackInfo ci) {
        PassivePresentationService.initialize();
    }
}
