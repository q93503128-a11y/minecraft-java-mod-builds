package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
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

/** Fast-brewing snapshot plus local Area Acceleration processing. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandUtilityMixin {
    private static final int VANILLA_BREW_TICKS = 400;
    private static final ThreadLocal<Boolean> AUGMENTATION_SURVIVAL_AREA_REENTRY =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

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
        if (accessor.augmentationSurvival$getBrewTime() == VANILLA_BREW_TICKS) {
            accessor.augmentationSurvival$setBrewTime(
                    UtilityAugmentService.modifyBrewingDuration(
                            serverLevel,
                            pos,
                            VANILLA_BREW_TICKS
                    )
            );
        }

        if (Boolean.TRUE.equals(AUGMENTATION_SURVIVAL_AREA_REENTRY.get())) {
            return;
        }
        int extra = AreaAccelerationService.extraTicksAt(serverLevel, pos);
        if (extra <= 0) {
            return;
        }
        AUGMENTATION_SURVIVAL_AREA_REENTRY.set(Boolean.TRUE);
        try {
            for (int index = 0; index < extra; index++) {
                BrewingStandBlockEntity.serverTick(
                        serverLevel,
                        pos,
                        serverLevel.getBlockState(pos),
                        blockEntity
                );
            }
        } finally {
            AUGMENTATION_SURVIVAL_AREA_REENTRY.remove();
        }
    }
}
