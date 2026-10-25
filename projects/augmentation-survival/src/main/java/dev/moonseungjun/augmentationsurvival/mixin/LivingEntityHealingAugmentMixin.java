package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.AugmentHealingService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Completes healing amplification for external non-natural heals without re-entering managed augment heals. */
@Mixin(LivingEntity.class)
abstract class LivingEntityHealingAugmentMixin {
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float augmentationSurvival$amplifyExternalHeal(float amount) {
        return AugmentHealingService.interceptExternalHealAmount((LivingEntity) (Object) this, amount);
    }

    @Inject(method = "heal", at = @At("RETURN"))
    private void augmentationSurvival$finishExternalHeal(float amount, CallbackInfo ci) {
        AugmentHealingService.finishExternalHeal((LivingEntity) (Object) this);
    }
}
