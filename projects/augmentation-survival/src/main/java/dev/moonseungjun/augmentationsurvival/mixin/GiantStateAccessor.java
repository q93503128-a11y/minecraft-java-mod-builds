package dev.moonseungjun.augmentationsurvival.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Mutable end-tick accessor for the private Gigantification runtime record. */
@Mixin(targets = "dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService$GiantState", remap = false)
public interface GiantStateAccessor {
    @Accessor("endTick")
    long augmentationSurvival$getEndTick();

    @Mutable
    @Accessor("endTick")
    void augmentationSurvival$setEndTick(long endTick);
}
