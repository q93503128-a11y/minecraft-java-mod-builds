package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes ordinary hurt animation and duplicate impact feedback from delayed installments. */
@Mixin(LivingEntity.class)
abstract class LivingEntityDelayedDamageFeedbackMixin {
    @Shadow public int hurtTime;
    @Shadow public int hurtDuration;

    @Unique private boolean augmentationSurvival$restoreDelayedVisualState;
    @Unique private int augmentationSurvival$hurtTimeBeforeDelayed;
    @Unique private int augmentationSurvival$hurtDurationBeforeDelayed;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void augmentationSurvival$captureDelayedVisualState(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        augmentationSurvival$restoreDelayedVisualState =
                (Object) this instanceof ServerPlayer && CombatProvenanceContext.isDelayedDamage();
        if (!augmentationSurvival$restoreDelayedVisualState) return;
        augmentationSurvival$hurtTimeBeforeDelayed = hurtTime;
        augmentationSurvival$hurtDurationBeforeDelayed = hurtDuration;
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void augmentationSurvival$restoreDelayedVisualState(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!augmentationSurvival$restoreDelayedVisualState) return;
        augmentationSurvival$restoreDelayedVisualState = false;
        hurtTime = augmentationSurvival$hurtTimeBeforeDelayed;
        hurtDuration = augmentationSurvival$hurtDurationBeforeDelayed;
    }

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
