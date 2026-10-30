package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActivePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Periodically reasserts the server-owned Area Acceleration timer and spherical boundary. */
@Mixin(value = AreaAccelerationService.class, remap = false)
abstract class AreaAccelerationPresentationSyncMixin {
    private static final String AREA_ACCELERATION_ID =
            AugmentationSurvival.MOD_ID + ":area_acceleration";

    @Inject(method = "tick", at = @At("TAIL"))
    private static void augmentationSurvival$resyncAreaAccelerationPresentation(
            MinecraftServer server,
            CallbackInfo ci
    ) {
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int remaining = AreaAccelerationService.remainingTicks(player);
            if (remaining <= 0) continue;
            ActivePresentationService.notifyState(
                    player,
                    AREA_ACCELERATION_ID,
                    ActivePresentationPayload.SUSTAINED,
                    remaining,
                    AreaAccelerationService.radius()
            );
        }
    }
}
