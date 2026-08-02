package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.level.ServerLevel;

/** Alpha.11 validates imported settlements as a whole and never rebuilds obsolete procedural lots. */
public final class RealmLayoutIntegrity {
    private RealmLayoutIntegrity() {
    }

    public static void apply(ServerLevel level, String homelandId,
                             RealmSiteLayoutSavedData.RealmSite site) {
        // Stable compatibility hook only. No legacy block rewriting is allowed here.
    }
}
