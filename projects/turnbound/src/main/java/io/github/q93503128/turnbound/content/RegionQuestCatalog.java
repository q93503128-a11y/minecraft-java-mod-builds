package io.github.q93503128.turnbound.content;

import java.util.List;

/**
 * v0.4 Region Quest identity/reward catalog.
 * The source defines 12 IDs and a common reward bundle, but not per-quest objective logic nor the exact chest tier.
 */
public final class RegionQuestCatalog {
    public record RegionQuest(String id, String region, int crystal, int gold, String chestRule, boolean objectiveSpecified) {}

    private static final List<RegionQuest> ALL = List.of(
            quest("RQ_M01_broken_cart", "MEADOW"),
            quest("RQ_M02_missing_scout", "MEADOW"),
            quest("RQ_M03_fuse_nest", "MEADOW"),
            quest("RQ_G01_lost_lantern", "GLOAMWOOD"),
            quest("RQ_G02_moss_path", "GLOAMWOOD"),
            quest("RQ_G03_root_sample", "GLOAMWOOD"),
            quest("RQ_A01_pressure_valve", "AQUEDUCT"),
            quest("RQ_A02_rusted_message", "AQUEDUCT"),
            quest("RQ_A03_flood_cache", "AQUEDUCT"),
            quest("RQ_Q01_worker_tags", "QUARRY"),
            quest("RQ_Q02_cooling_route", "QUARRY"),
            quest("RQ_Q03_old_tool", "QUARRY")
    );

    private RegionQuestCatalog() {}

    public static List<RegionQuest> all() { return ALL; }
    public static RegionQuest get(String id) {
        return ALL.stream().filter(q -> q.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Region Quest " + id));
    }

    private static RegionQuest quest(String id, String region) {
        return new RegionQuest(id, region, 200, 2_000, "REGION_T1_T2_OR_T3_CHEST_TIER_UNSPECIFIED", false);
    }
}
