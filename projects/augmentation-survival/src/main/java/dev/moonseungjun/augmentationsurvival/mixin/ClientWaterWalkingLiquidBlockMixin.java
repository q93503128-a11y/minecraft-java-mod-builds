package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.utility.WaterWalkingSurface;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Local prediction companion for Water Walk's water-surface collision. */
@Mixin(LiquidBlock.class)
abstract class ClientWaterWalkingLiquidBlockMixin {
    private static final String WATER_WALK_ID = AugmentationSurvival.MOD_ID + ":water_run";

    @Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$predictWaterWalkingSurface(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!(context instanceof EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof LocalPlayer player)
                || ClientAugmentProgressState.level(WATER_WALK_ID) <= 0
                || !WaterWalkingSurface.canUseSurface(player)) {
            return;
        }
        VoxelShape surface = WaterWalkingSurface.shape(level, pos, state.getFluidState());
        if (!surface.isEmpty() && context.isAbove(surface, pos, false)) {
            cir.setReturnValue(Shapes.or(cir.getReturnValue(), surface));
        }
    }
}
