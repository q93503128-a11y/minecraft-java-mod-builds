package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.SweepingEnhancementService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Better Combat relation/hit classification bridge. */
@Pseudo
@Mixin(targets = "net.bettercombat.logic.TargetHelper", remap = false)
abstract class BetterCombatTargetHelperMixin {
    @Inject(method = "getRelation", at = @At("HEAD"))
    private static void augmentationSurvival$rememberTarget(
            Player attacker,
            Entity target,
            CallbackInfoReturnable<?> cir
    ) {
        SweepingEnhancementService.noteBetterCombatTarget(attacker, target);
    }

    @ModifyVariable(method = "isHitAllowed", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static boolean augmentationSurvival$rememberDirectClassification(boolean direct) {
        return SweepingEnhancementService.noteBetterCombatDirect(direct);
    }

    @Inject(method = "isHitAllowed", at = @At("RETURN"), cancellable = true)
    private static void augmentationSurvival$validateSecondaryTarget(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(SweepingEnhancementService.validateBetterCombatHit(cir.getReturnValueZ()));
    }
}
