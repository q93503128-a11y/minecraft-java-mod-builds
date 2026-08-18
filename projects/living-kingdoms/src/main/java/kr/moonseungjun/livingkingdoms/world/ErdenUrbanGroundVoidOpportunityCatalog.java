package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Source-only compatibility view of Erden's authored ground-floor topology.
 *
 * <p>The old implementation ran a second, fixed-door-height flood fill that could disagree with
 * {@link ErdenUrbanPlacedTopologyCatalog}: real stair transitions were ignored and valid imported
 * floors could collapse to two or eleven cells. There is now one topology authority. This catalog
 * only projects the exact fragment profiles into the small compatibility record still consumed by
 * older planning code; it performs no independent geometry search, world read, or mutation.</p>
 */
public final class ErdenUrbanGroundVoidOpportunityCatalog {
    public static final int CATALOG_REVISION = 3;
    public static final int EXPECTED_PLACEMENTS = 233;
    private static final int MIN_USABLE_CELLS = 35;
    private static final int MIN_GROUND_BAND_CELLS = 12;

    private static final Map<String, VoidProfile> FRAGMENTS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanGroundVoidOpportunityCatalog() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        FRAGMENTS.clear();

        Map<String, ErdenUrbanPlacedTopologyCatalog.FragmentProfile> exactFragments =
                ErdenUrbanPlacedTopologyCatalog.fragments();
        int placements = 0;
        int usablePlacements = 0;
        int minimumReachable = Integer.MAX_VALUE;
        int maximumReachable = 0;
        int minimumGroundBand = Integer.MAX_VALUE;
        int maximumGroundBand = 0;
        int minimumVerticalSpan = Integer.MAX_VALUE;
        int maximumVerticalSpan = 0;

        for (ErdenUrbanPlacedTopologyCatalog.PlacementProfile placement
                : ErdenUrbanPlacedTopologyCatalog.placements().values()) {
            placements++;
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile exact =
                    exactFragments.get(placement.fragmentKey());
            if (exact == null) {
                throw new IllegalStateException(
                        "Missing exact authored-ground topology " + placement.fragmentKey());
            }
            VoidProfile profile = FRAGMENTS.computeIfAbsent(
                    placement.fragmentKey(), ignored -> fromExact(exact));
            if (profile.usable()) usablePlacements++;
            minimumReachable = Math.min(minimumReachable, profile.reachableCells());
            maximumReachable = Math.max(maximumReachable, profile.reachableCells());
            minimumGroundBand = Math.min(minimumGroundBand, profile.existingSupportedCells());
            maximumGroundBand = Math.max(maximumGroundBand, profile.existingSupportedCells());
            minimumVerticalSpan = Math.min(minimumVerticalSpan, profile.maxDepth());
            maximumVerticalSpan = Math.max(maximumVerticalSpan, profile.maxDepth());
        }

        if (placements != EXPECTED_PLACEMENTS) {
            throw new IllegalStateException(
                    "Authored-ground Erden placement count drifted: " + placements);
        }
        if (FRAGMENTS.size() != exactFragments.size()) {
            throw new IllegalStateException(
                    "Authored-ground fragment coverage drifted: compatibility=" + FRAGMENTS.size()
                            + " exact=" + exactFragments.size());
        }
        if (minimumReachable == Integer.MAX_VALUE) minimumReachable = 0;
        if (minimumGroundBand == Integer.MAX_VALUE) minimumGroundBand = 0;
        if (minimumVerticalSpan == Integer.MAX_VALUE) minimumVerticalSpan = 0;

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_AUTHORED_GROUND_SURVEY placements={} fragments={} usable_placements={} reachable_min={} reachable_max={} ground_band_min={} ground_band_max={} vertical_span_min={} vertical_span_max={} min_usable_cells={} min_ground_band_cells={} exact_topology_authoritative=true duplicate_bfs=false source_blocks_cut=0 source_only=true world_reads=false mutations=0 revision={}",
                placements, FRAGMENTS.size(), usablePlacements,
                minimumReachable, maximumReachable,
                minimumGroundBand, maximumGroundBand,
                minimumVerticalSpan, maximumVerticalSpan,
                MIN_USABLE_CELLS, MIN_GROUND_BAND_CELLS, CATALOG_REVISION);

        for (Map.Entry<String, VoidProfile> entry : FRAGMENTS.entrySet()) {
            VoidProfile profile = entry.getValue();
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_AUTHORED_GROUND_FRAGMENT fragment={} reachable={} ground_floor_cells={} vertical_span={} usable={} exact_topology_authoritative=true source_blocks_cut=0 source_only=true world_reads=false",
                    entry.getKey(), profile.reachableCells(), profile.existingSupportedCells(),
                    profile.maxDepth(), profile.usable());
        }
    }

    public static VoidProfile profile(String fragmentKey) {
        bootstrap();
        return FRAGMENTS.get(fragmentKey);
    }

    private static VoidProfile fromExact(
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile exact) {
        int groundFloorCells = exact.floorBands().stream()
                .mapToInt(ErdenUrbanPlacedTopologyCatalog.FloorBand::reachableCells)
                .max()
                .orElse(0);
        boolean usable = exact.classification()
                != ErdenUrbanPlacedTopologyCatalog.Classification.FALLBACK
                && exact.reachableCells() >= MIN_USABLE_CELLS
                && groundFloorCells >= MIN_GROUND_BAND_CELLS;

        // Keep the compatibility record shape until the obsolete GroundOnlyFunctionalPlanCatalog
        // is removed. existingSupportedCells now means exact authored ground-band cells;
        // newFloorCells is always zero; maxDepth carries exact vertical span.
        return new VoidProfile(
                exact.reachableCells(), groundFloorCells, 0, exact.verticalSpan(), usable);
    }

    public record VoidProfile(
            int reachableCells,
            int existingSupportedCells,
            int newFloorCells,
            int maxDepth,
            boolean usable) {
        private static final VoidProfile EMPTY = new VoidProfile(0, 0, 0, 0, false);
    }
}
