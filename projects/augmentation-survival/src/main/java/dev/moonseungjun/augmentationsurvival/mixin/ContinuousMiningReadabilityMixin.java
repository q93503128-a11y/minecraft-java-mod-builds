package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.presentation.ConditionalPassiveReadabilityService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirrors the confirmed Continuous Mining stack edge after the authoritative server update. */
@Mixin(value = SurvivalAugmentService.class, remap = false)
public abstract class ContinuousMiningReadabilityMixin {
    @Inject(method = "onBlockBroken", at = @At("TAIL"))
    private static void augmentationSurvival$presentContinuousMiningStack(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        ConditionalPassiveReadabilityService.presentContinuousMiningBreak(player);
    }
}
