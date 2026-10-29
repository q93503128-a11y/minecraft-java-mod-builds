package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeCarrier;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Return to Sender replaces the projectile that actually struck the shield instead of trying to
 * resurrect that collision object after vanilla has already resolved it.
 */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
public abstract class ReturnToSenderDeferralMixin {
    @Unique private static final String AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";

    @Inject(method = "tryReturnProjectile", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$replaceBlockedProjectile(
            ServerPlayer player,
            DamageSource source,
            CallbackInfo ci
    ) {
        if (!(source.getDirectEntity() instanceof Projectile original)
                || ReactiveCombatAugmentService.isReflectedProjectile(original)) {
            return;
        }
        double originalSpeed = original.getDeltaMovement().length();
        Vec3 direction = player.getLookAngle();
        int ownedLevel = AugmentationPlayerStateService.state(player)
                .level(AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID);
        if (!(originalSpeed > 1.0E-7D)
                || direction.lengthSqr() <= 1.0E-7D
                || ownedLevel <= 0
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        Entity created = original.getType().create(level, EntitySpawnReason.EVENT);
        if (!(created instanceof Projectile replacement)) {
            return;
        }

        int augmentLevel = Math.min(
                ownedLevel,
                AugmentCatalog.require(AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID).maxLevel()
        );
        double speed = originalSpeed
                * ReactiveCombatTuning.current().returnSpeedScale(augmentLevel);
        Vec3 aim = direction.normalize();
        Vec3 launch = player.getEyePosition().add(
                aim.scale(Math.max(0.85D, player.getBbWidth() * 0.5D + 0.55D))
        );

        // Projectile#restoreFrom copies the serializable projectile payload (arrow stack, potion
        // data, trident data, etc.) while the new entity keeps an independent network identity.
        replacement.restoreFrom(original);
        replacement.setUUID(UUID.randomUUID());
        replacement.setOwner(player);
        replacement.setPos(launch.x, launch.y, launch.z);
        replacement.setDeltaMovement(aim.scale(speed));

        if (replacement instanceof AbstractArrow arrow) {
            ((AbstractArrowAugmentAccessor) arrow).augmentationSurvival$invokeSetInGround(false);
            ((AbstractArrowAugmentAccessor) arrow).augmentationSurvival$invokeResetPiercedEntities();
        }
        if (replacement instanceof ProjectileRuntimeCarrier carrier) {
            ProjectileRuntimeState runtime = carrier.augmentationSurvival$getProjectileRuntime();
            if (runtime == null) {
                runtime = new ProjectileRuntimeState();
                carrier.augmentationSurvival$setProjectileRuntime(runtime);
            }
            if (!runtime.initialized() && replacement instanceof AbstractArrow arrow) {
                runtime.initialize(player.getUUID(), arrow.getPierceLevel(), 0, 0, 0, 0);
            }
            runtime.markReflected();
        }

        if (!level.addFreshEntity(replacement)) {
            return;
        }

        Set<Projectile> reflected = ReactiveCombatReflectedAccessor
                .augmentationSurvival$getReflectedProjectiles();
        // Keep the original marked until the existing presentation RETURN hook observes it; the
        // actual moving reflected projectile is the replacement.
        reflected.add(original);
        reflected.add(replacement);
        original.discard();
        ci.cancel();
    }
}
