package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;

/** Keeps only the first household's test buildings loaded during headless realm diagnostics. */
public final class ErdenPopulationCiChunkRetainer {
    private static final boolean ENABLED =
            "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    private static final int RETAIN_INTERVAL_TICKS = 5;
    private static final Set<Long> RETAINED_CHUNKS = new HashSet<>();

    private static MinecraftServer activeServer;

    private ErdenPopulationCiChunkRetainer() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED
                || event.getServer().getTickCount() % RETAIN_INTERVAL_TICKS != 0) return;
        if (activeServer != event.getServer()) {
            activeServer = event.getServer();
            RETAINED_CHUNKS.clear();
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
    }

    private static void retainBuilding(ServerLevel level, int x, int z) {
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = null;
        for (ExternalUrbanFabricBuilder.UrbanEntrance candidate
                : ExternalUrbanFabricBuilder.entrances()) {
            if (candidate.x() == x && candidate.z() == z) {
                entrance = candidate;
                break;
            }
        }
        if (entrance == null) return;

        int deltaX = entrance.roadX() - entrance.x();
        int deltaZ = entrance.roadZ() - entrance.z();
        int inwardX;
        int inwardZ;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            inwardX = deltaX >= 0 ? -1 : 1;
            inwardZ = 0;
        } else {
            inwardX = 0;
            inwardZ = deltaZ >= 0 ? -1 : 1;
        }
        int rightX = -inwardZ;
        int rightZ = inwardX;
        int minX = x;
        int maxX = x;
        int minZ = z;
        int maxZ = z;
        for (int lateral : new int[]{-3, 3}) {
            for (int forward : new int[]{1, 9}) {
                int blockX = x + inwardX * forward + rightX * lateral;
                int blockZ = z + inwardZ * forward + rightZ * lateral;
                minX = Math.min(minX, blockX);
                maxX = Math.max(maxX, blockX);
                minZ = Math.min(minZ, blockZ);
                maxZ = Math.max(maxZ, blockZ);
            }
        }
        for (int chunkX = Math.floorDiv(minX, 16);
             chunkX <= Math.floorDiv(maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(minZ, 16);
                 chunkZ <= Math.floorDiv(maxZ, 16); chunkZ++) {
                if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                }

                // requestChunk releases its own temporary ticket when construction finishes.
                // Reassert the diagnostic ticket afterwards so the interior, upper floor and
                // resident materialisation stages all observe the same loaded building.
                level.setChunkForced(chunkX, chunkZ, true);
                long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
                if (RETAINED_CHUNKS.add(key) || !level.hasChunk(chunkX, chunkZ)) {
                    level.getChunk(chunkX, chunkZ);
                }
            }
        }
    }
}
