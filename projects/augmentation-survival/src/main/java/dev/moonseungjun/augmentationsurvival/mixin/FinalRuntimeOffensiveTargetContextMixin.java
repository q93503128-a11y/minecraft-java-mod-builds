package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import dev.moonseungjun.augmentationsurvival.augment.WallSlamMeleeRuntime;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scopes final player-offense augments to non-allies and mirrors ROOT melee into Wall Slam runtime. */
@Mixin(value = FinalAugmentRuntimeService.class, remap = false)
abstract class FinalRuntimeOffensiveTargetContextMixin {
    @Inject(method = "afterDamage", at = @At("HEAD"))
    private static void augmentationSurvival$pushOffensiveContext(
            LivingEntity target,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked,
            CallbackInfo ci
    ) {
        OffensiveTargetContext.push();
    }

    @Inject(method = "afterDamage", at = @At("RETURN"))
    private static void augmentationSurvival$armWallSlamAndPop(
            LivingEntity target,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked,
            CallbackInfo ci
    ) {
        try {
            if (!blocked
                    && baseDamageTaken > 0.0F
                    && Float.isFinite(baseDamageTaken)
                    && source.getEntity() instanceof ServerPlayer attacker
                    && CombatProvenanceContext.resolve(source) == CombatProvenance.ROOT_MELEE) {
                WallSlamMeleeRuntime.arm(attacker, target, baseDamageTaken);
            }
        } finally {
            OffensiveTargetContext.pop();
        }
    }
}
