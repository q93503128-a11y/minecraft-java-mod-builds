package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeCarrier;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies Return to Sender at vanilla's successful player projectile-deflection boundary. */
@Mixin(Player.class)
abstract class PlayerReturnToSenderMixin {
    @Unique
    private static final String AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";
    @Unique private Projectile augmentationSurvival$deflectProjectile;
    @Unique private double augmentationSurvival$deflectSpeed;

    @Inject(method = "tryDeflect", at = @At("HEAD"))
    private void augmentationSurvival$captureIncomingProjectile(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof Projectile projectile) {
            augmentationSurvival$deflectProjectile = projectile;
            augmentationSurvival$deflectSpeed = projectile.getDeltaMovement().length();
        } else {
            augmentationSurvival$deflectProjectile = null;
            augmentationSurvival$deflectSpeed = 0.0D;
        }
    }

    @Inject(method = "tryDeflect", at = @At("RETURN"))
    private void augmentationSurvival$applyReturnToSender(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Projectile projectile = augmentationSurvival$deflectProjectile;
        double originalSpeed = augmentationSurvival$deflectSpeed;
        augmentationSurvival$deflectProjectile = null;
        augmentationSurvival$deflectSpeed = 0.0D;

        if (!cir.getReturnValueZ()
                || projectile == null
                || projectile != entity
                || !(originalSpeed > 1.0E-7D)
                || !((Object) this instanceof ServerPlayer player)) {
            return;
        }

        int ownedLevel = AugmentationPlayerStateService.state(player)
                .level(AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID);
        if (ownedLevel <= 0) return;
        int level = Math.min(
                ownedLevel,
                AugmentCatalog.require(AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID).maxLevel()
        );
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() <= 1.0E-7D) return;
        direction = direction.normalize();
        double reflectedSpeed = originalSpeed
                * ReactiveCombatTuning.current().returnSpeedScale(level);

        projectile.setOwner(player);
        Vec3 launch = player.getEyePosition().add(direction.scale(0.85D));
        projectile.setPos(launch.x, launch.y, launch.z);
        projectile.setDeltaMovement(direction.scale(reflectedSpeed));
        projectile.hurtMarked = true;
        ReactiveCombatReflectedAccessor.augmentationSurvival$getReflectedProjectiles().add(projectile);

        if (projectile instanceof AbstractArrow arrow) {
            ((AbstractArrowAugmentAccessor) arrow).augmentationSurvival$invokeSetInGround(false);
            ((AbstractArrowAugmentAccessor) arrow).augmentationSurvival$invokeResetPiercedEntities();
            if (projectile instanceof ProjectileRuntimeCarrier carrier) {
                ProjectileRuntimeState runtime = carrier.augmentationSurvival$getProjectileRuntime();
                if (runtime == null) {
                    runtime = new ProjectileRuntimeState();
                    carrier.augmentationSurvival$setProjectileRuntime(runtime);
                }
                if (!runtime.initialized()) {
                    runtime.initialize(player.getUUID(), arrow.getPierceLevel(), 0, 0, 0, 0);
                }
                runtime.markReflected();
            }
        }
    }
}
