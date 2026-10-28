package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.AugmentationSurvivalClient;
import dev.moonseungjun.augmentationsurvival.client.ClientInstantMiningState;
import dev.moonseungjun.augmentationsurvival.client.FlightInputClient;
import dev.moonseungjun.augmentationsurvival.client.InstantMiningInputClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers dedicated rebindable movement and utility inputs after the normal client bootstrap. */
@Mixin(value = AugmentationSurvivalClient.class, remap = false)
abstract class ClientCoopMobilityBootstrapMixin {
    @Inject(method = "onInitializeClient", at = @At("TAIL"))
    private void augmentationSurvival$initializeDedicatedInputs(CallbackInfo ci) {
        FlightInputClient.initialize();
        ClientInstantMiningState.initialize();
        InstantMiningInputClient.initialize();
    }
}
