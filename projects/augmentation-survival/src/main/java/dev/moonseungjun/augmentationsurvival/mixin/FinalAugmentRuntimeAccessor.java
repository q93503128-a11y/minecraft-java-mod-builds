package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import java.util.Map;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Narrow accessor used to consume a Wall Slam window once the actual melee boundary resolves it. */
@Mixin(value = FinalAugmentRuntimeService.class, remap = false)
interface FinalAugmentRuntimeAccessor {
    @Accessor("WALL_SLAMS")
    static Map<UUID, ?> augmentationSurvival$getWallSlams() {
        throw new AssertionError();
    }
}
