package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.AugmentationSurvivalClient;
import dev.moonseungjun.augmentationsurvival.client.FlightInputClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers the dedicated superhero-flight input with the existing client bootstrap. */
@Mixin(value = AugmentationSurvivalClient.class, remap = false)
abstract class ClientCoopMobilityBootstrapMixin {
    @Inject(method = "onInitializeClient", at = @At("TAIL"))
    private void augmentationSurvival$initializeFlightInput(CallbackInfo ci) {
        FlightInputClient.initialize();
    }
}
