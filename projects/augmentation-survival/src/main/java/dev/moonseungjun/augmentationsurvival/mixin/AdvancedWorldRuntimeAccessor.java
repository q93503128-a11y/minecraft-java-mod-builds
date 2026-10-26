package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import java.util.Map;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access to the bounded Gigantification runtime map for shared active-policy integration. */
@Mixin(value = AdvancedWorldAugmentService.class, remap = false)
public interface AdvancedWorldRuntimeAccessor {
    @Accessor("GIANT")
    static Map<UUID, Object> augmentationSurvival$getGiantStates() {
        throw new AssertionError();
    }
}
