package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freezes the common projectile tick path during the mythic time-stop window. */
@Mixin(Projectile.class)
abstract class ProjectileTimeStopMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$freezeProjectileTick(CallbackInfo ci) {
        if (SpecialActiveAugmentService.isProjectileTimeStopped((Projectile) (Object) this)) {
            ci.cancel();
        }
    }
}
