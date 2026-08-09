from pathlib import Path

ROOT = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world")

# 1. Authoritative catalog: preserve logical parcels, add collision-free physical anchors.
catalog = ROOT / "ErdenExteriorResidenceCatalog.java"
catalog.write_text(r'''package kr.moonseungjun.livingkingdoms.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One authoritative catalog shared by exterior construction, resident spawning and estate audits.
 * Parcel coordinates preserve the original workforce/save contract. Physical coordinates are a
 * separate generated-world concern so worker homes never carve through licensed production buildings.
 */
public final class ErdenExteriorResidenceCatalog {
    public static final int EXPECTED_RESIDENCES = ErdenExteriorWorkforceManager.EXPECTED_HOUSEHOLDS;
    public static final int EXPECTED_ATTACHED_QUARTERS = ErdenKingdomSupplyCatalog.nodes().size();
    public static final int EXPECTED_DETACHED_COTTAGES =
            EXPECTED_RESIDENCES - EXPECTED_ATTACHED_QUARTERS;
    public static final int HAMLET_DISTANCE = 96;

    private static final int[][] PARCEL_OFFSETS = {
            {0, 0}, {28, 0}, {-28, 0}, {0, 28}, {0, -28}
    };
    private static final int[] HAMLET_SIDE_OFFSETS = {0, -24, 24, -48, 48};

    public record ResidencePlot(
            String householdId,
            String nodeId,
            String nodeRole,
            int parcelX,
            int parcelZ,
            int physicalX,
            int physicalZ,
            int localIndex,
            boolean attachedQuarters) {
        public long parcelChunk() {
            return pack(parcelX >> 4, parcelZ >> 4);
        }

        public long physicalChunk() {
            return pack(physicalX >> 4, physicalZ >> 4);
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
                int[] parcelOffset = PARCEL_OFFSETS[localIndex];
                Point physical = physicalAnchor(node, localIndex);
                String householdId = "erden_exterior_household_%03d"
                        .formatted(globalHousehold + 1);
                ResidencePlot plot = new ResidencePlot(
                        householdId, node.id, node.role,
                        node.x + parcelOffset[0], node.z + parcelOffset[1],
                        physical.x(), physical.z(),
                        localIndex, localIndex == 0);
                if (byHousehold.put(householdId, plot) != null) {
                    throw new IllegalStateException(
                            "Duplicate Erden exterior residence household " + householdId);
                }
                plots.add(plot);
                byNode.computeIfAbsent(node.id, ignored -> new ArrayList<>()).add(plot);
                byChunk.computeIfAbsent(plot.physicalChunk(), ignored -> new ArrayList<>()).add(plot);
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
                            + " unique_physical_chunks=" + byChunk.size());
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

    private static Point physicalAnchor(
            ErdenKingdomSupplyCatalog.SupplyNode node,
            int localIndex) {
        int outwardX;
        int outwardZ;
        if (Math.abs(node.x) >= Math.abs(node.z)) {
            outwardX = Integer.signum(node.x);
            outwardZ = 0;
        } else {
            outwardX = 0;
            outwardZ = Integer.signum(node.z);
        }
        if (outwardX == 0 && outwardZ == 0) outwardZ = 1;
        int sideX = -outwardZ;
        int sideZ = outwardX;
        int side = HAMLET_SIDE_OFFSETS[localIndex];
        return new Point(
                node.x + outwardX * HAMLET_DISTANCE + sideX * side,
                node.z + outwardZ * HAMLET_DISTANCE + sideZ * side);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private record Point(int x, int z) {
    }
}
''', encoding="utf-8")

