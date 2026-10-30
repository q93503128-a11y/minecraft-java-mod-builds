package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.WaterRunSprintIntentService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures Water Run sprint intent at the earliest authoritative ServerPlayer tick boundary. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerWaterRunSprintIntentMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void augmentationSurvival$captureWaterRunSprintIntent(CallbackInfo ci) {
        WaterRunSprintIntentService.capture((ServerPlayer) (Object) this);
    }
}
