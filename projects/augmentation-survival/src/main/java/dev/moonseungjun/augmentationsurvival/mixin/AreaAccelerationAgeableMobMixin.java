package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Accelerates livestock baby growth and breeding cooldown without replaying animal AI. */
@Mixin(AgeableMob.class)
abstract class AreaAccelerationAgeableMobMixin {
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void augmentationSurvival$accelerateLivestockAge(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof Animal animal) || !(animal.level() instanceof ServerLevel level)) {
            return;
        }
        int extra = AreaAccelerationService.extraTicksAt(level, animal.position());
        if (extra <= 0) {
            return;
        }
        int age = animal.getAge();
        if (age < 0) {
            animal.setAge(Math.min(0, age + extra));
        } else if (age > 0) {
            animal.setAge(Math.max(0, age - extra));
        }
    }
}
