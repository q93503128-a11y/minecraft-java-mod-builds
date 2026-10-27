package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.ClientPhaseState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla client wall-ejection fights phase collision bypass and causes visible back-and-forth jitter. */
@Mixin(LocalPlayer.class)
abstract class ClientPhaseShiftPushOutMixin {
    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$skipWallPushDuringPhase(double x, double z, CallbackInfo ci) {
        if (ClientPhaseState.phaseShiftActive()) {
            ci.cancel();
        }
    }
}
