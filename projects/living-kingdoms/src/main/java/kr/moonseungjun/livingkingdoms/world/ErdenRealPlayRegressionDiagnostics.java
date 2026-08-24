package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** CI-only proof for bugs that require real player-facing entry semantics rather than build counts. */
public final class ErdenRealPlayRegressionDiagnostics {
    private static final boolean ENABLED = "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    private static MinecraftServer activeServer;
    private static int readyWaitTicks;
    private static boolean passed;

    private ErdenRealPlayRegressionDiagnostics() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED || passed) return;
        MinecraftServer server = event.getServer();
        if (activeServer != server) {
            activeServer = server;
            readyWaitTicks = 0;
            passed = false;
        }
        if (server.getTickCount() % 10 != 0) return;

        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null || !RealmSitePlanner.isBuilt(realm, PlayableOriginCatalog.DEFAULT_HOMELAND)) return;
        readyWaitTicks += 10;
        if (readyWaitTicks > 12_000) {
            throw new IllegalStateException(
                    "LK_REAL_PLAY_DIAGNOSTIC_FAIL authored player residence did not become ready within 600 seconds");
        }

        BlockPos target = SafeResidenceLocator.residence(
                realm, PlayableOriginCatalog.DEFAULT_HOMELAND, PlayableOriginCatalog.DEFAULT_RESIDENCE);
        if (target == null) return;
        if (!SafeResidenceLocator.isAuthoritativePlayerResidence(
                realm, PlayableOriginCatalog.DEFAULT_HOMELAND,
                PlayableOriginCatalog.DEFAULT_RESIDENCE, target)) {
            throw new IllegalStateException(
                    "LK_REAL_PLAY_DIAGNOSTIC_FAIL resolved player target is not the authored tenement room: " + target);
        }
        boolean staging = Math.abs((long) target.getX() - SelectionStagingManager.CENTER_X) <= 64L
                && Math.abs((long) target.getZ() - SelectionStagingManager.CENTER_Z) <= 64L;
        if (staging) {
            throw new IllegalStateException(
                    "LK_REAL_PLAY_DIAGNOSTIC_FAIL player residence resolved onto hidden staging: " + target);
        }

        passed = true;
        LivingKingdoms.LOGGER.info(
                "LK_REAL_PLAY_ENTRY_PASS target={},{},{} authored_tenement=true upper_room=true walkable=true staging=false synthetic_floor=false completion_requires_real_placement=true retry_while_staged=true",
                target.getX(), target.getY(), target.getZ());
    }
}
