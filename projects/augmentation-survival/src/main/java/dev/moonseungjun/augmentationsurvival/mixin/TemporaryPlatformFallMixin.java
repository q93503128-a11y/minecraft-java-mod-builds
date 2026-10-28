package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformLandingProtection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Temporary platforms nullify only the landing fall-damage result; actual fall distance remains observable. */
@Mixin(LivingEntity.class)
public abstract class TemporaryPlatformFallMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$cancelTemporaryPlatformFallDamage(
            double fallDistance,
            float damageMultiplier,
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof ServerPlayer player
                && TemporaryPlatformLandingProtection.protects(player)) {
            cir.setReturnValue(false);
        }
    }
}
