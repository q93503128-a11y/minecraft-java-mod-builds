package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.level.ServerLevel;

/**
 * Temporarily keeps exactly the chosen starter tenement footprint resident while its authored
 * interior/upper route finishes. The underlying PORTAL leases are transient and expire when the
 * player is placed because this method stops refreshing them; no persistent forced chunks are saved.
 */
final class ErdenPlayerResidenceChunkRetainer {
    private ErdenPlayerResidenceChunkRetainer() {
    }

    static void retain(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement = null;
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement candidate
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            if (candidate.entrance().x() == entrance.x()
                    && candidate.entrance().z() == entrance.z()) {
                placement = candidate;
                break;
            }
        }
        if (placement == null) return;

        int minChunkX = Math.floorDiv(placement.minX(), 16) - 1;
        int maxChunkX = Math.floorDiv(placement.maxX(), 16) + 1;
        int minChunkZ = Math.floorDiv(placement.minZ(), 16) - 1;
        int maxChunkZ = Math.floorDiv(placement.maxZ(), 16) + 1;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                }
                ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);
            }
        }
    }
}
