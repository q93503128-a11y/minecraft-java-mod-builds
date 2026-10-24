package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Narrow accessor for the vanilla 26.2 brewing progress counter. */
@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandAugmentAccessor {
    @Accessor("brewTime")
    int augmentationSurvival$getBrewTime();

    @Accessor("brewTime")
    void augmentationSurvival$setBrewTime(int brewTime);
}
