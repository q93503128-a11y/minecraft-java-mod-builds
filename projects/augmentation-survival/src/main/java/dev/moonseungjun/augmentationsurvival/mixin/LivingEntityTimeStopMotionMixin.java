package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps scripted/Brain-driven hostile movement from escaping a server-authoritative time stop. */
@Mixin(LivingEntity.class)
abstract class LivingEntityTimeStopMotionMixin {
    @Unique private Vec3 augmentationSurvival$timeStopAnchor;

    @Inject(method = "tick", at = @At("HEAD"))
    private void augmentationSurvival$captureTimeStopAnchor(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (SpecialActiveAugmentService.isTimeStopped(self)) {
            if (augmentationSurvival$timeStopAnchor == null) {
                augmentationSurvival$timeStopAnchor = self.position();
            }
        } else {
            augmentationSurvival$timeStopAnchor = null;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void augmentationSurvival$restoreTimeStopAnchor(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || augmentationSurvival$timeStopAnchor == null
                || !SpecialActiveAugmentService.isTimeStopped(self)) return;
        self.setPos(
                augmentationSurvival$timeStopAnchor.x,
                augmentationSurvival$timeStopAnchor.y,
                augmentationSurvival$timeStopAnchor.z
        );
        self.setDeltaMovement(Vec3.ZERO);
    }
}
