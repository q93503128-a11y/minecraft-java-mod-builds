package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.EchoStrikeAfterimageService;
import dev.moonseungjun.augmentationsurvival.augment.combat.SweepingEnhancementService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Converts Echo Strike's same-tick derived damage into a distinct delayed second melee hit. */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class EchoStrikeAfterimageMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$initializeEchoAfterimages(CallbackInfo ci) {
        EchoStrikeAfterimageService.initialize();
    }

    @Inject(method = "triggerEchoStrike", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$routeEchoAfterimage(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            float baseDamageTaken,
            long now,
            CallbackInfo ci
    ) {
        ci.cancel();
        EchoStrikeAfterimageService.onChargedMelee(attacker, target, state, baseDamageTaken, now);
    }

    /**
     * Better Combat can legitimately execute an approved direct swing before vanilla reaches the
     * 0.90 charged-hit threshold. Its target validation bridge marks exactly that damage call, so
     * Echo Strike counts the same accepted swing without weakening vanilla's charged-hit contract.
     */
    @Inject(method = "afterDamage", at = @At("TAIL"))
    private static void augmentationSurvival$bridgeBetterCombatEchoHit(
            LivingEntity target,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked,
            CallbackInfo ci
    ) {
        if (blocked || !(damageTaken > 0.0F) || !(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (CombatProvenanceContext.resolve(source) != CombatProvenance.ROOT_MELEE) {
            return;
        }
        if (attacker.getAttackStrengthScale(0.5F) >= 0.90F
                || !SweepingEnhancementService.isBetterCombatDirectDamage()) {
            return;
        }
        EchoStrikeAfterimageService.onChargedMelee(
                attacker,
                target,
                AugmentationPlayerStateService.state(attacker),
                baseDamageTaken,
                attacker.level().getGameTime()
        );
    }
}
