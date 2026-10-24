package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.threat.ThreatScalingService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks only combat boundaries that the public Fabric events cannot modify precisely. */
@Mixin(LivingEntity.class)
abstract class CombatAugmentMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float augmentationSurvival$modifyRootOutgoingDamage(
            float damage,
            ServerLevel level,
            DamageSource source,
            float originalDamage
    ) {
        float threatScaled = CombatProvenanceContext.isDelayedDamage()
                ? damage
                : ThreatScalingService.modifyHostileOutgoingDamage(source, damage);
        return CombatAugmentService.modifyOutgoingDamage(
                (LivingEntity) (Object) this,
                source,
                threatScaled
        );
    }

    /** Delayed installments are already post-mitigation and must not damage armor again. */
    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$skipArmorForDelayedDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        if (CombatProvenanceContext.isDelayedDamage()) {
            cir.setReturnValue(CombatProvenanceContext.delayedDamageOr(damage));
        }
    }

    /** Delayed installments skip Protection/resistance but still consume current augment pools. */
    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$skipMagicForDelayedDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!CombatProvenanceContext.isDelayedDamage()) {
            return;
        }
        float reserved = CombatProvenanceContext.delayedDamageOr(damage);
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player) {
            reserved = ProtectionAugmentService.modifyDelayedDamage(player, reserved);
        }
        cir.setReturnValue(reserved);
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$modifyPostMitigationDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        if (CombatProvenanceContext.isDelayedDamage()) {
            return;
        }
        cir.setReturnValue(CombatAugmentService.modifyPostMitigationDamage(
                (LivingEntity) (Object) this,
                source,
                cir.getReturnValueF()
        ));
    }

    /** Vanilla held-item death protection runs first; inventory totem/reincarnation follow it. */
    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$postVanillaDeathProtection(
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ()
                && CombatAugmentService.tryPostVanillaDeathProtection(
                        (LivingEntity) (Object) this,
                        source
                )) {
            cir.setReturnValue(true);
        }
    }
}
