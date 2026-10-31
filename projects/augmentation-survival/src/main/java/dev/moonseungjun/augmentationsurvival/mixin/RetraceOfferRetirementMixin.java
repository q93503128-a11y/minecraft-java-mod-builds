package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the retrace id as a save/network tombstone while removing it from all future offers. */
@Mixin(value = AugmentOfferService.class, remap = false)
abstract class RetraceOfferRetirementMixin {
    private static final String RETRACE_ID = AugmentationSurvival.MOD_ID + ":retrace";

    @Inject(method = "eligibleCandidates", at = @At("RETURN"), cancellable = true)
    private static void augmentationSurvival$removeRetraceFromOffers(
            AugmentationPlayerState state,
            AugmentTier tier,
            CallbackInfoReturnable<List<AugmentDefinition>> cir
    ) {
        cir.setReturnValue(cir.getReturnValue().stream()
                .filter(definition -> !RETRACE_ID.equals(definition.id()))
                .toList());
    }

    @Inject(method = "rerollPool", at = @At("RETURN"), cancellable = true)
    private static void augmentationSurvival$removeRetraceFromRerolls(
            AugmentationPlayerState state,
            AugmentTier tier,
            Set<String> currentCandidates,
            Set<String> seenIds,
            boolean unseenOnly,
            CallbackInfoReturnable<List<AugmentDefinition>> cir
    ) {
        cir.setReturnValue(cir.getReturnValue().stream()
                .filter(definition -> !RETRACE_ID.equals(definition.id()))
                .toList());
    }
}
