package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;

/** Resolves and verifies the active Erden residence and civic custody cells. */
public final class SafeResidenceLocator {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final int CITIZEN_QUARTER_X_OFFSET = 320;
    private static final int CITIZEN_QUARTER_Z_OFFSET = 180;

    private SafeResidenceLocator() {
    }

    /**
     * Returns the authored citizen-quarter building entrance used for the player's rented room.
     * This is metadata only; it never invents a platform or treats a roof as a residence.
     */
    public static BlockPos preferredResidence(ServerLevel level, String homelandId, String residenceId) {
        ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement =
                authoredResidencePlacement(level, homelandId, residenceId);
        return new BlockPos(
                placement.entrance().x(),
                RealmSitePlanner.surfaceY(level, placement.entrance().x(), placement.entrance().z()) + 1,
                placement.entrance().z());
    }

    /** Requests the exact chunks occupied by the selected real tenement and keeps them transiently loaded. */
    public static void prepareResidence(ServerLevel level, String homelandId, String residenceId) {
        ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement =
                authoredResidencePlacement(level, homelandId, residenceId);
        int minChunkX = placement.minX() >> 4;
        int maxChunkX = placement.maxX() >> 4;
        int minChunkZ = placement.minZ() >> 4;
        int maxChunkZ = placement.maxZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                // PORTAL tickets are timeout-based. Refresh only while placement is pending; once the
                // player is placed, refreshes stop and no persistent forced chunk remains.
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }
        }
    }

    /** Returns only a verified authored room target; null means construction must continue. */
    public static BlockPos tryResidence(ServerLevel level, String homelandId, String residenceId) {
        ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement =
                authoredResidencePlacement(level, homelandId, residenceId);
        prepareResidence(level, homelandId, residenceId);

        for (int chunkX = placement.minX() >> 4; chunkX <= placement.maxX() >> 4; chunkX++) {
            for (int chunkZ = placement.minZ() >> 4; chunkZ <= placement.maxZ() >> 4; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)
                        || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                    return null;
                }
            }
        }

        ExternalUrbanFabricBuilder.UrbanEntrance entrance = placement.entrance();
        if (!ErdenUrbanResidenceResolver.isResidenceReady(level, entrance)) return null;
        BlockPos target = ErdenUrbanResidenceResolver.resolveHomeTarget(level, entrance, 0);
        if (target == null) return null;
        ErdenUrbanResidenceResolver.verifyTargetOrThrow(level, target, "player-authored-tenement");
        ErdenUrbanAuthoredUpperRouteManager.verifyOrThrow(level, entrance);
        return target;
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        BlockPos target = tryResidence(level, homelandId, residenceId);
        if (target == null) {
            throw new IllegalStateException(
                    "Authored player residence is not ready; synthetic fallback is forbidden");
        }
        return target;
    }

    public static ExternalUrbanFabricBuilder.UrbanEntrance residenceEntrance(
            ServerLevel level, String homelandId, String residenceId) {
        return authoredResidencePlacement(level, homelandId, residenceId).entrance();
    }

    public static BlockPos preferredJail(ServerLevel level, String jurisdiction) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, jurisdiction);
        int x = site.centerX() - 360;
        int z = site.centerZ() - 80;
        int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
        return new BlockPos(x, surfaceY + 1, z);
    }

    public static BlockPos jail(ServerLevel level, String jurisdiction) {
        BlockPos preferred = preferredJail(level, jurisdiction);
        if (!level.hasChunkAt(preferred)) return preferred;
        return findOrCreateWalkable(level, preferred, Blocks.STONE_BRICKS, 8, 10);
    }

    public static float yaw(String homelandId, String residenceId) {
        if (!"erden_kingdom".equals(homelandId) || !"erden_city_room".equals(residenceId)) {
            throw new IllegalArgumentException("Inactive origin residence");
        }
        return 180.0F;
    }

    public static boolean isWalkable(ServerLevel level, BlockPos feet) {
        if (!level.hasChunkAt(feet)) return false;
        return level.getBlockState(feet.below()).isSolid()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir()
                && level.getBlockState(feet.above(2)).isAir();
    }

    private static ExternalUrbanFabricBuilder.UrbanBuildingPlacement authoredResidencePlacement(
            ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, homelandId);
        if (!"erden_kingdom".equals(homelandId) || !"erden_city_room".equals(residenceId)) {
            throw new IllegalArgumentException("Inactive residence: " + residenceId);
        }
        int targetX = site.centerX() + CITIZEN_QUARTER_X_OFFSET;
        int targetZ = site.centerZ() + CITIZEN_QUARTER_Z_OFFSET;
        return ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics().stream()
                .filter(placement -> "tenement".equals(placement.role()))
                .filter(placement -> ErdenUrbanAuthoredUpperRouteManager.isEligible(placement.entrance()))
                .min(Comparator
                        .comparingLong((ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement) ->
                                distanceSquared(placement.entrance().x(), placement.entrance().z(), targetX, targetZ))
                        .thenComparingInt(placement -> placement.entrance().x())
                        .thenComparingInt(placement -> placement.entrance().z()))
                .orElseThrow(() -> new IllegalStateException(
                        "No authored upper-floor Erden tenement is available for the player residence"));
    }

    private static long distanceSquared(int x, int z, int targetX, int targetZ) {
        long dx = (long) x - targetX;
        long dz = (long) z - targetZ;
        return dx * dx + dz * dz;
    }

    private static RealmSiteLayoutSavedData.RealmSite requiredSite(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, homelandId);
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) {
            throw new IllegalStateException("Authored site is not ready: " + homelandId);
        }
        return site;
    }

    /** Jail-only fallback retained for custody safety; player homes never call this path. */
    private static BlockPos findOrCreateWalkable(ServerLevel level, BlockPos preferred, Block floor,
                                                  int horizontalRadius, int verticalRadius) {
        if (!level.hasChunkAt(preferred)) return preferred;
        for (int dy = 0; dy <= verticalRadius; dy++) {
            for (int sign : new int[]{1, -1}) {
                if (dy == 0 && sign < 0) continue;
                int y = preferred.getY() + dy * sign;
                for (int radius = 0; radius <= horizontalRadius; radius++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        BlockPos north = new BlockPos(preferred.getX() + dx, y, preferred.getZ() - radius);
                        if (isWalkable(level, north)) return north;
                        BlockPos south = new BlockPos(preferred.getX() + dx, y, preferred.getZ() + radius);
                        if (isWalkable(level, south)) return south;
                    }
                    for (int dz = -radius + 1; dz < radius; dz++) {
                        BlockPos west = new BlockPos(preferred.getX() - radius, y, preferred.getZ() + dz);
                        if (isWalkable(level, west)) return west;
                        BlockPos east = new BlockPos(preferred.getX() + radius, y, preferred.getZ() + dz);
                        if (isWalkable(level, east)) return east;
                    }
                }
            }
        }
        return secure(level, preferred, floor);
    }

    private static BlockPos secure(ServerLevel level, BlockPos feet, Block floor) {
        if (!level.hasChunkAt(feet)) return feet;
        BlockPos floorPos = feet.below();
        level.setBlock(floorPos, floor.defaultBlockState(), UPDATE_FLAGS);
        for (int dy = 0; dy <= 2; dy++) {
            level.setBlock(feet.above(dy), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        if (!isWalkable(level, feet)) {
            throw new IllegalStateException("Unable to create safe custody spawn at " + feet);
        }
        return feet;
    }
}
