package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.ShieldThrowFlightService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the legacy instant Shield Throw loop with the physical flight runtime. */
@Mixin(value = AdvancedActiveAugmentService.class, remap = false)
abstract class ShieldThrowFlightMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$initializeShieldFlight(CallbackInfo ci) {
        ShieldThrowFlightService.initialize();
    }

    @Inject(method = "tryShieldThrow", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$replaceShieldThrow(
            ServerPlayer player,
            double efficiency,
            boolean manualUse,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(ShieldThrowFlightService.tryUse(player, efficiency, manualUse));
    }
}
