package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Converts the directly targeted, normally harvestable root block to a one-tick mining speed. */
@Mixin(Player.class)
public abstract class InstantMiningPlayerMixin {
    @ModifyReturnValue(
            method = "getDestroySpeed",
            at = @At("RETURN")
    )
    private float augmentationSurvival$instantMine(float original, BlockState state) {
        if ((Object) this instanceof ServerPlayer player
                && UtilityAugmentService.shouldInstantMine(player, state, original)) {
            return Float.MAX_VALUE;
        }
        return original;
    }
}
