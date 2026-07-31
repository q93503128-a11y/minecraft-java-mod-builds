package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Bounded startup diagnostic used by CI and explicit developer verification.
 * It is inert in normal games unless LIVING_KINGDOMS_CI_REALM_TEST=1.
 */
public final class StarterRealmDiagnostics {
    private StarterRealmDiagnostics() {
    }

    public static void runIfRequested(MinecraftServer server) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) {
            return;
        }

        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            throw new IllegalStateException("Living Kingdoms realm is unavailable during diagnostics");
        }

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
            }
            for (PlayableOriginCatalog.ResidenceOption residence : PlayableOriginCatalog.residences().values()) {
                invoke(prepareSpawn, realm, residence);
                verifySpawn(realm, residence);
            }

            StarterRealmSavedData state = realm.getDataStorage().computeIfAbsent(StarterRealmSavedData.TYPE);
            if (state.generatedRegionCount() != PlayableOriginCatalog.HOMELANDS.size()) {
                throw new IllegalStateException(
                        "Expected " + PlayableOriginCatalog.HOMELANDS.size()
                                + " generated starter homelands but found " + state.generatedRegionCount()
                );
            }

            LivingKingdoms.LOGGER.info(
                    "LK_REALM_DIAGNOSTIC_PASS regions={} residences={}",
                    state.generatedRegionCount(),
                    PlayableOriginCatalog.residences().size()
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Living Kingdoms starter realm diagnostic could not run", exception);
        }
    }

    private static void invoke(Method method, Object... arguments)
            throws InvocationTargetException, IllegalAccessException {
        method.invoke(null, arguments);
    }

    private static void verifySpawn(
            ServerLevel realm,
            PlayableOriginCatalog.ResidenceOption residence
    ) {
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
}
