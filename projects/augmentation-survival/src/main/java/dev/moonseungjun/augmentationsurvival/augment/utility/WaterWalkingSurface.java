package dev.moonseungjun.augmentationsurvival.augment.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared geometry for the Water Walk augment's query-time water surface. */
public final class WaterWalkingSurface {
    private WaterWalkingSurface() { }

    public static boolean canUseSurface(Entity entity) {
        return entity != null
                && !entity.isShiftKeyDown()
                && entity.isAlive()
                && !entity.isPassenger()
                && entity.fallDistance < 5.0F;
    }

    public static VoxelShape shape(BlockGetter level, BlockPos pos, FluidState fluid) {
        if (!fluid.is(FluidTags.WATER)
                || level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return Shapes.empty();
        }
        return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, fluid.getHeight(level, pos), 1.0D);
    }

    /** True only while the entity's feet are resting on the top face of a water surface. */
    public static boolean supported(Entity entity) {
        if (!canUseSurface(entity)) {
            return false;
        }
        var box = entity.getBoundingBox();
        int y = (int) Math.floor(box.minY + 0.001D);
        int minX = (int) Math.floor(box.minX + 1.0E-6D);
        int maxX = (int) Math.floor(box.maxX - 1.0E-6D);
        int minZ = (int) Math.floor(box.minZ + 1.0E-6D);
        int maxZ = (int) Math.floor(box.maxZ - 1.0E-6D);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int dy = 0; dy >= -1; dy--) {
                    BlockPos pos = new BlockPos(x, y + dy, z);
                    FluidState fluid = entity.level().getFluidState(pos);
                    if (!fluid.is(FluidTags.WATER)
                            || entity.level().getFluidState(pos.above()).is(FluidTags.WATER)) {
                        continue;
                    }
                    double surfaceY = pos.getY() + fluid.getHeight(entity.level(), pos);
                    if (box.minY >= surfaceY - 0.001D && box.minY <= surfaceY + 0.10D) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
