package dev.moonseungjun.augmentationsurvival.augment.active;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Enforces the time-stop invariant at the final server damage boundary. */
public final class TimeStopIntegrityService {
    private static boolean initialized;

    private TimeStopIntegrityService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (target, source, amount) -> !isBlockedByTimeStop(source)
        );
    }

    public static boolean isBlockedByTimeStop(DamageSource source) {
        if (source == null) return false;
        Entity causing = source.getEntity();
        Entity direct = source.getDirectEntity();
        return isFrozenSource(causing)
                || isFrozenSource(direct)
                || SpecialActiveAugmentService.isHostileSourceInsideActiveTimeStopZone(causing)
                || SpecialActiveAugmentService.isHostileSourceInsideActiveTimeStopZone(direct);
    }

    private static boolean isFrozenSource(Entity entity) {
        if (entity instanceof LivingEntity living && SpecialActiveAugmentService.isTimeStopped(living)) {
            return true;
        }
        return entity instanceof Projectile projectile
                && SpecialActiveAugmentService.isProjectileTimeStopped(projectile);
    }
}
