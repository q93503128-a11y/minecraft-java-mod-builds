package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Broadens the mythic Time Stop from hostile-only control to the whole dynamic sphere. */
@Mixin(value = SpecialActiveAugmentService.class, remap = false)
abstract class TimeStopAllTargetsMixin {
    @Inject(method = "applyTimeStopField", at = @At("TAIL"))
    private static void augmentationSurvival$freezeWholeSphere(
            ServerPlayer player,
            int ticks,
            CallbackInfo ci
    ) {
        ServerLevel level = player.level();
        double radius = SpecialActiveTuning.current().timeStopRadius();
        double radiusSquared = radius * radius;

        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate != player
                        && player.distanceToSqr(candidate) <= radiusSquared
                        && EntityCompatibilityProfileService.allowTimeStop(candidate)
        )) {
            SpecialActiveAugmentServiceInvoker.augmentationSurvival$applyTimeStop(candidate, ticks);
        }

        for (Projectile projectile : level.getEntitiesOfClass(
                Projectile.class,
                player.getBoundingBox().inflate(radius),
                projectile -> player.distanceToSqr(projectile) <= radiusSquared
        )) {
            SpecialActiveAugmentServiceInvoker.augmentationSurvival$freezeProjectile(projectile, ticks);
        }
    }
}
