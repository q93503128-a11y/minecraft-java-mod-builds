package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.network.FlightNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the co-op rescue and superhero-flight runtimes after the normal common bootstrap. */
@Mixin(value = AugmentationSurvival.class, remap = false)
abstract class CoopMobilityBootstrapMixin {
    @Inject(method = "onInitialize", at = @At("TAIL"))
    private void augmentationSurvival$initializeCoopMobility(CallbackInfo ci) {
        PlayerRescueAugmentService.initialize();
        FlightAugmentService.initialize();
        FlightNetworking.initialize();
    }
}
