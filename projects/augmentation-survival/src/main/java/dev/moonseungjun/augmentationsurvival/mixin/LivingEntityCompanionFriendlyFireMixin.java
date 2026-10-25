package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.CloneActiveService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Permanent companions and temporary clones cannot damage their owner/allies through normal hurtServer. */
@Mixin(LivingEntity.class)
abstract class LivingEntityCompanionFriendlyFireMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$companionFriendlyFire(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity dealer = source.getEntity();
        if (dealer != null && (
                CompanionAugmentService.areAllied(dealer, self)
                        || CloneActiveService.areCloneAllies(dealer, self)
        )) {
            cir.setReturnValue(false);
        }
    }
}
