package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.TimeStopBoundaryService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the old hostile/moving-center field with the mythic contract: the cast origin is fixed,
 * the caster alone is exempt, and every compatible living entity/projectile inside is stopped.
 */
@Mixin(value = SpecialActiveAugmentService.class, remap = false)
abstract class TimeStopAllTargetsMixin {
    @Inject(method = "applyTimeStopField", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$freezeWholeFixedSphere(
            ServerPlayer player,
            int ticks,
            CallbackInfo ci
    ) {
        ci.cancel();
        if (ticks <= 0) return;

        TimeStopBoundaryService.ActiveField field = TimeStopBoundaryService.ensureActive(player);
        if (field == null || field.level() != player.level()) return;

        ServerLevel level = field.level();
        Vec3 center = field.center();
        double radius = SpecialActiveTuning.current().timeStopRadius();
        double radiusSquared = radius * radius;
        AABB bounds = new AABB(center, center).inflate(radius);

        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class,
                bounds,
                candidate -> candidate != player
                        && center.distanceToSqr(candidate.getBoundingBox().getCenter()) <= radiusSquared
                        && EntityCompatibilityProfileService.allowTimeStop(candidate)
        )) {
            SpecialActiveAugmentServiceInvoker.augmentationSurvival$applyTimeStop(candidate, ticks);
        }

        for (Projectile projectile : level.getEntitiesOfClass(
                Projectile.class,
                bounds,
                projectile -> !projectile.isRemoved()
                        && center.distanceToSqr(projectile.getBoundingBox().getCenter()) <= radiusSquared
        )) {
            SpecialActiveAugmentServiceInvoker.augmentationSurvival$freezeProjectile(projectile, ticks);
        }
    }
}
