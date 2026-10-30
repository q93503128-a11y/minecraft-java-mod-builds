package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.WaterWalkingSurface;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Water Walk landing is a supported surface, not a normal solid-block fall impact. */
@Mixin(LivingEntity.class)
public abstract class WaterWalkingFallMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$cancelWaterSurfaceFallDamage(
            double fallDistance,
            float damageMultiplier,
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof ServerPlayer player
                && UtilityAugmentService.canRunOnWater(player)
                && fallDistance < 5.0D
                && WaterWalkingSurface.supported(player)) {
            cir.setReturnValue(false);
        }
    }
}
