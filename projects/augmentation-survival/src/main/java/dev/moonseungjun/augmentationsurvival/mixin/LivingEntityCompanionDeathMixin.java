package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.ReviveAugmentService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures eligible revive state before releasing a domination slot on actual death. */
@Mixin(LivingEntity.class)
abstract class LivingEntityCompanionDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void augmentationSurvival$companionDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ReviveAugmentService.onActualDeath(entity);
        CompanionAugmentService.onActualDeath(entity);
    }
}
