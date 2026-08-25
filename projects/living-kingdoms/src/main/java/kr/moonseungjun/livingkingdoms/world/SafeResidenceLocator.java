package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;

/** Resolves and verifies the active Erden residence and civic custody cells. */
public final class SafeResidenceLocator {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final int STARTER_DISTRICT_OFFSET_X = 320;
    private static final int STARTER_DISTRICT_OFFSET_Z = 180;

    private SafeResidenceLocator() {
    }

    /**
     * Returns the source-authored home target for the deterministic starter tenement. This method
     * never scans roofs, roads or walls and never creates a floor. The returned point is metadata;
     * {@link #residenceIfReady} is the authority for whether the room is physically ready.
     */
    public static BlockPos preferredResidence(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, homelandId);
        requireResidence(residenceId);
        ExternalUrbanFabricBuilder.UrbanEntrance tenement = starterTenement(site);
        BlockPos target = ErdenUrbanAuthoredGroundPlanCatalog.homeTarget(tenement, 0);
        if (target == null) {
            throw new IllegalStateException("Starter tenement has no authored home target at "
                    + tenement.x() + "," + tenement.z());
        }
        return target;
    }

    /** Returns a verified room inside the authored starter tenement, or null while its chunk/interior is still streaming. */
    public static BlockPos residenceIfReady(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, homelandId);
        requireResidence(residenceId);
        ExternalUrbanFabricBuilder.UrbanEntrance tenement = starterTenement(site);
        BlockPos authoredTarget = ErdenUrbanAuthoredGroundPlanCatalog.homeTarget(tenement, 0);
        if (authoredTarget == null) return null;

        int chunkX = authoredTarget.getX() >> 4;
        int chunkZ = authoredTarget.getZ() >> 4;
        ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
        if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)
                || !level.hasChunkAt(authoredTarget)) return null;

        BlockPos target = ErdenUrbanResidenceResolver.resolveHomeTarget(level, tenement, 0);
        return target != null && isWalkable(level, target) ? target : null;
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        BlockPos target = residenceIfReady(level, homelandId, residenceId);
        if (target == null) {
            throw new IllegalStateException("Authored Erden starter residence is not physically ready");
        }
        return target;
    }

    static ExternalUrbanFabricBuilder.UrbanEntrance starterTenement(ServerLevel level, String homelandId) {
        return starterTenement(requiredSite(level, homelandId));
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance starterTenement(
            RealmSiteLayoutSavedData.RealmSite site) {
        int preferredX = site.centerX() + STARTER_DISTRICT_OFFSET_X;
        int preferredZ = site.centerZ() + STARTER_DISTRICT_OFFSET_Z;
        return ExternalUrbanFabricBuilder.entrances().stream()
                .filter(entrance -> "tenement".equals(entrance.role()))
                .min(Comparator
                        .comparingLong((ExternalUrbanFabricBuilder.UrbanEntrance entrance) ->
                                distanceSquared(preferredX, preferredZ, entrance.x(), entrance.z()))
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                .orElseThrow(() -> new IllegalStateException("Erden has no authored starter tenement"));
    }

    public static BlockPos preferredJail(ServerLevel level, String jurisdiction) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, jurisdiction);
        int x = site.centerX() - 360;
        int z = site.centerZ() - 80;
        int surfaceY = authoredSurfaceY(x, z);
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
        if (feet == null || !level.hasChunkAt(feet)) return false;
        return level.getBlockState(feet.below()).isSolid()
                && level.getBlockState(feet.below()).getFluidState().isEmpty()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir();
    }

    private static RealmSiteLayoutSavedData.RealmSite requiredSite(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, homelandId);
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) {
            throw new IllegalStateException("Authored site is not ready: " + homelandId);
        }
        return site;
    }

    private static void requireResidence(String residenceId) {
        if (!"erden_city_room".equals(residenceId)) {
            throw new IllegalArgumentException("Inactive residence: " + residenceId);
        }
    }

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
            throw new IllegalStateException("Unable to create safe custody cell at " + feet);
        }
        return feet;
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }

    private static int authoredSurfaceY(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }
}
