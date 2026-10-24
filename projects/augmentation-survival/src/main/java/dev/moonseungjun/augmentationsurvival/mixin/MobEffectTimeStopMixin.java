package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents active status-effect duration/effect ticks while the target is time-stopped. */
@Mixin(MobEffectInstance.class)
abstract class MobEffectTimeStopMixin {
    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$freezeEffectTimer(
            ServerLevel level,
            LivingEntity target,
            Runnable onEffectUpdate,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (SpecialActiveAugmentService.isTimeStopped(target)) {
            cir.setReturnValue(true);
        }
    }
}
