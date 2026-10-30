package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves explicit blank active slots instead of auto-filling them again on the next sync. */
@Mixin(value = ActiveAbilityRouter.class, remap = false)
abstract class ActiveLoadoutExplicitSlotsMixin {
    @Inject(method = "resolvedSlots", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$preserveExplicitBlanks(
            AugmentationPlayerState state,
            CallbackInfoReturnable<List<String>> cir
    ) {
        // Empty list is the legacy/unconfigured state: let the original router seed a useful default.
        // Any non-empty 10-slot layout is player-authored state, where blank means intentionally blank.
        if (state.activeSlots().isEmpty()) return;

        List<String> result = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) result.add("");
        Set<String> used = new HashSet<>();
        List<String> supported = ActiveAbilityRouter.supportedIds();
        for (int i = 0; i < Math.min(10, state.activeSlots().size()); i++) {
            String id = state.activeSlots().get(i);
            if (id != null && supported.contains(id) && state.level(id) > 0 && used.add(id)) {
                result.set(i, id);
            }
        }
        cir.setReturnValue(List.copyOf(result));
    }
}