# 2. Residence builder uses physical anchors; v2 forces migration.
builder = ROOT / "ErdenExteriorResidenceBuilder.java"
text = builder.read_text(encoding="utf-8")
text = text.replace("public static final int RESIDENCE_REVISION = 1;", "public static final int RESIDENCE_REVISION = 2;")
old = '''    private static Footprint footprint(
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        int chunkX = plot.parcelX() >> 4;
        int chunkZ = plot.parcelZ() >> 4;
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int desiredX = plot.parcelX() - WIDTH / 2;
        int desiredZ = plot.parcelZ() - DEPTH / 2;
        if (plot.attachedQuarters()) {
            int localX = Math.floorMod(plot.parcelX(), 16);
            int localZ = Math.floorMod(plot.parcelZ(), 16);
            desiredX = localX < 8 ? chunkMinX + 6 : chunkMinX + 1;
            desiredZ = localZ < 8 ? chunkMinZ + 6 : chunkMinZ + 1;
        }
        int minX = Math.clamp(desiredX, chunkMinX + 1, chunkMinX + 6);
        int minZ = Math.clamp(desiredZ, chunkMinZ + 1, chunkMinZ + 6);
'''
new = '''    private static Footprint footprint(
            ErdenExteriorResidenceCatalog.ResidencePlot plot) {
        int chunkX = plot.physicalX() >> 4;
        int chunkZ = plot.physicalZ() >> 4;
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int desiredX = plot.physicalX() - WIDTH / 2;
        int desiredZ = plot.physicalZ() - DEPTH / 2;
        int minX = Math.clamp(desiredX, chunkMinX + 1, chunkMinX + 6);
        int minZ = Math.clamp(desiredZ, chunkMinZ + 1, chunkMinZ + 6);
'''
if old not in text:
    raise SystemExit("Residence footprint migration anchor missing")
text = text.replace(old, new, 1)
text = text.replace(
    "Metre-scale physical homes for the 74 exterior household parcels. Every unit has a real door,",
    "Metre-scale physical homes for the 74 exterior households. Logical estate parcels remain stable,"
)
text = text.replace(
    "three beds, storage, a hearth/work surface, lighting and a short access path. Attached quarters\n * deliberately occupy a corner of the node's central production building; detached parcels receive\n * a compact cottage wholly contained in their authoritative parcel chunk.",
    "while physical homes occupy collision-free worker hamlets outside production geometry. Every unit has\n * a real door, three beds, storage, a hearth/work surface, lighting and a short access path."
)
builder.write_text(text, encoding="utf-8")

# 3. Exterior builder: repair old v1 site chunks, build new home chunks, and request both in CI.
exterior = ROOT / "ErdenKingdomExteriorBuilder.java"
text = exterior.read_text(encoding="utf-8")
text = text.replace("public static final int EXTERIOR_REVISION = 1;", "public static final int EXTERIOR_REVISION = 2;")
text = text.replace("public static final int EXPECTED_CI_ANCHORS = 104;", "public static final int EXPECTED_CI_ANCHORS = 178;")
old = '''        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                unique.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
            }
            unique.add(storageAnchorChunk(node));
        }
        if (unique.size() != EXPECTED_CI_ANCHORS) {'''
new = '''        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                unique.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
            }
            unique.add(storageAnchorChunk(node));
        }
        for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                ErdenExteriorResidenceCatalog.plots()) {
            unique.add(plot.physicalChunk());
        }
        if (unique.size() != EXPECTED_CI_ANCHORS) {'''
if old not in text:
    raise SystemExit("prepareCiAnchors migration anchor missing")
text = text.replace(old, new, 1)

old = '''            if (!data.needs(packed, EXTERIOR_REVISION)
                    && !residences.needsChunk(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) continue;'''
new = '''            boolean exteriorNeeded = isCiExteriorAnchor(packed)
                    && data.needs(packed, EXTERIOR_REVISION);
            if (!exteriorNeeded
                    && !residences.needsChunk(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) continue;'''
if text.count(old) < 1:
    raise SystemExit("advance CI need anchor missing")
text = text.replace(old, new, 1)

old = '''        if (!data.needs(packed, EXTERIOR_REVISION)
                && !residences.needsChunk(
                chunkX, chunkZ,
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) {
            release(level, packed);
            return;
        }'''
