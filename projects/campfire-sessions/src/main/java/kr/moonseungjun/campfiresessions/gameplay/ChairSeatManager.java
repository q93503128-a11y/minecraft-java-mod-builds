package kr.moonseungjun.campfiresessions.gameplay;

import kr.moonseungjun.campfiresessions.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
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
                || !(event.getPlayer() instanceof ServerPlayer player)
                || !level.getBlockState(event.getPos()).is(ModBlocks.WOODEN_CHAIR.get())) return;

        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);

        if (player.getVehicle() instanceof ArmorStand current && current.entityTags().contains(SEAT_TAG)) {
            player.stopRiding();
            current.discard();
            return;
        }

        BlockPos pos = event.getPos();
        if (isOccupied(level, pos)) return;

        ArmorStand seat = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        if (seat == null) return;
        seat.setPos(pos.getX() + 0.5, pos.getY() - 0.45, pos.getZ() + 0.5);
        seat.setInvisible(true);
        seat.setMarker(true);
        seat.setNoGravity(true);
        seat.setInvulnerable(true);
        seat.addTag(SEAT_TAG);
        seat.addTag(POS_PREFIX + pos.asLong());
        if (!level.addFreshEntity(seat)) return;
        if (!player.startRiding(seat, true)) seat.discard();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) continue;
            level.getEntitiesOfClass(
                    ArmorStand.class,
                    player.getBoundingBox().inflate(48.0),
                    stand -> stand.entityTags().contains(SEAT_TAG)
            ).forEach(stand -> {
                if (stand.getPassengers().isEmpty()) stand.discard();
            });
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getVehicle() instanceof ArmorStand seat && seat.entityTags().contains(SEAT_TAG)) {
            seat.discard();
        }
    }

    private static boolean isOccupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(
                ArmorStand.class,
                new AABB(pos).inflate(0.8, 1.2, 0.8),
                stand -> stand.entityTags().contains(SEAT_TAG)
                        && stand.entityTags().contains(POS_PREFIX + pos.asLong())
                        && !stand.getPassengers().isEmpty()
        ).isEmpty();
    }
}
