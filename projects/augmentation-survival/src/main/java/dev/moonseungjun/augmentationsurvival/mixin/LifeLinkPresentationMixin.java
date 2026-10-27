package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveReadabilityService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Presents the actual linked enemy group after the server has rebuilt Life Link. */
@Mixin(value = FinalAugmentRuntimeService.class, remap = false)
abstract class LifeLinkPresentationMixin {
    @Inject(method = "buildLifeLink", at = @At("TAIL"))
    private static void augmentationSurvival$presentLifeLink(
            ServerPlayer owner,
            LivingEntity primary,
            CallbackInfo ci
    ) {
        PassiveReadabilityService.presentLifeLink(owner, primary);
    }
}
