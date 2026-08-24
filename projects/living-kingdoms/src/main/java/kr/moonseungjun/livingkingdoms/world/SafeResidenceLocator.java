package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
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
    private static final int RESIDENCE_CHUNK_RADIUS = 1;

    private SafeResidenceLocator() {
    }

    /** Intended citizen-quarter anchor used only to select the nearest authored tenement. */
    public static BlockPos preferredResidence(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, homelandId);
        if (!"erden_city_room".equals(residenceId)) {
            throw new IllegalArgumentException("Inactive residence: " + residenceId);
        }
        int x = site.centerX() + 320;
        int z = site.centerZ() + 180;
        int surfaceY = authoredSurfaceY(x, z);
        return new BlockPos(x, surfaceY + 1, z);
    }

    /**
     * Returns only a verified target inside the deterministic authored tenement selected for the
     * player. Roofs, walls, roads and synthetic emergency floors are never accepted as residences.
     * While the target building is still streaming, transient chunk tickets keep its 3x3 area warm
     * and this method returns null so the blocking entry screen remains active.
     */
    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        BlockPos preferred = preferredResidence(level, homelandId, residenceId);
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = playerResidenceEntrance(preferred);
        if (entrance == null) return null;

        requestResidenceArea(level, entrance);
        if (!ErdenUrbanResidenceResolver.isResidenceReady(level, entrance)) return null;

        BlockPos target = ErdenUrbanResidenceResolver.resolveHomeTarget(level, entrance, 0);
        return isAuthoritativePlayerResidence(level, homelandId, residenceId, target) ? target : null;
    }

    /** Read-only proof that a target is the exact authored upper-room target of the player tenement. */
    public static boolean isAuthoritativePlayerResidence(
            ServerLevel level, String homelandId, String residenceId, BlockPos target) {
        if (target == null || !isWalkable(level, target)) return false;
        BlockPos preferred = preferredResidence(level, homelandId, residenceId);
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = playerResidenceEntrance(preferred);
        if (entrance == null || !ErdenUrbanResidenceResolver.isResidenceReady(level, entrance)) return false;
        BlockPos expected = ErdenUrbanResidenceResolver.resolveHomeTarget(level, entrance, 0);
        return target.equals(expected);
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
                && level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir();
    }

    private static void requestResidenceArea(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        int centerChunkX = entrance.x() >> 4;
        int centerChunkZ = entrance.z() >> 4;
        for (int dx = -RESIDENCE_CHUNK_RADIUS; dx <= RESIDENCE_CHUNK_RADIUS; dx++) {
            for (int dz = -RESIDENCE_CHUNK_RADIUS; dz <= RESIDENCE_CHUNK_RADIUS; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }
        }
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance playerResidenceEntrance(BlockPos preferred) {
        return ExternalUrbanFabricBuilder.entrances().stream()
                .filter(entrance -> "tenement".equals(entrance.role()))
                .min(Comparator.comparingLong(entrance -> distanceSquared(
                        entrance.x(), entrance.z(), preferred.getX(), preferred.getZ())))
                .orElse(null);
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
        for (int dy = 0; dy <= 1; dy++) {
            level.setBlock(feet.above(dy), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        if (!isWalkable(level, feet)) {
            throw new IllegalStateException("Unable to create safe civic target at " + feet);
        }
        return feet;
    }

    private static int authoredSurfaceY(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }
}
