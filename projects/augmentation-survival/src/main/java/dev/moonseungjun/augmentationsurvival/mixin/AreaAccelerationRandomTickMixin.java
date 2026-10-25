package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds local extra growth random-ticks without accelerating unrelated world time or random ticks. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class AreaAccelerationRandomTickMixin {
    private static final ThreadLocal<Boolean> AUGMENTATION_SURVIVAL_REENTRY =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void augmentationSurvival$accelerateCompatibleGrowth(
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(AUGMENTATION_SURVIVAL_REENTRY.get())) {
            return;
        }
        BlockState current = level.getBlockState(pos);
        if (!AreaAccelerationService.isCompatibleGrowthBlock(current)) {
            return;
        }
        int extra = AreaAccelerationService.extraTicksAt(level, pos);
        if (extra <= 0) {
            return;
        }
        AUGMENTATION_SURVIVAL_REENTRY.set(Boolean.TRUE);
        try {
            for (int index = 0; index < extra; index++) {
                BlockState next = level.getBlockState(pos);
                if (!AreaAccelerationService.isCompatibleGrowthBlock(next)) {
                    break;
                }
                next.randomTick(level, pos, random);
            }
        } finally {
            AUGMENTATION_SURVIVAL_REENTRY.remove();
        }
    }
}
