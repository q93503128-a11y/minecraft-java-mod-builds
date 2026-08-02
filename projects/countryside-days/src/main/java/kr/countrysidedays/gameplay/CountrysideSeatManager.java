package kr.countrysidedays.gameplay;

import kr.countrysidedays.world.CountrysideRegionManager;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Makes generated stair chairs usable by players without a custom client entity. */
public final class CountrysideSeatManager {
    public static final String SEAT_TAG = "cd_seat_v14";
    private static final String POS_PREFIX = "cd_seat_pos_";

    private CountrysideSeatManager() {
    }

    public static void onUseBlock(UseItemOnBlockEvent event) {
        if (event.isCanceled()
                || event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player)
                || !event.getItemStack().isEmpty()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.OAK_STAIRS) || !state.hasProperty(StairBlock.FACING)) return;
        if (!CountrysideRegionManager.isInsideCountryside(level, pos) || !canUseChair(level, player, pos)) return;

        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
        if (player.getVehicle() instanceof ArmorStand current
                && current.entityTags().contains(SEAT_TAG)) {
            player.stopRiding();
            current.discard();
            return;
        }
        if (isSeatOccupied(level, pos)) return;

        ArmorStand seat = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        if (seat == null) return;
        seat.setPos(pos.getX() + 0.5, pos.getY() - 0.35, pos.getZ() + 0.5);
        seat.setInvisible(true);
        seat.setMarker(true);
        seat.setNoGravity(true);
        seat.setInvulnerable(true);
        seat.addTag(SEAT_TAG);
        seat.addTag(POS_PREFIX + pos.asLong());
        if (!level.addFreshEntity(seat)) return;

        Direction look = state.getValue(StairBlock.FACING).getOpposite();
        float yaw = look.toYRot();
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        if (!player.startRiding(seat, true)) seat.discard();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 40L != 0L) return;
        CountrysideWorldData data = CountrysideWorldData.get(event.getServer());
        BlockPos village = data.homesteadOrigin().orElse(null);
        if (village == null) return;
        level.getEntitiesOfClass(
                ArmorStand.class,
                new AABB(village).inflate(1800.0, 48.0, 1800.0),
                stand -> stand.entityTags().contains(SEAT_TAG)
        ).forEach(stand -> {
            if (stand.getPassengers().isEmpty()) stand.discard();
        });
    }

    public static boolean isSeatOccupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(
                ArmorStand.class,
                new AABB(pos).inflate(0.8, 1.2, 0.8),
                stand -> stand.entityTags().contains(SEAT_TAG)
                        && stand.entityTags().contains(POS_PREFIX + pos.asLong())
                        && !stand.getPassengers().isEmpty()
        ).isEmpty();
    }

    private static boolean canUseChair(ServerLevel level, ServerPlayer player, BlockPos pos) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        CountrysideWorldData.PlayerEstate estate = data.estateAt(pos).orElse(null);
        if (estate == null) return true;
        if (PlayerEstateLayout.isRestaurantArea(estate.originPos(), pos)) {
            return SharedRestaurantAccess.isStaff(data, player.getUUID());
        }
        return estate.isOwner(player.getUUID());
    }
}
