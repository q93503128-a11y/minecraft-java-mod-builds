package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.client.ClientWaterRunSprintIntentState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Client prediction companion to the forgiving server Water Run boundary. */
@Mixin(LivingEntity.class)
abstract class ClientWaterRunLivingEntityMixin {
    @Unique private static final String AUGMENTATION_SURVIVAL$WATER_RUN_ID =
            AugmentationSurvival.MOD_ID + ":water_run";
    @Unique private static final double AUGMENTATION_SURVIVAL$WATER_RUN_MIN_SPEED_SQUARED = 0.0064D;
    @Unique private static final long AUGMENTATION_SURVIVAL$SPRINT_EDGE_GRACE_TICKS = 2L;

    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean augmentationSurvival$predictWaterRun(boolean original, FluidState fluidState) {
        if (original) return true;
        if (!((Object) this instanceof LocalPlayer player) || !fluidState.is(FluidTags.WATER)) return false;
        if (ClientAugmentProgressState.level(AUGMENTATION_SURVIVAL$WATER_RUN_ID) <= 0
                || player.isShiftKeyDown()
                || player.isDeadOrDying()
                || player.isPassenger()
                || player.getDeltaMovement().horizontalDistanceSqr() < AUGMENTATION_SURVIVAL$WATER_RUN_MIN_SPEED_SQUARED) {
            return false;
        }
        return player.isSprinting()
                || ClientWaterRunSprintIntentState.wasRecent(player, AUGMENTATION_SURVIVAL$SPRINT_EDGE_GRACE_TICKS);
    }
}
