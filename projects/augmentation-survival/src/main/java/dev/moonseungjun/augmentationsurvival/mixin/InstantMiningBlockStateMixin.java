package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies server-authoritative instant mining at the final block-break progress boundary. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class InstantMiningBlockStateMixin {
    @ModifyReturnValue(method = "getDestroyProgress", at = @At("RETURN"))
    private float augmentationSurvival$applyInstantMining(
            float original,
            Player player,
            BlockGetter level,
            BlockPos pos
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(original > 0.0F)
                || !Float.isFinite(original)) {
            return original;
        }
        BlockState state = (BlockState) (Object) this;
        return UtilityAugmentService.shouldInstantMine(serverPlayer, state, original)
                ? Math.max(1.0F, original)
                : original;
    }
}
