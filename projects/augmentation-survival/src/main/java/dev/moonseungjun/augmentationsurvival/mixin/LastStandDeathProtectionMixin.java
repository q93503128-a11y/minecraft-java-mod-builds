package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ensures Last Stand resolves before vanilla held-item and augment inventory death protection. */
@Mixin(LivingEntity.class)
abstract class LastStandDeathProtectionMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$applyLastStandBeforeTotems(
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!CombatAugmentServiceInvoker.augmentationSurvival$allowDeath(self, source, 0.0F)) {
            cir.setReturnValue(true);
        }
    }
}
