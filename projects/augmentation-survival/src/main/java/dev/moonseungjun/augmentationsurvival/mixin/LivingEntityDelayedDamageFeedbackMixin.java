package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Removes ordinary hit-direction, hurt-animation and duplicate hurt-sound feedback from delayed installments. */
@Mixin(LivingEntity.class)
abstract class LivingEntityDelayedDamageFeedbackMixin {
    @Inject(method = "indicateDamage", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$suppressDelayedDirectionalImpact(double x, double z, CallbackInfo ci) {
        augmentationSurvival$cancelForDelayedPlayer(ci);
    }

    @Inject(method = "animateHurt", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$suppressDelayedHurtAnimation(float yaw, CallbackInfo ci) {
        augmentationSurvival$cancelForDelayedPlayer(ci);
    }

    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$suppressDelayedVanillaHurtSound(DamageSource source, CallbackInfo ci) {
        augmentationSurvival$cancelForDelayedPlayer(ci);
    }

    @Inject(method = "playSecondaryHurtSound", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$suppressDelayedSecondaryHurtSound(DamageSource source, CallbackInfo ci) {
        augmentationSurvival$cancelForDelayedPlayer(ci);
    }

    private void augmentationSurvival$cancelForDelayedPlayer(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer && CombatProvenanceContext.isDelayedDamage()) {
            ci.cancel();
        }
    }
}
