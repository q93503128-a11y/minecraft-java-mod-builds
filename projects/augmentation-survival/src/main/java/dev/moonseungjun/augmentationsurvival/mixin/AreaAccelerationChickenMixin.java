package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Accelerates timed egg production only; chicken movement/goal AI still ticks once per game tick. */
@Mixin(Chicken.class)
abstract class AreaAccelerationChickenMixin {
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void augmentationSurvival$accelerateEggTimer(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;
        if (!(chicken.level() instanceof ServerLevel level)
                || chicken.isBaby()
                || chicken.isChickenJockey
                || chicken.eggTime <= 1) {
            return;
        }
        int extra = AreaAccelerationService.extraTicksAt(level, chicken.position());
        if (extra > 0) {
            chicken.eggTime = Math.max(1, chicken.eggTime - extra);
        }
    }
}
