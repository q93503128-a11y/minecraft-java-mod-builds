package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server-authoritative Water Run entry/continuation state at Minecraft's fluid collision boundary. */
@Mixin(LivingEntity.class)
public abstract class WaterRunLivingEntityMixin {
    @Unique private static final double AUGMENTATION_SURVIVAL$WATER_RUN_MIN_SPEED_SQUARED = 0.0064D;
    @Unique private static final long AUGMENTATION_SURVIVAL$SPRINT_EDGE_GRACE_TICKS = 1L;

    @Unique private long augmentationSurvival$lastGroundSprintTick = Long.MIN_VALUE;
    @Unique private long augmentationSurvival$lastSprintTick = Long.MIN_VALUE;
    @Unique private boolean augmentationSurvival$waterRunActive;

    @Inject(method = "tick", at = @At("TAIL"))
    private void augmentationSurvival$trackWaterRunEntry(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        long now = player.level().getGameTime();
        boolean waterBelow = augmentationSurvival$hasWaterBelow(player);
        if (player.isSprinting()) {
            augmentationSurvival$lastSprintTick = now;
            if (player.onGround() && !player.isInWater() && !waterBelow) {
                augmentationSurvival$lastGroundSprintTick = now;
            }
        }

        if (player.isShiftKeyDown()
                || player.isDeadOrDying()
                || player.isInWater()
                || player.getDeltaMovement().horizontalDistanceSqr()
                < AUGMENTATION_SURVIVAL$WATER_RUN_MIN_SPEED_SQUARED
                || !waterBelow) {
            augmentationSurvival$waterRunActive = false;
        }
    }

    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean augmentationSurvival$runOnWater(boolean original, FluidState fluidState) {
        if (original) return true;
        if (!((Object) this instanceof ServerPlayer player) || !fluidState.is(FluidTags.WATER)) {
            return false;
        }

        double horizontalSpeedSquared = player.getDeltaMovement().horizontalDistanceSqr();
        if (horizontalSpeedSquared < AUGMENTATION_SURVIVAL$WATER_RUN_MIN_SPEED_SQUARED
                || !UtilityAugmentService.canRunOnWater(player)) {
            augmentationSurvival$waterRunActive = false;
            return false;
        }

        long now = player.level().getGameTime();
        boolean sprintIntent = player.isSprinting()
                || augmentationSurvival$withinSprintGrace(augmentationSurvival$lastSprintTick, now);
        if (!sprintIntent) {
            augmentationSurvival$waterRunActive = false;
            return false;
        }
        if (augmentationSurvival$waterRunActive) {
            return true;
        }

        if (augmentationSurvival$withinSprintGrace(augmentationSurvival$lastGroundSprintTick, now)) {
            augmentationSurvival$waterRunActive = true;
            return true;
        }
        return false;
    }

    @Unique
    private static boolean augmentationSurvival$withinSprintGrace(long recordedTick, long now) {
        return recordedTick != Long.MIN_VALUE
                && now >= recordedTick
                && now - recordedTick <= AUGMENTATION_SURVIVAL$SPRINT_EDGE_GRACE_TICKS;
    }

    @Unique
    private static boolean augmentationSurvival$hasWaterBelow(ServerPlayer player) {
        BlockPos below = BlockPos.containing(
                player.getX(),
                player.getBoundingBox().minY - 0.05D,
                player.getZ()
        );
        return player.level().getFluidState(below).is(FluidTags.WATER);
    }
}
