package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.ActivePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveRangeTelegraphService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirrors the authoritative presentation state into world-space range boundaries. */
@Mixin(ActivePresentationService.class)
abstract class ActivePresentationRangeTelegraphMixin {
    @Inject(method = "sendState", at = @At("TAIL"))
    private static void augmentationSurvival$trackRangeState(
            ServerPlayer player,
            String abilityId,
            int phase,
            int durationTicks,
            float radius,
            CallbackInfo ci
    ) {
        ActiveRangeTelegraphService.onState(player, abilityId, phase, durationTicks, radius);
    }
}
