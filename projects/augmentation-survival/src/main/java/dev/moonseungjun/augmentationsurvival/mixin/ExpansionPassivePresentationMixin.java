package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassivePresentationService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds event-shaped world feedback to discrete combat procs while leaving all damage logic untouched. */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class ExpansionPassivePresentationMixin {
    private static final String CHAIN_EXPLOSION = PassivePresentationService.id("chain_explosion");
    private static final String LIGHTNING = PassivePresentationService.id("lightning");
    private static final String ECHO_STRIKE = PassivePresentationService.id("echo_strike");
    private static final String WEAKNESS_MARK = PassivePresentationService.id("weakness_mark");
    private static final String HEAVY_STRIKE = PassivePresentationService.id("heavy_strike");
    private static final String LIFE_WAVE = PassivePresentationService.id("life_wave");
    private static final String COMBAT_CYCLE = PassivePresentationService.id("combat_cycle");
    private static final String DERIVED_DAMAGE_TARGET =
            "Ldev/moonseungjun/augmentationsurvival/augment/combat/CombatExpansionService;"
                    + "applyDerivedDamage(Lnet/minecraft/server/level/ServerPlayer;"
                    + "Lnet/minecraft/world/entity/LivingEntity;F"
                    + "Ldev/moonseungjun/augmentationsurvival/augment/combat/CombatProvenance;)V";

    @Inject(method = "triggerChainExplosion", at = @At("RETURN"))
    private static void augmentationSurvival$presentChainExplosion(
            ServerLevel level,
            ServerPlayer player,
            LivingEntity killed,
            AugmentationPlayerState state,
            CallbackInfo ci
    ) {
        if (state.level(CHAIN_EXPLOSION) > 0) {
            PassivePresentationService.proc(player, CHAIN_EXPLOSION, killed);
        }
    }

    @Inject(method = "triggerLightning", at = @At(value = "INVOKE", target = DERIVED_DAMAGE_TARGET))
    private static void augmentationSurvival$presentLightning(
            ServerPlayer attacker,
            LivingEntity primaryTarget,
            AugmentationPlayerState state,
            float actualDamage,
            CallbackInfo ci
    ) {
        PassivePresentationService.proc(attacker, LIGHTNING, primaryTarget);
    }

    @Inject(method = "triggerEchoStrike", at = @At(value = "INVOKE", target = DERIVED_DAMAGE_TARGET))
    private static void augmentationSurvival$presentEchoStrike(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            float baseDamageTaken,
            long now,
            CallbackInfo ci
    ) {
        PassivePresentationService.proc(attacker, ECHO_STRIKE, target);
    }

    @Inject(method = "triggerHeavyStrike", at = @At(value = "INVOKE", target = DERIVED_DAMAGE_TARGET))
    private static void augmentationSurvival$presentHeavyStrike(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            long now,
            CallbackInfo ci
    ) {
        PassivePresentationService.proc(attacker, HEAVY_STRIKE, target);
    }

    @Inject(
            method = "updateWeaknessMark",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private static void augmentationSurvival$presentWeaknessMark(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            long now,
            CallbackInfo ci
    ) {
        PassivePresentationService.proc(attacker, WEAKNESS_MARK, target);
    }

    @Inject(method = "onPrimaryHealing", at = @At("RETURN"))
    private static void augmentationSurvival$presentLifeWave(
            ServerPlayer player,
            float actualHealing,
            CallbackInfo ci
    ) {
        if (actualHealing > 0.0F && AugmentationPlayerStateService.state(player).level(LIFE_WAVE) > 0) {
            PassivePresentationService.proc(player, LIFE_WAVE);
        }
    }

    @Inject(
            method = "afterKill",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/moonseungjun/augmentationsurvival/augment/active/ActiveAbilityService;reduceSupportedCooldowns(Lnet/minecraft/server/level/ServerPlayer;I)V"
            )
    )
    private static void augmentationSurvival$presentCombatCycle(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source,
            CallbackInfo ci
    ) {
        if (killer instanceof ServerPlayer player) {
            PassivePresentationService.proc(player, COMBAT_CYCLE);
        }
    }
}
