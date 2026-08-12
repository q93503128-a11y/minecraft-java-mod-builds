package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only bridge between exact source-fragment classification and the finished-world pre-conversion
 * survey. No compatibility geometry is skipped here: this audit only establishes which individual
 * buildings have both halves of the proof required by the source-native conversion gate.
 */
public final class ErdenUrbanTopologyCorrelationAudit {
    public static final int AUDIT_REVISION = 1;
    private static final int EXPECTED_BUILDINGS = 233;

    private static MinecraftServer activeServer;
    private static final Set<Long> CHECKED = new HashSet<>();
    private static final Set<Long> DUAL_MULTILEVEL_READY = new HashSet<>();
    private static int sourceMultilevel;
    private static int sourceGroundOnly;
    private static int sourceFallback;
    private static int runtimeMultilevel;
    private static boolean completionLogged;

    private ErdenUrbanTopologyCorrelationAudit() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances =
                ExternalUrbanFabricBuilder.entrances();
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : entrances) {
            long key = key(entrance.x(), entrance.z());
            if (CHECKED.contains(key)) continue;
            ErdenUrbanAuthoredInteriorSurvey.Profile runtime =
                    ErdenUrbanAuthoredInteriorSurvey.profile(entrance.x(), entrance.z());
            if (runtime == null) continue;
            ErdenUrbanPlacedTopologyCatalog.PlacementProfile source =
                    ErdenUrbanPlacedTopologyCatalog.profile(entrance.x(), entrance.z());
            if (source == null) {
                throw new IllegalStateException("Missing exact Erden topology placement for surveyed entrance "
                        + entrance.x() + "," + entrance.z());
            }

            CHECKED.add(key);
            switch (source.classification()) {
                case AUTHORED_MULTILEVEL -> sourceMultilevel++;
                case AUTHORED_GROUND_ONLY -> sourceGroundOnly++;
                case FALLBACK -> sourceFallback++;
            }
            if (runtime.authoredMultilevelCandidate()) runtimeMultilevel++;
            boolean dualReady = source.classification()
                    == ErdenUrbanPlacedTopologyCatalog.Classification.AUTHORED_MULTILEVEL
                    && runtime.authoredMultilevelCandidate();
            if (dualReady) DUAL_MULTILEVEL_READY.add(key);

            if (diagnosticMode() && (dualReady
                    || source.classification()
                    == ErdenUrbanPlacedTopologyCatalog.Classification.AUTHORED_MULTILEVEL)) {
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_TOPOLOGY_CORRELATION entrance={},{} role={} fragment={} source={} runtime_multilevel={} runtime_reachable={} runtime_vertical_span={} runtime_stairs={} dual_ready={} read_only=true",
                        entrance.x(), entrance.z(), entrance.role(), source.fragmentKey(),
                        source.classification(), runtime.authoredMultilevelCandidate(),
                        runtime.reachableCells(), runtime.verticalSpan(), runtime.stairs(), dualReady);
            }
        }

        if (!completionLogged && CHECKED.size() == EXPECTED_BUILDINGS) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden exact/runtime interior topology correlation buildings={} source_multilevel={} source_ground_only={} source_fallback={} runtime_multilevel={} dual_multilevel_ready={} read_only=true world_mutations=0 revision={}",
                    CHECKED.size(), sourceMultilevel, sourceGroundOnly, sourceFallback,
                    runtimeMultilevel, DUAL_MULTILEVEL_READY.size(), AUDIT_REVISION);
        }
    }

    public static boolean dualMultilevelReady(int entranceX, int entranceZ) {
        return DUAL_MULTILEVEL_READY.contains(key(entranceX, entranceZ));
    }

    public static int checkedCount() {
        return CHECKED.size();
    }

    public static int dualReadyCount() {
        return DUAL_MULTILEVEL_READY.size();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        CHECKED.clear();
        DUAL_MULTILEVEL_READY.clear();
        sourceMultilevel = 0;
        sourceGroundOnly = 0;
        sourceFallback = 0;
        runtimeMultilevel = 0;
        completionLogged = false;
    }

    private static boolean diagnosticMode() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || "1".equals(System.getenv("LIVING_KINGDOMS_CI_ENTRY_TRAVERSAL"));
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
