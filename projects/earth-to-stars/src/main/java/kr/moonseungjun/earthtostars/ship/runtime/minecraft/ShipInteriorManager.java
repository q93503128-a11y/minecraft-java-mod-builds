package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.interior.InteriorAnchor;
import kr.moonseungjun.earthtostars.ship.interior.InteriorRef;
import kr.moonseungjun.earthtostars.ship.interior.InteriorSlotLayout;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.InteriorSavedData;
import kr.moonseungjun.earthtostars.space.SpaceLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;

public final class ShipInteriorManager {
    private static final int ROOM_RADIUS = 6;

    private ShipInteriorManager() {
    }

    public static boolean enterNearest(ServerPlayer player) {
        if (player.level().dimension().equals(SpaceLevels.SHIP_INTERIORS)) {
            return false;
        }
        ShipState ship = ShipRuntimeManager.nearestInteriorAccessible(player).orElse(null);
        if (ship == null) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        ServerLevel interiors = server.getLevel(SpaceLevels.SHIP_INTERIORS);
        if (interiors == null) {
            return false;
        }
        InteriorRef ref = InteriorSavedData.get(server).getOrAllocate(ship.shipId());
        ensureP0Room(interiors, ref);
        InteriorAnchor anchor = ref.anchor();
        return teleport(player, interiors, new Vec3(anchor.x(), anchor.y(), anchor.z()));
    }

    public static boolean exit(ServerPlayer player) {
        if (!player.level().dimension().equals(SpaceLevels.SHIP_INTERIORS)) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        Optional<ShipId> linkedShip = InteriorSavedData.get(server).findShipAt(player.getX(), player.getZ());
        if (linkedShip.isPresent()) {
            Optional<ShipRuntimeManager.ExteriorAnchor> exterior = ShipRuntimeManager.exteriorAnchor(linkedShip.orElseThrow());
            if (exterior.isPresent()) {
                ShipRuntimeManager.ExteriorAnchor anchor = exterior.orElseThrow();
                Vec3 destination = new Vec3(
                        anchor.transform().position().x(),
                        anchor.transform().position().y() + 2.5D,
                        anchor.transform().position().z()
                );
                return teleport(player, anchor.level(), destination);
            }
        }
        return returnToEarth(player);
    }

    public static void recoverUnlinkedInteriorPlayer(ServerPlayer player) {
        if (!player.level().dimension().equals(SpaceLevels.SHIP_INTERIORS)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (InteriorSavedData.get(server).findShipAt(player.getX(), player.getZ()).isEmpty()) {
            returnToEarth(player);
        }
    }

    public static Optional<ShipId> linkedShip(ServerPlayer player) {
        if (!player.level().dimension().equals(SpaceLevels.SHIP_INTERIORS)) {
            return Optional.empty();
        }
        return InteriorSavedData.get(player.level().getServer()).findShipAt(player.getX(), player.getZ());
    }

    private static boolean returnToEarth(ServerPlayer player) {
        ServerLevel earth = player.level().getServer().getLevel(Level.OVERWORLD);
        if (earth == null) {
            return false;
        }
        BlockPos spawn = earth.getSharedSpawnPos();
        Vec3 destination = new Vec3(spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D);
        return teleport(player, earth, destination);
    }

    private static boolean teleport(ServerPlayer player, ServerLevel target, Vec3 destination) {
        ServerPlayer result = player.teleport(new TeleportTransition(
                target,
                destination,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                Set.of(),
                TeleportTransition.DO_NOTHING
        ));
        return result != null;
    }

    private static void ensureP0Room(ServerLevel level, InteriorRef ref) {
        InteriorAnchor anchor = ref.anchor();
        int centerX = (int) Math.round(anchor.x());
        int centerZ = (int) Math.round(anchor.z());
        BlockPos marker = new BlockPos(centerX, InteriorSlotLayout.FLOOR_Y, centerZ);
        if (level.getBlockState(marker).is(Blocks.IRON_BLOCK)) {
            return;
        }

        for (int dx = -ROOM_RADIUS; dx <= ROOM_RADIUS; dx++) {
            for (int dz = -ROOM_RADIUS; dz <= ROOM_RADIUS; dz++) {
                BlockPos floor = new BlockPos(centerX + dx, InteriorSlotLayout.FLOOR_Y, centerZ + dz);
                level.setBlockAndUpdate(floor, Blocks.SMOOTH_STONE.defaultBlockState());
                if (Math.abs(dx) == ROOM_RADIUS || Math.abs(dz) == ROOM_RADIUS) {
                    level.setBlockAndUpdate(floor.above(), Blocks.BARRIER.defaultBlockState());
                    level.setBlockAndUpdate(floor.above(2), Blocks.BARRIER.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(marker, Blocks.IRON_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(marker.offset(4, 0, 4), Blocks.SEA_LANTERN.defaultBlockState());
        level.setBlockAndUpdate(marker.offset(-4, 0, 4), Blocks.SEA_LANTERN.defaultBlockState());
        level.setBlockAndUpdate(marker.offset(4, 0, -4), Blocks.SEA_LANTERN.defaultBlockState());
        level.setBlockAndUpdate(marker.offset(-4, 0, -4), Blocks.SEA_LANTERN.defaultBlockState());
    }
}
