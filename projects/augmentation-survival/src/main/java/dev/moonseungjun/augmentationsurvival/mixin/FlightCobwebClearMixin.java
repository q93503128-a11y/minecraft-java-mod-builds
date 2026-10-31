package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cobwebs have no collision shape, so powered flight clears them before the normal impact filter. */
@Mixin(value = FlightAugmentService.class, remap = false)
abstract class FlightCobwebClearMixin {
    private static final int MAX_COBWEBS_PER_TICK = 12;

    @Inject(method = "resolveImpacts", at = @At("HEAD"))
    private static void augmentationSurvival$clearCobwebsDuringFlight(
            ServerPlayer player,
            Vec3 movement,
            CallbackInfo ci
    ) {
        if (!(player.level() instanceof ServerLevel level)) return;
        AABB swept = player.getBoundingBox().expandTowards(movement).inflate(0.24D, 0.18D, 0.24D);
        int minX = Mth.floor(swept.minX), minY = Mth.floor(swept.minY), minZ = Mth.floor(swept.minZ);
        int maxX = Mth.floor(swept.maxX), maxY = Mth.floor(swept.maxY), maxZ = Mth.floor(swept.maxZ);
        int broken = 0;
        for (int y = minY; y <= maxY && broken < MAX_COBWEBS_PER_TICK; y++) {
            for (int x = minX; x <= maxX && broken < MAX_COBWEBS_PER_TICK; x++) {
                for (int z = minZ; z <= maxZ && broken < MAX_COBWEBS_PER_TICK; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(Blocks.COBWEB)) continue;
                    AABB blockBox = new AABB(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
                    if (!blockBox.intersects(swept)) continue;
                    if (level.destroyBlock(pos, false, player)) broken++;
                }
            }
        }
    }
}
