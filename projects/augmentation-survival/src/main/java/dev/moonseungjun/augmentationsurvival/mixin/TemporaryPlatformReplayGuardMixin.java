package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformService;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replay is a combat-active mechanic; rescue platforms must never schedule a duplicate cast. */
@Mixin(value = TemporaryPlatformService.class, remap = false)
abstract class TemporaryPlatformReplayGuardMixin {
    @Shadow @Final private static Map<UUID, ?> REPLAYS;

    @Inject(method = "tryUse", at = @At("RETURN"))
    private static void augmentationSurvival$removeInvalidPlatformReplay(
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) {
            REPLAYS.remove(player.getUUID());
        }
    }
}
