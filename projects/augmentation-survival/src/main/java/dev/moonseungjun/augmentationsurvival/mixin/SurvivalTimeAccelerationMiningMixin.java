package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds Time Acceleration to the same additive mining-speed axis as passive mining augments. */
@Mixin(value = SurvivalAugmentService.class, remap = false)
abstract class SurvivalTimeAccelerationMiningMixin {
    @ModifyReturnValue(method = "modifyBlockBreakSpeed", at = @At("RETURN"))
    private static float augmentationSurvival$addTimeAccelerationMining(
            float returned,
            ServerPlayer player,
            BlockState state,
            float original
    ) {
        if (!(original > 0.0F)
                || !Float.isFinite(original)
                || !AdvancedActiveAugmentService.isTimeAccelerationActive(player)) {
            return returned;
        }
        double bonus = AdvancedActiveTuning.current().timeAccelerationMiningBonus();
        double combined = returned + original * bonus;
        return Double.isFinite(combined)
                ? (float) Math.min(Float.MAX_VALUE, combined)
                : returned;
    }
}
