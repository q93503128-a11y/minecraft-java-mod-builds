package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs after the default-priority vanilla/emergency/reincarnation death-protection return hook. */
@Mixin(value = LivingEntity.class, priority = 900)
abstract class PlayerRescueDeathProtectionMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$applyCoopRescueLast(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && PlayerRescueAugmentService.tryPostVanillaDeathProtection((LivingEntity) (Object) this, source)) {
            cir.setReturnValue(true);
        }
    }
}
