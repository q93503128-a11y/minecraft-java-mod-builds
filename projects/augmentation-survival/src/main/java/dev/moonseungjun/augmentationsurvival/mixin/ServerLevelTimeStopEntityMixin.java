package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.TimeStopBoundaryService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops ordinary world entities such as drops, XP, vehicles, TNT and falling blocks while they are
 * inside Time Stop. Living entities and projectiles keep their dedicated restore-aware paths.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelTimeStopEntityMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$haltWorldEntity(Entity entity, CallbackInfo ci) {
        if (entity instanceof LivingEntity || entity instanceof Projectile) return;
        if (TimeStopBoundaryService.freezesWorldEntity(entity)) {
            ci.cancel();
        }
    }
}
