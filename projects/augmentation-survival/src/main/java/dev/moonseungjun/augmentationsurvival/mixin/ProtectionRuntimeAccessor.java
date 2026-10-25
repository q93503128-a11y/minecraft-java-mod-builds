package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import java.util.Map;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only presentation accessor for the server protection runtime. */
@Mixin(ProtectionAugmentService.class)
public interface ProtectionRuntimeAccessor {
    @Accessor("RUNTIME")
    static Map<UUID, Object> augmentationSurvival$getRuntime() {
        throw new AssertionError();
    }
}
