package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase Shift intentionally places the player inside horizontal/upward solid collision. Vanilla's
 * movement packet validator otherwise sees that intended overlap as a new illegal collision and
 * rubber-bands the player back into the wall.
 */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class PhaseShiftMovementValidationMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "isEntityCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$allowIntentionalPhaseOverlap(
            LevelReader level,
            Entity entity,
            AABB oldBox,
            double newX,
            double newY,
            double newZ,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity == player && AdvancedWorldAugmentService.isPhaseShiftActive(player)) {
            cir.setReturnValue(false);
        }
    }
}
