package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.SweepingEnhancementService;
import dev.moonseungjun.augmentationsurvival.augment.projectile.FocusedAimAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
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
        Float focused = FocusedAimAugmentService.modifyFocusedProjectileDamage(
                (LivingEntity) (Object) this,
                source,
                threatScaled
        );
        float modified = focused != null
                ? focused
                : CombatAugmentService.modifyOutgoingDamage(
                        (LivingEntity) (Object) this,
                        source,
                        threatScaled
                );
        float advanced = AdvancedUtilityAugmentService.modifyRootOutgoingDamage(source, modified);
        return SweepingEnhancementService.modifyBetterCombatOutgoingDamage(
                (LivingEntity) (Object) this,
                source,
                advanced
        );
    }

    /** Reduces only the armor/toughness inputs for ROOT direct attacks with Armor Pierce. */
    @WrapOperation(
            method = "getDamageAfterArmorAbsorb",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"
            )
    )
    private float augmentationSurvival$applyArmorPierce(
            LivingEntity victim,
            float damage,
            DamageSource source,
            float armor,
            float armorToughness,
            Operation<Float> original
    ) {
        double multiplier = AdvancedUtilityAugmentService.armorEffectivenessMultiplier(source);
        return original.call(
                victim,
                damage,
                source,
                (float) (armor * multiplier),
                (float) (armorToughness * multiplier)
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
        LivingEntity self = (LivingEntity) (Object) this;
        cir.setReturnValue(CombatAugmentService.modifyPostMitigationDamage(
                self,
                source,
                cir.getReturnValueF()
        ));
    }

    /**
     * Vanilla held-item death protection resolves first. If it declines, personal augment protection
     * (Emergency Totem / Reincarnation) gets priority; co-op rescue is the final fallback.
     */
    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$postVanillaDeathProtection(
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (CombatAugmentService.tryPostVanillaDeathProtection(self, source)) {
            cir.setReturnValue(true);
            return;
        }
        if (PlayerRescueAugmentService.tryPostVanillaDeathProtection(self, source)) {
            cir.setReturnValue(true);
        }
    }
}
