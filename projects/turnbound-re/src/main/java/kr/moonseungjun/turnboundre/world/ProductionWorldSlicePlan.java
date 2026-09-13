package kr.moonseungjun.turnboundre.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Production-facing scale and readability contract for the first authored HUB_01 -> REGION_01 prototype.
 * Stable gameplay locators remain owned by {@link FunctionalWorldSliceLayout}; this class only locks the
 * physical composition rules that were selected by the M6 World Asset Gate.
 */
public final class ProductionWorldSlicePlan {
    public static final int HUB_HALF_WIDTH = 11;
    public static final int HUB_HALF_DEPTH = 9;
    public static final int ROUTE_HALF_WIDTH = 2;
    public static final int MINE_HALF_WIDTH = 7;
    public static final int MINE_HALF_DEPTH = 7;
    public static final int FARM_HALF_WIDTH = 7;
    public static final int FARM_HALF_DEPTH = 7;
    public static final int RIVER_HALF_WIDTH = 8;
    public static final int RIVER_HALF_DEPTH = 7;
    public static final int PATROL_HALF_SIZE = 4;
    public static final int ELITE_HALF_SIZE = 5;

    public static final int HUB_GATE_X = 10;
    public static final int ROUTE_END_X = 78;
    public static final int LANTERN_SPACING = 14;

    public record Footprint(String id, int minX, int maxX, int minZ, int maxZ) {
        public Footprint {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("footprint id required");
            if (minX > maxX || minZ > maxZ) throw new IllegalArgumentException("invalid footprint bounds");
        }

        public boolean overlaps(Footprint other) {
            return minX <= other.maxX && maxX >= other.minX && minZ <= other.maxZ && maxZ >= other.minZ;
        }
    }

    public static final Footprint HUB = new Footprint(
            "hub", -HUB_HALF_WIDTH, HUB_HALF_WIDTH, -HUB_HALF_DEPTH, HUB_HALF_DEPTH);
    public static final Footprint MINE = around(
            "mine", FunctionalWorldSliceLayout.ORE_OUTCROP, MINE_HALF_WIDTH, MINE_HALF_DEPTH);
    public static final Footprint FARM = around(
            "farm", FunctionalWorldSliceLayout.RIVERSIDE_PLOT, FARM_HALF_WIDTH, FARM_HALF_DEPTH);
    public static final Footprint RIVER = around(
            "river", FunctionalWorldSliceLayout.RIVER_POOL, RIVER_HALF_WIDTH, RIVER_HALF_DEPTH);
    public static final Footprint PATROL = aroundSquare(
            "patrol", FunctionalWorldSliceLayout.OVERWORLD_PATROL, PATROL_HALF_SIZE);
    public static final Footprint ELITE = aroundSquare(
            "elite", FunctionalWorldSliceLayout.RIFT_ELITE, ELITE_HALF_SIZE);

    public static final List<Footprint> LANDMARK_FOOTPRINTS = List.of(MINE, FARM, RIVER, PATROL, ELITE);

    private ProductionWorldSlicePlan() {}

    public static List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (FunctionalWorldSliceLayout.HUB_FORGE.offsetX() != 0 || FunctionalWorldSliceLayout.HUB_FORGE.offsetZ() != 0) {
            errors.add("hub forge must remain the HUB_01 visual origin");
        }
        if (HUB_GATE_X <= 0 || HUB_GATE_X > HUB_HALF_WIDTH) {
            errors.add("east hub gate must sit inside the hub footprint");
        }
        if (ROUTE_HALF_WIDTH != 2) {
            errors.add("production main route must remain five blocks wide");
        }
        if (LANTERN_SPACING < 10 || LANTERN_SPACING > 18) {
            errors.add("route landmark cadence must stay readable without becoming cluttered");
        }

        requireBranchGap(errors, MINE, "mine");
        requireBranchGap(errors, FARM, "farm");
        requireBranchGap(errors, RIVER, "river");

        for (int i = 0; i < LANDMARK_FOOTPRINTS.size(); i++) {
            for (int j = i + 1; j < LANDMARK_FOOTPRINTS.size(); j++) {
                Footprint a = LANDMARK_FOOTPRINTS.get(i);
                Footprint b = LANDMARK_FOOTPRINTS.get(j);
                if (a.overlaps(b)) errors.add("production footprints overlap: " + a.id() + "/" + b.id());
            }
        }

        int patrolX = FunctionalWorldSliceLayout.OVERWORLD_PATROL.offsetX();
        int eliteX = FunctionalWorldSliceLayout.RIFT_ELITE.offsetX();
        if (patrolX <= FunctionalWorldSliceLayout.ORE_OUTCROP.offsetX()) {
            errors.add("patrol must read after the first resource branch");
        }
        if (eliteX - patrolX < 16) {
            errors.add("elite landmark needs a distinct approach after the patrol");
        }
        if (ROUTE_END_X < eliteX + ELITE_HALF_SIZE) {
            errors.add("main route must visibly carry through the elite landmark");
        }

        return List.copyOf(errors);
    }

    private static void requireBranchGap(List<String> errors, Footprint footprint, String label) {
        if (footprint.minZ() <= ROUTE_HALF_WIDTH && footprint.maxZ() >= -ROUTE_HALF_WIDTH) {
            errors.add(label + " footprint must not swallow the main route");
        }
        int nearestRoadEdge = footprint.minZ() > 0
                ? footprint.minZ() - ROUTE_HALF_WIDTH
                : -ROUTE_HALF_WIDTH - footprint.maxZ();
        if (nearestRoadEdge < 2) errors.add(label + " needs at least two blocks of visual separation from the main route");
        if (nearestRoadEdge > 8) errors.add(label + " branch is too detached from the main route");
    }

    private static Footprint around(String id, FunctionalWorldSliceLayout.Site site, int halfWidth, int halfDepth) {
        return new Footprint(id,
                site.offsetX() - halfWidth, site.offsetX() + halfWidth,
                site.offsetZ() - halfDepth, site.offsetZ() + halfDepth);
    }

    private static Footprint aroundSquare(String id, FunctionalWorldSliceLayout.Site site, int halfSize) {
        return around(id, site, halfSize, halfSize);
    }
}
