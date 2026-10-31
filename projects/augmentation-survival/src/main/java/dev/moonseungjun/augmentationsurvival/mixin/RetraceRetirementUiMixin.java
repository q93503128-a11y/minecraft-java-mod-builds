package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hides the save-compatible retrace tombstone from the player-facing owned augment browser. */
@Mixin(AugmentDeviceScreen.class)
abstract class RetraceRetirementUiMixin {
    private static final String RETRACE_ID = AugmentationSurvival.MOD_ID + ":retrace";

    @Inject(method = "ownedDefinitions", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$hideRetiredRetrace(
            CallbackInfoReturnable<List<AugmentDefinition>> cir
    ) {
        cir.setReturnValue(cir.getReturnValue().stream()
                .filter(definition -> !RETRACE_ID.equals(definition.id()))
                .toList());
    }
}
