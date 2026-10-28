package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.MCEndgameDamageCompatibility;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owns the final player-damage boundary, including the pinned MCEndgame replacement path. */
@Mixin(value = Player.class, priority = 900)
abstract class PlayerDamagePipelineMixin {
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

    /** Normal runtime: apply final player layers after vanilla armor/magic mitigation. */
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

    /**
     * MCEndgame injects its own DamageService result into Player#actuallyHurt and then uses that
     * Optional value for vanilla absorption and HP. Run after its default-priority mixin and modify
     * the exact value the replacement body consumes.
     */
    @ModifyExpressionValue(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/fuballer/mcendgame/main/component/damage/DamageService;applyDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Ljava/util/Optional;",
                    remap = false
            ),
            require = 0
    )
    private Optional<Float> augmentationSurvival$applyMCEndgameFinalPlayerDamageLayers(
            Optional<Float> calculatedDamage,
            ServerLevel level,
            DamageSource source,
            float incomingDamage
    ) {
        return MCEndgameDamageCompatibility.modifyCalculatedDamage(
                (Player) (Object) this,
                level,
                source,
                calculatedDamage
        );
    }
}
