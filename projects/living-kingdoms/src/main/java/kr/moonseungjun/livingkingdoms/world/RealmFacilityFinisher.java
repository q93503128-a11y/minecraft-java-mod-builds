package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Final non-destructive settlement pass.
 *
 * <p>Legacy alpha builds rebuilt the old Erden coordinates after the new plan finished, which
 * overwrote roads, recut an obsolete canal and restored the same broken structures. Revision 8
 * already emits its final facilities in one ordered plan, so finishing now only clears stored
 * construction debris.</p>
 */
public final class RealmFacilityFinisher {
    private static final Set<MinecraftServer> FINISHED_SERVERS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private RealmFacilityFinisher() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        synchronized (FINISHED_SERVERS) {
            if (FINISHED_SERVERS.contains(server)) return;
        }
        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return;
        boolean anyBuilt = false;
        for (String homeland : new String[]{"erden_kingdom", "silvana_forest", "kardum_league"}) {
            RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, homeland);
            if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) continue;
            ConstructionDebrisCleaner.schedule(realm, homeland, site);
            anyBuilt = true;
        }
        if (anyBuilt) {
            synchronized (FINISHED_SERVERS) {
                FINISHED_SERVERS.add(server);
            }
        }
    }

    public static void ensureCriticalFacilities(ServerLevel realm,
                                                RealmSiteLayoutSavedData.RealmSite site) {
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) return;
        ConstructionDebrisCleaner.schedule(realm, "erden_kingdom", site);
    }
}
