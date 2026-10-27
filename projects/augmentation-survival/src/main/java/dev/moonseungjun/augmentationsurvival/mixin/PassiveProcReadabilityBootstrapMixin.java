package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.presentation.ConditionalPassiveReadabilityService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveProcReadabilityService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers lightweight proc feedback after gameplay services have installed their authoritative listeners. */
@Mixin(value = AugmentationSurvival.class, remap = false)
abstract class PassiveProcReadabilityBootstrapMixin {
    @Inject(method = "onInitialize", at = @At("TAIL"))
    private void augmentationSurvival$initializePassiveProcReadability(CallbackInfo ci) {
        PassiveProcReadabilityService.initialize();
        ConditionalPassiveReadabilityService.initialize();
    }
}
