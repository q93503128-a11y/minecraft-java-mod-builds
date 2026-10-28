package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.MCEndgameDamageCompatibility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owns the final player-damage boundary, including the pinned MCEndgame replacement path. */
@Mixin(value = Player.class, priority = 2000)
abstract class PlayerDamagePipelineMixin {
    /**
     * MCEndgame 1.7.0+26.2 consumes and cancels actuallyHurt at HEAD, before vanilla's
     * armor/magic tail can reach our normal hook. Give that replacement the already-finalized
     * Augmentation Survival damage value instead. Higher mixin priority keeps this argument
     * rewrite ahead of MCEndgame's cancellable HEAD handler.
     */
    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float augmentationSurvival$bridgeMCEndgameDamage(
            float damage,
            ServerLevel level,
            DamageSource source,
            float originalDamage
    ) {
        return MCEndgameDamageCompatibility.modifyActuallyHurtInput(
                (Player) (Object) this,
                level,
                source,
                damage
        );
    }

    /** Delayed installments are post-mitigation and must not wear armor again in MCEndgame's replacement. */
    @Inject(method = "hurtArmor", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$skipMCEndgameArmorWearForDelayedDamage(
            DamageSource source,
            float damage,
            CallbackInfo ci
    ) {
        if (MCEndgameDamageCompatibility.active() && CombatProvenanceContext.isDelayedDamage()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
            )
    )
    private float augmentationSurvival$applyFinalPlayerDamageLayers(
            float vanillaPostMagic,
            ServerLevel level,
            DamageSource source,
            float incomingDamage
    ) {
        if (MCEndgameDamageCompatibility.active() || CombatProvenanceContext.isDelayedDamage()) {
            return vanillaPostMagic;
        }
        return CombatAugmentService.modifyPostMitigationDamage(
                (Player) (Object) this,
                source,
                vanillaPostMagic
        );
    }
}
