package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationGrowthSampler;
import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers the smoothed crop-growth sampler with the Area Acceleration runtime. */
@Mixin(value = AreaAccelerationService.class, remap = false)
abstract class AreaAccelerationGrowthSamplerBootstrapMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$initializeGrowthSampler(CallbackInfo ci) {
        AreaAccelerationGrowthSampler.initialize();
    }
}
