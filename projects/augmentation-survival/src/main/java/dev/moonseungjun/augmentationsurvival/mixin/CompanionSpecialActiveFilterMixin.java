package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dominated companions leave the hostile selector used by time stop and divine punishment. */
@Mixin(SpecialActiveAugmentService.class)
abstract class CompanionSpecialActiveFilterMixin {
    @Inject(method = "isHostileTarget", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$excludeDominated(
            LivingEntity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CompanionAugmentService.isDominated(entity)) {
            cir.setReturnValue(false);
        }
    }
}
