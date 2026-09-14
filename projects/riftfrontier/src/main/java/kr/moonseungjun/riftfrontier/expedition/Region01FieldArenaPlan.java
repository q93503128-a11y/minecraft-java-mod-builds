package kr.moonseungjun.riftfrontier.expedition;

import java.util.List;

/**
 * Pure Region 01 field-layout contract.
 *
 * This is deliberately independent of Minecraft classes so layout intent can be regression-tested without
 * bootstrapping a game runtime. The current pattern is a field-review combat-space baseline derived from the
 * staggered melee/ranged combat rooms of Minecraft Trial Chambers; it is not final Riftfrontier art direction.
 */
public final class Region01FieldArenaPlan {
    public static final int FIELD_RADIUS = 5;
    public static final int PRESENTATION_MAX_Z = 2;

    private static final List<SpawnCell> HUNTER_SPAWNS = List.of(
        new SpawnCell(-4, -1),
        new SpawnCell(-4, 1),
        new SpawnCell(-4, 2)
    );
    private static final List<SpawnCell> SCOUT_SPAWNS = List.of(
        new SpawnCell(4, 2),
        new SpawnCell(4, 0),
        new SpawnCell(4, -2)
    );
    private static final SpawnCell ELITE_SPAWN = new SpawnCell(0, 2);

    private Region01FieldArenaPlan() {}

    public enum FloorRole {
        COMBAT_FIELD,
        OPEN_LANE
    }

    public record FloorCell(int dx, int dz, FloorRole role) {}

    public record CoverPillar(int dx, int dz, int height) {
        public CoverPillar {
            if (height < 1) throw new IllegalArgumentException("Region 01 cover height must be positive");
        }
    }

    public record SpawnCell(int dx, int dz) {}

    /**
     * Leaves z=3..5 untouched so the existing extraction-relay approach can keep owning its reviewed
     * basalt/calcite/deepslate presentation without two temporary presentation systems fighting each other.
     */
    public static List<FloorCell> floorCells() {
        var cells = new java.util.ArrayList<FloorCell>();
        for (int dx = -FIELD_RADIUS; dx <= FIELD_RADIUS; dx++) {
            for (int dz = -FIELD_RADIUS; dz <= PRESENTATION_MAX_Z; dz++) {
                boolean openLane = Math.abs(dx) <= 1 || dz == -4 || dz == 2;
                cells.add(new FloorCell(dx, dz, openLane ? FloorRole.OPEN_LANE : FloorRole.COMBAT_FIELD));
            }
        }
        return List.copyOf(cells);
    }

    /**
     * Two offset pairs create actual line-of-sight breaks for the current hunter/scout mix while preserving
     * the arrival lane (0,-4), central salvage (0,0), corner salvage nodes and extraction relay (0,5).
     */
    public static List<CoverPillar> coverPillars() {
        return List.of(
            new CoverPillar(-3, -1, 2),
            new CoverPillar(-3, 0, 2),
            new CoverPillar(3, 0, 2),
            new CoverPillar(3, 1, 2)
        );
    }

    /**
     * Reviewed technical staging points keep every patrol role inside the combat-owned z=-5..2 space.
     * Hunters begin on the west flank behind/alongside the left cover pair, scouts on the east flank with
     * staggered sightlines, and the elite keeps the open center-back anchor. This changes no role count or stats.
     */
    public static List<SpawnCell> hunterSpawnCells() { return HUNTER_SPAWNS; }
    public static List<SpawnCell> scoutSpawnCells() { return SCOUT_SPAWNS; }
    public static SpawnCell eliteSpawnCell() { return ELITE_SPAWN; }
}
