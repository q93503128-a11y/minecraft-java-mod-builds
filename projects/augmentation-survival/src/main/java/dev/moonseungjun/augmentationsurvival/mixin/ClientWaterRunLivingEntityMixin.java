package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Client prediction companion to the server-authoritative Water Run collision rule. */
@Mixin(LivingEntity.class)
abstract class ClientWaterRunLivingEntityMixin {
    private static final String WATER_RUN_ID = AugmentationSurvival.MOD_ID + ":water_run";
    private static final double WATER_RUN_MIN_HORIZONTAL_SPEED_SQUARED = 0.0064D;

    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean augmentationSurvival$predictWaterRun(boolean original, FluidState fluidState) {
        if (original) return true;
        if (!((Object) this instanceof LocalPlayer player)) return false;
        boolean movingFastEnough = player.isSprinting()
                || player.getDeltaMovement().horizontalDistanceSqr() >= WATER_RUN_MIN_HORIZONTAL_SPEED_SQUARED;
        return movingFastEnough
                && !player.isShiftKeyDown()
                && !player.isDeadOrDying()
                && ClientAugmentProgressState.level(WATER_RUN_ID) > 0
                && fluidState.is(FluidTags.WATER);
    }
}
