package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.network.ActiveAbilityNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the loadout/charge HUD aligned when Time Acceleration commits its charge after focus. */
@Mixin(AdvancedActiveAugmentService.class)
abstract class AdvancedActiveLoadoutSyncMixin {
    @Inject(method = "syncTemporalState", at = @At("TAIL"))
    private static void augmentationSurvival$syncActiveLoadoutAfterTemporalState(
            ServerPlayer player,
            boolean active,
            CallbackInfo ci
    ) {
        ActiveAbilityNetworking.syncNow(player);
    }
}
