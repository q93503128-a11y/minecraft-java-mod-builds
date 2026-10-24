package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeCarrier;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowAugmentMixin implements ProjectileRuntimeCarrier {
    @Unique
    private ProjectileRuntimeState augmentationSurvival$projectileRuntime;

    @Override
    public ProjectileRuntimeState augmentationSurvival$getProjectileRuntime() {
        return augmentationSurvival$projectileRuntime;
    }

    @Override
    public void augmentationSurvival$setProjectileRuntime(ProjectileRuntimeState state) {
        augmentationSurvival$projectileRuntime = state;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void augmentationSurvival$beforeProjectileTick(CallbackInfo ci) {
        ProjectileAugmentService.beforeArrowTick((AbstractArrow) (Object) this);
    }

    @Redirect(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean augmentationSurvival$applyProjectileDamage(
            Entity target,
            DamageSource source,
            float damage
    ) {
        return ProjectileAugmentService.hurtFromArrow(
                (AbstractArrow) (Object) this,
                target,
                source,
                damage
        );
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$ricochet(
            BlockHitResult hitResult,
            CallbackInfo ci
    ) {
        if (ProjectileAugmentService.tryRicochet(
                (AbstractArrow) (Object) this,
                hitResult
        )) {
            ci.cancel();
        }
    }
}
