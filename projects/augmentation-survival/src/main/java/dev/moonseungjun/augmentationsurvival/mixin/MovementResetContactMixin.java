package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Uses only current-world contact for movement recharge instead of cached climbable/fluid flags. */
@Mixin(value = MovementAugmentService.class, remap = false)
abstract class MovementResetContactMixin {
    @ModifyReturnValue(method = "isResetContact", at = @At("RETURN"))
    private static boolean augmentationSurvival$useLiveResetContact(
            boolean original,
            ServerPlayer player
    ) {
        return player.onGround()
                || player.level().getBlockState(player.blockPosition()).is(BlockTags.CLIMBABLE)
                || player.level().getFluidState(player.blockPosition()).is(FluidTags.WATER);
    }
}
