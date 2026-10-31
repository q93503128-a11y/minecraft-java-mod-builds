package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.EchoStrikeAfterimageService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the old charged-hit/counter Echo Strike with one immediate clone hit per root melee. */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class EchoStrikeAfterimageMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$initializeEchoAfterimages(CallbackInfo ci) {
        EchoStrikeAfterimageService.initialize();
    }

    /** The old charged-only trigger is retired; the all-root-melee hook below owns Echo Strike. */
    @Inject(method = "triggerEchoStrike", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$retireOldEchoTrigger(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            float baseDamageTaken,
            long now,
            CallbackInfo ci
    ) {
        ci.cancel();
    }

    @Inject(method = "afterDamage", at = @At("TAIL"))
    private static void augmentationSurvival$echoEveryRootMelee(
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
        EchoStrikeAfterimageService.onRootMelee(
                attacker,
                target,
                AugmentationPlayerStateService.state(attacker),
                baseDamageTaken,
                attacker.level().getGameTime()
        );
    }
}
