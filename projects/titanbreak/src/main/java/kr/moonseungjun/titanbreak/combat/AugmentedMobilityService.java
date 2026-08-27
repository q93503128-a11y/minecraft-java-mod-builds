package kr.moonseungjun.titanbreak.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class AugmentedMobilityService {
    private static final double MAX_HORIZONTAL_SPEED = 1.15;
    private static final int MAX_BREAKS_PER_TICK = 10;

    private AugmentedMobilityService() {}

    public static void tick(ServerPlayer player, boolean driveActive) {
        if (!driveActive || !player.isSprinting() || !player.onGround()) return;

        Vec3 movement = player.getDeltaMovement();
        double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        if (horizontal > 0.05 && horizontal < MAX_HORIZONTAL_SPEED) {
            double multiplier = Math.min(1.18, MAX_HORIZONTAL_SPEED / horizontal);
            player.setDeltaMovement(movement.x * multiplier, movement.y,
                    movement.z * multiplier);
            player.hurtMarked = true;
        }

        if (horizontal > 0.30) breach((ServerLevel) player.level(), player);
    }

    private static void breach(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 0.001) return;
        flat = flat.normalize();

        BlockPos base = BlockPos.containing(player.getX() + flat.x * 1.2,
                player.getY(), player.getZ() + flat.z * 1.2);
        int breaks = 0;
        for (int forward = 0; forward <= 1 && breaks < MAX_BREAKS_PER_TICK; forward++) {
            BlockPos center = base.offset((int) Math.round(flat.x * forward), 0,
                    (int) Math.round(flat.z * forward));
            for (int y = 0; y <= 2 && breaks < MAX_BREAKS_PER_TICK; y++) {
                for (int side = -1; side <= 1 && breaks < MAX_BREAKS_PER_TICK; side++) {
                    BlockPos pos = center.offset((int) Math.round(-flat.z * side), y,
                            (int) Math.round(flat.x * side));
                    if (canBreach(level, pos) && level.destroyBlock(pos, true, player)) breaks++;
                }
            }
        }
    }

    private static boolean canBreach(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || level.getBlockEntity(pos) != null) return false;
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness <= 2.0F;
    }
}
