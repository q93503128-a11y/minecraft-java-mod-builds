package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.ResourceHarvestService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Continuous Mining counts only player-root block breaks, never derived tree/tunnel/sweep breaks. */
@Mixin(value = SurvivalAugmentService.class, remap = false)
abstract class SurvivalAugmentDerivedBreakGuardMixin {
    @Inject(method = "onBlockBroken", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$ignoreDerivedBreaks(ServerPlayer player, CallbackInfo ci) {
        if (ResourceHarvestService.isDerivedBreak()) {
            ci.cancel();
        }
    }
}
