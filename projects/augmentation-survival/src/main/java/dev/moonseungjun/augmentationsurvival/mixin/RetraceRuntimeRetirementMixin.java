package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy saves may retain retrace, but the retired augment no longer has a live runtime effect. */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
abstract class RetraceRuntimeRetirementMixin {
    private static final String RETRACE_ID = AugmentationSurvival.MOD_ID + ":retrace";

    @Inject(method = "boundedLevel", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$retireRetraceLevel(
            AugmentationPlayerState state,
            String augmentId,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (RETRACE_ID.equals(augmentId)) cir.setReturnValue(0);
    }
}
