package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Bounded CI-only verification for the authored starter realm. */
public final class StarterRealmDiagnostics {
    private StarterRealmDiagnostics() {
    }

    public static void runIfRequested(MinecraftServer server) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;

        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) throw new IllegalStateException("Living Kingdoms realm is unavailable during diagnostics");

        long started = System.nanoTime();
        try {
            Method ensureHomeland = StarterRealmManager.class.getDeclaredMethod(
                    "ensureHomeland", ServerLevel.class, String.class
            );
            Method prepareSpawn = StarterRealmManager.class.getDeclaredMethod(
                    "prepareSpawn", ServerLevel.class, PlayableOriginCatalog.ResidenceOption.class
            );
            ensureHomeland.setAccessible(true);
            prepareSpawn.setAccessible(true);

            for (String homelandId : PlayableOriginCatalog.HOMELANDS) {
                invoke(ensureHomeland, realm, homelandId);
                RealmRevisionFourManager.ensureRegion(realm, homelandId);
            }
            for (PlayableOriginCatalog.ResidenceOption residence : PlayableOriginCatalog.residences().values()) {
                invoke(prepareSpawn, realm, residence);
                verifySpawn(realm, residence);
            }

            StarterRealmSavedData state = realm.getDataStorage().computeIfAbsent(StarterRealmSavedData.TYPE);
            if (state.generatedRegionCount() != PlayableOriginCatalog.HOMELANDS.size()) {
                throw new IllegalStateException("Expected " + PlayableOriginCatalog.HOMELANDS.size()
                        + " generated starter homelands but found " + state.generatedRegionCount());
            }

            StarterRealmUpgradeSavedData upgrades = realm.getDataStorage()
                    .computeIfAbsent(StarterRealmUpgradeSavedData.TYPE);
            for (String homelandId : PlayableOriginCatalog.HOMELANDS) {
                if (upgrades.revision(homelandId) < RealmRevisionFourManager.CURRENT_REVISION) {
                    throw new IllegalStateException("Missing authored terrain revision four for " + homelandId);
                }
            }

            verifyTerrainVariation(realm, 0, 0, "erden_kingdom");
            verifyTerrainVariation(realm, 1240, 35, "silvana_forest");
            verifyTerrainVariation(realm, -1170, 38, "kardum_league");
            verifyErdenAccess(realm);
            verifySupportedMarket(realm);
            verifySolidGable(realm);
            verifyConnectedPalisade(realm);

            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            if (elapsedMs > 180_000L) {
                throw new IllegalStateException("Authored realm migration exceeded 180 seconds: " + elapsedMs + "ms");
            }
            LivingKingdoms.LOGGER.info(
                    "LK_REALM_DIAGNOSTIC_PASS regions={} residences={} upgrades=4 terrain_varied=true lots_drained=true roofs_solid=true palisade_connected=true migration_ms={}",
                    state.generatedRegionCount(), PlayableOriginCatalog.residences().size(), elapsedMs
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Living Kingdoms starter realm diagnostic could not run", exception);
        }
    }

    private static void invoke(Method method, Object... arguments)
            throws InvocationTargetException, IllegalAccessException {
        method.invoke(null, arguments);
    }

    private static void verifySpawn(ServerLevel realm, PlayableOriginCatalog.ResidenceOption residence) {
        BlockPos feet = new BlockPos(residence.spawnX(), residence.spawnY(), residence.spawnZ());
        BlockPos floor = feet.below();
        if (realm.getBlockState(floor).isAir()) {
            throw new IllegalStateException("Air floor at starter residence " + residence.id() + ": " + floor);
        }
        if (!realm.getBlockState(feet).isAir() || !realm.getBlockState(feet.above()).isAir()) {
            throw new IllegalStateException("Blocked headroom at starter residence " + residence.id() + ": " + feet);
        }
        if (residence.spawnY() <= realm.getMinY() + 4) {
            throw new IllegalStateException("Unsafe low starter residence " + residence.id());
        }
    }

    private static void verifyTerrainVariation(ServerLevel realm, int cx, int cz, String regionId) {
        Set<Integer> heights = new HashSet<>();
        for (int[] offset : new int[][]{
                {112, 36}, {-124, 44}, {76, -128}, {-148, -60}, {164, 72}, {-178, 96}
        }) {
            heights.add(surfaceY(realm, cx + offset[0], cz + offset[1]));
        }
        if (heights.size() < 3) {
            throw new IllegalStateException("Terrain is still effectively flat in " + regionId + ": " + heights);
        }
    }

    private static void verifyErdenAccess(ServerLevel realm) {
        for (BlockPos pos : new BlockPos[]{
                new BlockPos(16, 66, 7), new BlockPos(-29, 66, -31),
                new BlockPos(105, 66, 65), new BlockPos(-104, 66, 92),
                new BlockPos(84, 66, -112)
        }) {
            if (!realm.getBlockState(pos).isAir() || !realm.getBlockState(pos.above()).isAir()) {
                throw new IllegalStateException("Erden approach remains blocked or buried at " + pos);
            }
        }
    }

    private static void verifySupportedMarket(ServerLevel realm) {
        BlockPos post = new BlockPos(-12, 70, -9);
        BlockPos roof = new BlockPos(-13, 71, -10);
        if (realm.getBlockState(post).isAir() || realm.getBlockState(roof).isAir()) {
            throw new IllegalStateException("Erden market canopy is detached from its supports");
        }
    }

    private static void verifySolidGable(ServerLevel realm) {
        // First house: adjacent two-block roof bands must touch vertically and horizontally.
        for (BlockPos pos : new BlockPos[]{
                new BlockPos(9, 70, 7), new BlockPos(9, 70, 8),
                new BlockPos(9, 71, 8), new BlockPos(9, 71, 9),
                new BlockPos(9, 72, 9), new BlockPos(9, 72, 10)
        }) {
            if (realm.getBlockState(pos).isAir()) {
                throw new IllegalStateException("Detached or missing Erden gable roof block at " + pos);
            }
        }
    }

    private static void verifyConnectedPalisade(ServerLevel realm) {
        for (int x = -60; x <= -50; x++) {
            if (realm.getBlockState(new BlockPos(x, 67, -78)).isAir()) {
                throw new IllegalStateException("Gap in Erden palisade at x=" + x);
            }
        }
    }

    private static int surfaceY(ServerLevel realm, int x, int z) {
        for (int y = 120; y >= 60; y--) {
            if (!realm.getBlockState(new BlockPos(x, y, z)).isAir()) return y;
        }
        return 59;
    }
}
