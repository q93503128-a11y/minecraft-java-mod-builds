package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pull targets nearby entities rather than only hostile living targets. */
@Mixin(value = ActiveAbilityService.class, remap = false)
abstract class ActivePullEntityMixin {
    @Inject(method = "executePull", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$pullNearbyEntities(
            ServerPlayer player,
            int level,
            double efficiency,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        double radius = tuning.pullRadius(level);
        double radiusSquared = radius * radius;
        List<Entity> targets = player.level().getEntities(
                player,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate != player
                        && candidate.isAlive()
                        && (!(candidate instanceof LivingEntity living)
                                || EntityCompatibilityProfileService.allowPull(living))
                        && player.distanceToSqr(candidate) <= radiusSquared
        );
        if (targets.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        boolean moved = false;
        for (Entity target : targets) {
            Vec3 delta = player.position().subtract(target.position());
            if (delta.lengthSqr() < 1.0E-6D) continue;
            double bossScalar = target instanceof LivingEntity living
                    && CombatExpansionService.isConfiguredBoss(living)
                    ? EntityCompatibilityProfileService.resolve(living)
                            .map(profile -> EntityCompatibilityProfileService.pullMultiplier(
                                    profile, tuning.pullBossScalar()))
                            .orElse(tuning.pullBossScalar())
                    : 1.0D;
            Vec3 impulse = delta.normalize().scale(tuning.pullSpeed() * efficiency * bossScalar);
            target.setDeltaMovement(target.getDeltaMovement().add(impulse));
            target.hurtMarked = true;
            moved = true;
        }
        cir.setReturnValue(moved);
    }
}
