package kr.moonseungjun.campfiresessions.gameplay;

import kr.moonseungjun.campfiresessions.block.ChairBlock;
import kr.moonseungjun.campfiresessions.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ChairSeatManager {
    private static final String SEAT_TAG = "campfiresessions_chair_seat";
    private static final String POS_PREFIX = "campfiresessions_chair_pos_";

    private ChairSeatManager() {}

    public static void onUseBlock(UseItemOnBlockEvent event) {
        if (event.isCanceled()
                || event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.WOODEN_CHAIR.get())) return;
        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);

        if (player.getVehicle() instanceof ArmorStand current && current.entityTags().contains(SEAT_TAG)) {
            player.stopRiding();
            current.discard();
            return;
        }
        if (isOccupied(level, pos)) return;

        Direction facing = state.getValue(ChairBlock.FACING);
        ArmorStand seat = new ArmorStand(EntityTypes.ARMOR_STAND, level);
        seat.setPos(pos.getX() + 0.5 - facing.getStepX() * 0.025,
                pos.getY() - 1.43,
                pos.getZ() + 0.5 - facing.getStepZ() * 0.025);
        seat.setYRot(facing.toYRot());
        seat.setYHeadRot(facing.toYRot());
        seat.setInvisible(true);
        seat.setNoGravity(true);
        seat.setInvulnerable(true);
        seat.addTag(SEAT_TAG);
        seat.addTag(POS_PREFIX + pos.asLong());
        if (!level.addFreshEntity(seat)) return;

        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
        if (!player.startRiding(seat)) seat.discard();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) continue;
            level.getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(48.0),
                    stand -> stand.entityTags().contains(SEAT_TAG))
                    .forEach(stand -> { if (stand.getPassengers().isEmpty()) stand.discard(); });
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getVehicle() instanceof ArmorStand seat && seat.entityTags().contains(SEAT_TAG)) seat.discard();
    }

    private static boolean isOccupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(ArmorStand.class, new AABB(pos).inflate(0.8, 1.2, 0.8),
                stand -> stand.entityTags().contains(SEAT_TAG)
                        && stand.entityTags().contains(POS_PREFIX + pos.asLong())
                        && !stand.getPassengers().isEmpty()).isEmpty();
    }
}
