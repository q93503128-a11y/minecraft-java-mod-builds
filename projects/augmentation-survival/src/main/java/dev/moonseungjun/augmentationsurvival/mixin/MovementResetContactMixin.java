package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Prevents stale ServerPlayer fluid flags after teleports from consuming/denying airborne charges. */
@Mixin(value = MovementAugmentService.class, remap = false)
abstract class MovementResetContactMixin {
    @ModifyReturnValue(method = "isResetContact", at = @At("RETURN"))
    private static boolean augmentationSurvival$requireLiveResetContact(
            boolean original,
            ServerPlayer player
    ) {
        if (!original) return false;
        if (player.onGround() || player.onClimbable()) return true;
        return player.level().getFluidState(player.blockPosition()).is(FluidTags.WATER);
    }
}
