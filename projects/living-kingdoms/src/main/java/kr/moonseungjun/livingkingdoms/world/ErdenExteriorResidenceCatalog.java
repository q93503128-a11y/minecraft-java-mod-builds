package kr.moonseungjun.livingkingdoms.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One authoritative catalog shared by exterior construction, resident spawning and estate audits.
 * Parcel coordinates preserve the original workforce save contract; the first household at each
 * node occupies attached worker quarters while the remaining households use detached cottages.
 */
public final class ErdenExteriorResidenceCatalog {
    public static final int EXPECTED_RESIDENCES = ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS;
    public static final int EXPECTED_ATTACHED_QUARTERS = ErdenKingdomSupplyCatalog.nodes().size();
    public static final int EXPECTED_DETACHED_COTTAGES =
            EXPECTED_RESIDENCES - EXPECTED_ATTACHED_QUARTERS;

    private static final int[][] PARCEL_OFFSETS = {
            {0, 0}, {28, 0}, {-28, 0}, {0, 28}, {0, -28}
    };

    public record ResidencePlot(
            String householdId,
            String nodeId,
            String nodeRole,
            int parcelX,
            int parcelZ,
            int localIndex,
            boolean attachedQuarters) {
        public long parcelChunk() {
            return pack(parcelX >> 4, parcelZ >> 4);
        }
    }

    private static final List<ResidencePlot> PLOTS;
    private static final Map<String, ResidencePlot> BY_HOUSEHOLD;
    private static final Map<String, List<ResidencePlot>> BY_NODE;
    private static final Map<Long, List<ResidencePlot>> BY_CHUNK;

    static {
        List<ResidencePlot> plots = new ArrayList<>();
        Map<String, ResidencePlot> byHousehold = new LinkedHashMap<>();
        Map<String, List<ResidencePlot>> byNode = new LinkedHashMap<>();
        Map<Long, List<ResidencePlot>> byChunk = new LinkedHashMap<>();
        int globalHousehold = 0;
        int attached = 0;
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            int households = (requiredWorkers(node.role) + 1) / 2;
            if (households > PARCEL_OFFSETS.length) {
                throw new IllegalStateException(
                        "Too many Erden exterior households for node " + node.id);
            }
            for (int localIndex = 0; localIndex < households; localIndex++) {
                int[] offset = PARCEL_OFFSETS[localIndex];
                String householdId = "erden_exterior_household_%03d"
                        .formatted(globalHousehold + 1);
                ResidencePlot plot = new ResidencePlot(
                        householdId, node.id, node.role,
                        node.x + offset[0], node.z + offset[1],
                        localIndex, localIndex == 0);
                if (byHousehold.put(householdId, plot) != null) {
                    throw new IllegalStateException(
                            "Duplicate Erden exterior residence household " + householdId);
                }
                plots.add(plot);
                byNode.computeIfAbsent(node.id, ignored -> new ArrayList<>()).add(plot);
                byChunk.computeIfAbsent(plot.parcelChunk(), ignored -> new ArrayList<>()).add(plot);
                if (plot.attachedQuarters()) attached++;
                globalHousehold++;
            }
        }
        if (plots.size() != EXPECTED_RESIDENCES
                || attached != EXPECTED_ATTACHED_QUARTERS
                || byChunk.size() != EXPECTED_RESIDENCES) {
            throw new IllegalStateException(
                    "Invalid Erden exterior residence catalog plots=" + plots.size()
                            + " attached=" + attached
                            + " unique_chunks=" + byChunk.size());
        }
        PLOTS = List.copyOf(plots);
        BY_HOUSEHOLD = Collections.unmodifiableMap(byHousehold);
        Map<String, List<ResidencePlot>> frozenNodes = new LinkedHashMap<>();
        for (Map.Entry<String, List<ResidencePlot>> entry : byNode.entrySet()) {
            frozenNodes.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        BY_NODE = Collections.unmodifiableMap(frozenNodes);
        Map<Long, List<ResidencePlot>> frozenChunks = new LinkedHashMap<>();
        for (Map.Entry<Long, List<ResidencePlot>> entry : byChunk.entrySet()) {
            frozenChunks.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        BY_CHUNK = Collections.unmodifiableMap(frozenChunks);
    }

    private ErdenExteriorResidenceCatalog() {
    }

    public static List<ResidencePlot> plots() {
        return PLOTS;
    }

    public static ResidencePlot plot(String householdId) {
        return BY_HOUSEHOLD.get(householdId);
    }

    public static List<ResidencePlot> forNode(String nodeId) {
        return BY_NODE.getOrDefault(nodeId, List.of());
    }

    public static List<ResidencePlot> forChunk(int chunkX, int chunkZ) {
        return BY_CHUNK.getOrDefault(pack(chunkX, chunkZ), List.of());
    }

    public static boolean residenceChunk(int chunkX, int chunkZ) {
        return BY_CHUNK.containsKey(pack(chunkX, chunkZ));
    }

    public static int requiredWorkers(String role) {
        return switch (role) {
            case "grain_estate" -> 8;
            case "ranch" -> 7;
            case "colliery" -> 9;
            case "iron_mine" -> 10;
            case "paper_mill" -> 8;
            case "river_wharf" -> 6;
            default -> throw new IllegalStateException(
                    "Unknown Erden exterior residence role " + role);
        };
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
