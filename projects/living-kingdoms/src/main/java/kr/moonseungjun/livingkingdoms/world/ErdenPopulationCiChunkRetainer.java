package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Keeps only population and physical-economy sample buildings loaded during headless diagnostics. */
public final class ErdenPopulationCiChunkRetainer {
    private static final boolean ENABLED =
            "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    private static final int RETAIN_INTERVAL_TICKS = 5;
    private static MinecraftServer activeServer;

    private ErdenPopulationCiChunkRetainer() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED
                || event.getServer().getTickCount() % RETAIN_INTERVAL_TICKS != 0) return;
        if (activeServer != event.getServer()) {
            activeServer = event.getServer();
        }
        ServerLevel level = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        if (population.households().isEmpty()) return;

        ErdenPopulationSavedData.Household sample = population.households().getFirst();
        retainBuilding(level, sample.homeX(), sample.homeZ());
        for (ErdenPopulationSavedData.Resident resident : sample.residents()) {
            if (resident.worker()) retainBuilding(level, resident.workX(), resident.workZ());
        }
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ErdenAuthoritativeEconomyManager.ciEntrances()) {
            retainBuilding(level, entrance.x(), entrance.z());
        }
    }

    private static void retainBuilding(ServerLevel level, int x, int z) {
        ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement = null;
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement candidate
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            if (candidate.entrance().x() == x && candidate.entrance().z() == z) {
                placement = candidate;
                break;
            }
        }
        if (placement == null) return;

        // Population readiness is defined by the authored ground plan plus its verified upper
        // residence. Retaining only the old 7x9 doorway room can leave most of a 34x38 source
        // fragment unloaded forever in headless CI. Refresh the actual placement footprint with
        // one chunk of halo, and explicitly refresh all authored-ground plan chunks. These are
        // transient PORTAL leases only; no persistent forced chunk state is written.
        ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, placement.entrance());
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
