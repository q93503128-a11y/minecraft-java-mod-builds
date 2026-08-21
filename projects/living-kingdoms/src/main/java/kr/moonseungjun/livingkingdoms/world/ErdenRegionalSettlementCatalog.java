package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Terrain-authored second-ring settlements outside Erden's capital fringe.
 *
 * <p>The old exterior supply layer intentionally remains as the capital's near-market belt. These
 * settlements are placed in the actual national geography instead: the southern grain country,
 * northern forest/ranch edge and western mineral hills. The catalog is source-only and performs no
 * world reads or mutations.</p>
 */
public final class ErdenRegionalSettlementCatalog {
    public static final int REVISION = 1;
    public static final int SETTLEMENT_RADIUS = 220;
    public static final int SETTLEMENT_COUNT = 6;
    public static final int BUILDINGS_PER_SETTLEMENT = 10;
    public static final int TOTAL_BUILDINGS = SETTLEMENT_COUNT * BUILDINGS_PER_SETTLEMENT;

    private static final List<BuildingLot> BASE_LOTS = List.of(
            new BuildingLot(-72, -104, "house", "south", "farmstead_west"),
            new BuildingLot(0, -104, "manor", "south", "reeve_hall"),
            new BuildingLot(72, -104, "house", "south", "farmstead_east"),
            new BuildingLot(-108, -40, "house", "east", "artisan_house_west"),
            new BuildingLot(108, -40, "player_castle", "west", "watch_house_east"),
            new BuildingLot(-108, 40, "castle", "east", "storehouse_west"),
            new BuildingLot(108, 40, "house", "west", "craft_house_east"),
            new BuildingLot(-72, 104, "house", "north", "homestead_west"),
            new BuildingLot(0, 104, "tavern", "north", "village_inn"),
            new BuildingLot(72, 104, "house", "north", "homestead_east")
    );

    private static final List<Settlement> SETTLEMENTS = List.of(
            new Settlement("harvest_crossing", -2600, 7200, "grain", BASE_LOTS),
            new Settlement("silvermead", -850, 6400, "river_market", BASE_LOTS),
            new Settlement("sunfield", 2600, 7600, "grain", BASE_LOTS),
            new Settlement("pinewatch", -2500, -7200, "ranch", BASE_LOTS),
            new Settlement("blackstone", -7200, -1800, "colliery", BASE_LOTS),
            new Settlement("ironvale", -9800, 1600, "iron_mine", BASE_LOTS)
    );

    static {
        validate();
    }

    private ErdenRegionalSettlementCatalog() {
    }

    public static List<Settlement> settlements() {
        return SETTLEMENTS;
    }

    public static boolean intersects(ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int maxX = minX + 15;
        int minZ = chunk.getMinBlockZ();
        int maxZ = minZ + 15;
        for (Settlement settlement : SETTLEMENTS) {
            if (settlement.x + SETTLEMENT_RADIUS < minX
                    || settlement.x - SETTLEMENT_RADIUS > maxX
                    || settlement.z + SETTLEMENT_RADIUS < minZ
                    || settlement.z - SETTLEMENT_RADIUS > maxZ) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static Settlement settlementAt(int x, int z) {
        for (Settlement settlement : SETTLEMENTS) {
            if (Math.abs(x - settlement.x) <= SETTLEMENT_RADIUS
                    && Math.abs(z - settlement.z) <= SETTLEMENT_RADIUS) {
                return settlement;
            }
        }
        return null;
    }

    public static double minimumCapitalDistance() {
        return SETTLEMENTS.stream()
                .mapToDouble(settlement -> Math.hypot(settlement.x, settlement.z))
                .min()
                .orElse(0.0);
    }

    private static void validate() {
        if (SETTLEMENTS.size() != SETTLEMENT_COUNT) {
            throw new IllegalStateException("Regional settlement count drifted: " + SETTLEMENTS.size());
        }
        Set<String> ids = new HashSet<>();
        int buildings = 0;
        for (Settlement settlement : SETTLEMENTS) {
            if (!ids.add(settlement.id)) {
                throw new IllegalStateException("Duplicate regional settlement id " + settlement.id);
            }
            if (settlement.buildings.size() != BUILDINGS_PER_SETTLEMENT) {
                throw new IllegalStateException("Regional settlement building count drifted for "
                        + settlement.id + ": " + settlement.buildings.size());
            }
            if (Math.hypot(settlement.x, settlement.z) < 5_000.0) {
                throw new IllegalStateException("Regional settlement fell back into capital fringe: "
                        + settlement.id);
            }
            validateGeography(settlement);
            double surface = AuthoredContinentDensity.surfaceHeight(settlement.x, settlement.z);
            if (surface < 62.0 || surface > 150.0) {
                throw new IllegalStateException("Regional settlement surface is unsafe: "
                        + settlement.id + " y=" + surface);
            }
            buildings += settlement.buildings.size();
        }
        if (buildings != TOTAL_BUILDINGS) {
            throw new IllegalStateException("Regional building total drifted: " + buildings);
        }
    }

    private static void validateGeography(Settlement settlement) {
        switch (settlement.industry) {
            case "grain" -> {
                if (settlement.z < 5_000 || Math.abs(settlement.x) > 3_500) {
                    throw new IllegalStateException("Grain village left southern grain belt: " + settlement.id);
                }
            }
            case "river_market" -> {
                if (settlement.z < 5_000 || Math.abs(settlement.x) > 1_500) {
                    throw new IllegalStateException("River market left central southern river corridor: "
                            + settlement.id);
                }
            }
            case "ranch" -> {
                if (settlement.z > -5_000) {
                    throw new IllegalStateException("Ranch village left northern upland edge: " + settlement.id);
                }
            }
            case "colliery", "iron_mine" -> {
                if (settlement.x > -5_000) {
                    throw new IllegalStateException("Mining village left western mineral belt: " + settlement.id);
                }
            }
            default -> throw new IllegalStateException("Unknown regional industry " + settlement.industry);
        }
    }

    public record Settlement(
            String id,
            int x,
            int z,
            String industry,
            List<BuildingLot> buildings) {
        public Settlement {
            buildings = List.copyOf(buildings);
        }
    }

    public record BuildingLot(
            int dx,
            int dz,
            String style,
            String desiredFront,
            String role) {
    }
}
