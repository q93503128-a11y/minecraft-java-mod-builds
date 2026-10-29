package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileAugmentService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scopes tracking/chain target acquisition to non-allies instead of hostile-marker-only targets. */
@Mixin(value = ProjectileAugmentService.class, remap = false)
abstract class ProjectileOffensiveTargetContextMixin {
    @Inject(method = "beforeArrowTick", at = @At("HEAD"))
    private static void augmentationSurvival$pushTrackingContext(AbstractArrow arrow, CallbackInfo ci) {
        OffensiveTargetContext.push();
    }

    @Inject(method = "beforeArrowTick", at = @At("RETURN"))
    private static void augmentationSurvival$popTrackingContext(AbstractArrow arrow, CallbackInfo ci) {
        OffensiveTargetContext.pop();
    }

    @Inject(method = "afterDamage", at = @At("HEAD"))
    private static void augmentationSurvival$pushChainContext(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked, CallbackInfo ci
    ) {
        OffensiveTargetContext.push();
    }

    @Inject(method = "afterDamage", at = @At("RETURN"))
    private static void augmentationSurvival$popChainContext(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked, CallbackInfo ci
    ) {
        OffensiveTargetContext.pop();
    }
}
