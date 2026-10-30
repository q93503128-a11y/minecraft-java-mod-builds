package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.ClientWaterRunSprintIntentState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures Water Run sprint intent before LocalPlayer movement prediction can clear sprint at a fluid edge. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerWaterRunSprintIntentMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void augmentationSurvival$captureWaterRunSprintIntent(CallbackInfo ci) {
        ClientWaterRunSprintIntentState.capture((LocalPlayer) (Object) this);
    }
}
