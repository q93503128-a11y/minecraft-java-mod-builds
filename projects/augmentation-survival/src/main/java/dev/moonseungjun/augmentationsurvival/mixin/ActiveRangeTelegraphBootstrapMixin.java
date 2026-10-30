package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveRangeTelegraphService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ActiveAbilityRouter.class)
abstract class ActiveRangeTelegraphBootstrapMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$initializeRangeTelegraphs(CallbackInfo ci) {
        ActiveRangeTelegraphService.initialize();
    }
}
