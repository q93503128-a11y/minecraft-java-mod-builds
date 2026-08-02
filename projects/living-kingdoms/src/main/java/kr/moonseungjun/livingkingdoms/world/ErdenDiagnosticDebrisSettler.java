package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Removes delayed authored-construction drops only while the explicit headless realm diagnostic is
 * active. Normal worlds never enter this path, so legitimate player loot is not continuously
 * cleaned.
 */
public final class ErdenDiagnosticDebrisSettler {
    private static final boolean ENABLED =
            "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    private static final int CLEAN_INTERVAL_TICKS = 10;

    private ErdenDiagnosticDebrisSettler() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED
                || event.getServer().getTickCount() % CLEAN_INTERVAL_TICKS != 0) return;
        ServerLevel level = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        RealmSiteLayoutSavedData.RealmSite site =
                RealmSitePlanner.site(level, "erden_kingdom");
        if (site == null) return;
        ConstructionDebrisCleaner.cleanConstructionCompletion(
                level, "erden_kingdom", site);
    }
}
