package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scopes direct player-derived final augments such as Wall Slam/Life Link to non-allied targets. */
@Mixin(value = FinalAugmentRuntimeService.class, remap = false)
abstract class FinalRuntimeOffensiveTargetContextMixin {
    @Inject(method = "afterDamage", at = @At("HEAD"))
    private static void augmentationSurvival$pushOffensiveContext(
            LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked, CallbackInfo ci
    ) {
        OffensiveTargetContext.push();
    }

    @Inject(method = "afterDamage", at = @At("RETURN"))
    private static void augmentationSurvival$popOffensiveContext(
            LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked, CallbackInfo ci
    ) {
        OffensiveTargetContext.pop();
    }
}
