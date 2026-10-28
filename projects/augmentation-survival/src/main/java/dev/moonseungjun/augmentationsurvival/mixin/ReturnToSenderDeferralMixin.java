package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Legacy fallback for projectiles that reach the Fabric blocked-damage event without going through
 * Player.tryDeflect. Vanilla shield deflection is now handled earlier by PlayerReturnToSenderMixin.
 */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
public abstract class ReturnToSenderDeferralMixin {
    @Unique
    private static final String AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";
    @Unique
    private static final Deque<PendingReflection> AUGMENTATION_SURVIVAL$PENDING =
            new ArrayDeque<>();

    @Inject(method = "tryReturnProjectile", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$captureReflection(
            ServerPlayer player,
            DamageSource source,
            CallbackInfo ci
    ) {
        if (!(source.getDirectEntity() instanceof Projectile projectile)) {
            return;
        }
        // The vanilla deflection hook already applied the authoritative reflection. Do not scale it
        // a second time when AFTER_DAMAGE reports the same shield block.
        if (ReactiveCombatAugmentService.isReflectedProjectile(projectile)) {
            ci.cancel();
            return;
        }
        double originalSpeed = projectile.getDeltaMovement().length();
        if (!(originalSpeed > 1.0E-7D)) {
            return;
        }
        int ownedLevel = AugmentationPlayerStateService.state(player)
                .level(AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID);
        if (ownedLevel <= 0 || player.getLookAngle().lengthSqr() <= 1.0E-7D) {
            return;
        }
        int level = Math.min(
                ownedLevel,
                AugmentCatalog.require(AUGMENTATION_SURVIVAL$RETURN_TO_SENDER_ID).maxLevel()
        );
        double reflectedSpeed = originalSpeed
                * ReactiveCombatTuning.current().returnSpeedScale(level);
        AUGMENTATION_SURVIVAL$PENDING.addLast(
                new PendingReflection(projectile, player.getUUID(), reflectedSpeed)
        );
    }

    @Inject(method = "tickRuntime", at = @At("HEAD"))
    private static void augmentationSurvival$applyDeferredReflections(
            MinecraftServer server,
            CallbackInfo ci
    ) {
        Iterator<PendingReflection> iterator = AUGMENTATION_SURVIVAL$PENDING.iterator();
        while (iterator.hasNext()) {
            PendingReflection pending = iterator.next();
            iterator.remove();
            Projectile projectile = pending.projectile();
            ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
            if (player == null || player.isDeadOrDying() || projectile.isRemoved()) {
                continue;
            }
            Vec3 direction = player.getLookAngle();
            if (direction.lengthSqr() <= 1.0E-7D) {
                continue;
            }
            projectile.deflect(
                    ProjectileDeflection.AIM_DEFLECT,
                    player,
                    EntityReference.of(player),
                    true
            );
            projectile.setDeltaMovement(direction.normalize().scale(pending.speed()));
        }
    }

    @Unique
    private record PendingReflection(Projectile projectile, UUID playerId, double speed) { }
}
