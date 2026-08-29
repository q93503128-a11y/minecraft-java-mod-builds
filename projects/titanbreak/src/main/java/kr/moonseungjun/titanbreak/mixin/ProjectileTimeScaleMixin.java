package kr.moonseungjun.titanbreak.mixin;

import kr.moonseungjun.titanbreak.combat.ReflexFieldService;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileTimeScaleMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void titanbreak$scaleProjectileVelocity(CallbackInfo ci) {
        ReflexFieldService.applyProjectileTimeScale((Projectile) (Object) this);
    }
}
