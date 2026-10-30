package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ShieldCompressionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces fragile projectile reflection with blocked-damage compression and a forward cone wave. */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
public abstract class ReturnToSenderDeferralMixin {
    @Inject(method = "afterDamage", at = @At("HEAD"))
    private static void augmentationSurvival$compressBlockedDamage(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked,
            CallbackInfo ci
    ) {
        if (blocked && entity instanceof ServerPlayer player) {
            ShieldCompressionService.onBlockedDamage(player, source, baseDamageTaken);
        }
    }

    @Inject(method = "tryReturnProjectile", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$disableLegacyProjectileReflection(
            ServerPlayer player,
            DamageSource source,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
