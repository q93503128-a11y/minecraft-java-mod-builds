package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.SweepingEnhancementService;
import dev.moonseungjun.augmentationsurvival.client.ClientPerceptionState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Optional Better Combat client target-selection adapter. */
@Pseudo
@Mixin(targets = "net.bettercombat.client.collision.TargetFinder", remap = false)
abstract class BetterCombatTargetFinderMixin {
    @ModifyVariable(
            method = "findAttackTargetResult",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static double augmentationSurvival$extendAttackRange(double original) {
        return original + SweepingEnhancementService.clientRangeBonus(
                ClientPerceptionState.sweepingEnhancementLevel()
        );
    }

    @ModifyArg(
            method = "findAttackTargetResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/bettercombat/client/collision/TargetFinder$RadialFilter;<init>(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;DD)V"
            ),
            index = 3
    )
    private static double augmentationSurvival$extendAttackAngle(double original) {
        return Math.min(
                360.0D,
                original + SweepingEnhancementService.clientAngleBonus(
                        ClientPerceptionState.sweepingEnhancementLevel()
                )
        );
    }
}
