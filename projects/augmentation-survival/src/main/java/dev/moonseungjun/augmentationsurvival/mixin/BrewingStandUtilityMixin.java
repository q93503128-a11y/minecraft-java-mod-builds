package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Snapshots nearby-holder fast brewing when vanilla starts a new 400-tick brew. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandUtilityMixin {
    private static final int VANILLA_BREW_TICKS = 400;

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void augmentationSurvival$accelerateNewBrew(
            Level level,
            BlockPos pos,
            BlockState state,
            BrewingStandBlockEntity blockEntity,
            CallbackInfo ci
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BrewingStandAugmentAccessor accessor = (BrewingStandAugmentAccessor) blockEntity;
        if (accessor.augmentationSurvival$getBrewTime() != VANILLA_BREW_TICKS) {
            return;
        }
        accessor.augmentationSurvival$setBrewTime(
                UtilityAugmentService.modifyBrewingDuration(
                        serverLevel,
                        pos,
                        VANILLA_BREW_TICKS
                )
        );
    }
}
