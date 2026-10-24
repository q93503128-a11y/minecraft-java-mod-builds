package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Releases a domination slot only on an actual companion death. */
@Mixin(LivingEntity.class)
abstract class LivingEntityCompanionDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void augmentationSurvival$dominatedDeath(DamageSource source, CallbackInfo ci) {
        CompanionAugmentService.onActualDeath((LivingEntity) (Object) this);
    }
}
