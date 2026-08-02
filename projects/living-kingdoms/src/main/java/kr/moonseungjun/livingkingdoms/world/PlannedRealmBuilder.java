package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.level.ServerLevel;

/** Compatibility entry point retained for saved-code stability; alpha.11 delegates to external templates. */
public final class PlannedRealmBuilder {
    private PlannedRealmBuilder() {
    }

    public static IncrementalWorldEditPlan create(ServerLevel level, String homelandId,
                                                   RealmSiteLayoutSavedData.RealmSite site) {
        return ExternalRealmBuilder.create(level, homelandId, site);
    }
}
