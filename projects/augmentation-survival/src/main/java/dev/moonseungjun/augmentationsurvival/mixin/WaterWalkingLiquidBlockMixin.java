package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.WaterWalkingSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

/** Server-authoritative water-surface collision for Water Walk. */
@Mixin(LiquidBlock.class)
public abstract class WaterWalkingLiquidBlockMixin {
    @Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$waterWalkingSurface(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!(context instanceof EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof ServerPlayer player)
                || !UtilityAugmentService.canRunOnWater(player)
                || !WaterWalkingSurface.canUseSurface(player)) {
            return;
        }
        VoxelShape surface = WaterWalkingSurface.shape(level, pos, state.getFluidState());
        if (!surface.isEmpty() && context.isAbove(surface, pos, false)) {
            cir.setReturnValue(Shapes.or(cir.getReturnValue(), surface));
        }
    }
}