new = '''        boolean exteriorNeeded = (!isCi() || isCiExteriorAnchor(packed))
                && data.needs(packed, EXTERIOR_REVISION);
        if (!exteriorNeeded
                && !residences.needsChunk(
                chunkX, chunkZ,
                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION)) {
            release(level, packed);
            return;
        }'''
if old not in text:
    raise SystemExit("enqueue need anchor missing")
text = text.replace(old, new, 1)

old = '''            boolean buildExterior = data.needs(packed, EXTERIOR_REVISION);
            boolean buildResidences = residences.needsChunk('''
new = '''            boolean buildExterior = (!isCi() || isCiExteriorAnchor(packed))
                    && data.needs(packed, EXTERIOR_REVISION);
            boolean buildResidences = residences.needsChunk('''
if old not in text:
    raise SystemExit("startNext buildExterior anchor missing")
text = text.replace(old, new, 1)

insert = '''    private static boolean isCiExteriorAnchor(long packed) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                if (pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4) == packed) {
                    return true;
                }
            }
            if (storageAnchorChunk(node) == packed) return true;
        }
        return false;
    }

'''
anchor = "    private static boolean isCi() {\n"
if insert not in text:
    if anchor not in text:
        raise SystemExit("isCi helper insertion anchor missing")
    text = text.replace(anchor, insert + anchor, 1)
exterior.write_text(text, encoding="utf-8")

# 4. Ticket reaper owns the union of exterior and physical-home tickets.
reaper = ROOT / "ErdenExteriorTicketReaper.java"
text = reaper.read_text(encoding="utf-8")
old = '''        Set<Long> sampleAnchors = anchorsFor(sampleNode);
        boolean sampleResidentsReady = sampleResidentsReady(level, sampleNode);'''
new = '''        Set<Long> sampleAnchors = new LinkedHashSet<>(anchorsFor(sampleNode));
        for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                ErdenExteriorResidenceCatalog.forNode(sampleNode.id)) {
            sampleAnchors.add(plot.physicalChunk());
        }
        boolean sampleResidentsReady = sampleResidentsReady(level, sampleNode);'''
if old not in text:
    raise SystemExit("sample anchor migration point missing")
text = text.replace(old, new, 1)

old = '''            boolean residenceReady = !ErdenExteriorResidenceCatalog.residenceChunk(chunkX, chunkZ)
                    || residences.chunkBuilt(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            boolean storageReady = storageReadyForChunk(packed);
            if (RELEASED.contains(packed)
                    || !exterior.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION)
                    || !residenceReady'''
new = '''            boolean residenceReady = !ErdenExteriorResidenceCatalog.residenceChunk(chunkX, chunkZ)
                    || residences.chunkBuilt(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            boolean exteriorReady = !isExteriorAnchor(packed)
                    || exterior.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION);
            boolean storageReady = storageReadyForChunk(packed);
            if (RELEASED.contains(packed)
                    || !exteriorReady
                    || !residenceReady'''
if old not in text:
    raise SystemExit("reaper release readiness anchor missing")
text = text.replace(old, new, 1)

old = '''        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            anchors.addAll(anchorsFor(node));
        }
        if (anchors.size() != ErdenKingdomExteriorBuilder.EXPECTED_CI_ANCHORS) {'''
new = '''        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            anchors.addAll(anchorsFor(node));
        }
        for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                ErdenExteriorResidenceCatalog.plots()) {
            anchors.add(plot.physicalChunk());
        }
        if (anchors.size() != ErdenKingdomExteriorBuilder.EXPECTED_CI_ANCHORS) {'''
if old not in text:
    raise SystemExit("required anchors migration point missing")
text = text.replace(old, new, 1)

insert = '''    private static boolean isExteriorAnchor(long packed) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (anchorsFor(node).contains(packed)) return true;
        }
        return false;
    }

'''
anchor = "    private static boolean isCi() {\n"
if insert not in text:
    if anchor not in text:
        raise SystemExit("reaper helper insertion anchor missing")
    text = text.replace(anchor, insert + anchor, 1)
reaper.write_text(text, encoding="utf-8")
