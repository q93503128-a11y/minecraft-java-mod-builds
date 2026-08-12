package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.DoorBlock;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CI-only exhaustive road -> entrance -> interior traversal proof for every authored Erden capital
 * entrance. Only the chunks needed by a small moving batch are retained. The audit never calls a
 * synchronous chunk getter and releases every transient ticket after an entry is resolved.
 */
public final class ErdenEntryTraversalAudit {
    private static final int EXPECTED_URBAN = 233;
    private static final int EXPECTED_DISTRICT = 40;
    private static final int EXPECTED_TOTAL = EXPECTED_URBAN + EXPECTED_DISTRICT;
    private static final int ACTIVE_BATCH = 6;
    private static final int BOUNDS_MARGIN = 4;
    private static final int INTERIOR_DEPTH = 6;
    private static final int MAX_ENTRY_AGE_TICKS = 2_400;
    private static final int CHUNK_WAIT_REPORT_INTERVAL = 600;
    private static final int MAX_VERIFY_RETRIES = 120;
    private static final int MAX_BFS_NODES = 35_000;

    private static MinecraftServer activeServer;
    private static boolean initialized;
    private static boolean failed;
    private static boolean passed;
    private static int completedUrban;
    private static int completedDistrict;
    private static final ArrayDeque<Entry> PENDING = new ArrayDeque<>();
    private static final List<ActiveEntry> ACTIVE = new ArrayList<>();
    private static final Map<Long, Integer> TICKET_REFS = new HashMap<>();

    private ErdenEntryTraversalAudit() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!enabled() || failed || passed) return;
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        if (!initialized) initialize(level);
        fillBatch(level);
        processActive(level);
        fillBatch(level);

        if (!failed && !passed && PENDING.isEmpty() && ACTIVE.isEmpty()) {
            if (completedUrban != EXPECTED_URBAN || completedDistrict != EXPECTED_DISTRICT) {
                fail(level, null, "count_mismatch",
                        "urban=" + completedUrban + " district=" + completedDistrict);
                return;
            }
            if (!TICKET_REFS.isEmpty()) {
                fail(level, null, "ticket_leak", "tickets=" + TICKET_REFS.size());
                return;
            }
            passed = true;
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_ENTRY_TRAVERSAL_PASS urban={} district={} total={} road_to_door=true door_to_interior=true actual_doors=true exact_runtime_paths=true tickets_released=true synchronous_get_chunk=false",
                    completedUrban, completedDistrict, completedUrban + completedDistrict);
        }
    }

    private static boolean enabled() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_ENTRY_TRAVERSAL"));
    }

    private static void reset(MinecraftServer server) {
        releaseAllTickets();
        activeServer = server;
        initialized = false;
        failed = false;
        passed = false;
        completedUrban = 0;
        completedDistrict = 0;
        PENDING.clear();
        ACTIVE.clear();
        TICKET_REFS.clear();
    }

    private static void initialize(ServerLevel level) {
        List<Entry> entries = new ArrayList<>(EXPECTED_TOTAL);
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            entries.add(new Entry(
                    EntryKind.URBAN,
                    entrance.role(), entrance.x(), entrance.z(),
                    entrance.roadX(), entrance.roadZ()));
        }
        for (ExternalDistrictBuildingBuilder.BuildingEntrance entrance
                : ExternalDistrictBuildingBuilder.entrances()) {
            entries.add(new Entry(
                    EntryKind.DISTRICT,
                    entrance.role(), entrance.x(), entrance.z(),
                    entrance.roadX(), entrance.roadZ()));
        }

        long urban = entries.stream().filter(entry -> entry.kind == EntryKind.URBAN).count();
        long district = entries.size() - urban;
        if (urban != EXPECTED_URBAN || district != EXPECTED_DISTRICT
                || entries.size() != EXPECTED_TOTAL) {
            fail(level, null, "planned_count_mismatch",
                    "urban=" + urban + " district=" + district + " total=" + entries.size());
            return;
        }

        Set<Long> uniqueEntrances = new HashSet<>();
        for (Entry entry : entries) {
            long unique = (((long) entry.x) << 32) ^ (entry.z & 0xffffffffL);
            if (!uniqueEntrances.add(unique)) {
                fail(level, entry, "duplicate_entrance", "coordinate_collision=true");
                return;
            }
            if (ErdenCapitalStreamingBuilder.roadClassAt(entry.roadX, entry.roadZ)
                    == ErdenCapitalStreamingBuilder.RoadClass.NONE) {
                fail(level, entry, "planned_road_missing", "road_class=NONE");
                return;
            }
            int maximum = entry.kind == EntryKind.URBAN ? 72 : 112;
            int span = Math.max(Math.abs(entry.roadX - entry.x), Math.abs(entry.roadZ - entry.z));
            if (span <= 0 || span > maximum) {
                fail(level, entry, "planned_path_span", "span=" + span + " maximum=" + maximum);
                return;
            }
        }

        entries.sort(Comparator.comparingInt((Entry entry) -> entry.z)
                .thenComparingInt(entry -> entry.x));
        PENDING.addAll(entries);
        initialized = true;
        LivingKingdoms.LOGGER.info(
                "Prepared exhaustive Erden entry traversal audit urban={} district={} total={} active_batch={} runtime_world=true spatial_order=true",
                EXPECTED_URBAN, EXPECTED_DISTRICT, EXPECTED_TOTAL, ACTIVE_BATCH);
    }

    private static void fillBatch(ServerLevel level) {
        while (!failed && ACTIVE.size() < ACTIVE_BATCH && !PENDING.isEmpty()) {
            Entry entry = PENDING.removeFirst();
            Set<Long> chunks = requiredChunks(entry);
            if (chunks.isEmpty()) {
                fail(level, entry, "empty_chunk_plan", "required_chunks=0");
                return;
            }
            for (long packed : chunks) retainChunk(level, packed);
            ACTIVE.add(new ActiveEntry(entry, chunks));
        }
    }

    private static Set<Long> requiredChunks(Entry entry) {
        Vector inward = inward(entry);
        int interiorX = entry.x + inward.x * INTERIOR_DEPTH;
        int interiorZ = entry.z + inward.z * INTERIOR_DEPTH;
        int minX = Math.min(Math.min(entry.x, entry.roadX), interiorX) - BOUNDS_MARGIN;
        int maxX = Math.max(Math.max(entry.x, entry.roadX), interiorX) + BOUNDS_MARGIN;
        int minZ = Math.min(Math.min(entry.z, entry.roadZ), interiorZ) - BOUNDS_MARGIN;
        int maxZ = Math.max(Math.max(entry.z, entry.roadZ), interiorZ) + BOUNDS_MARGIN;
        Set<Long> result = new LinkedHashSet<>();
        for (int chunkX = Math.floorDiv(minX, 16); chunkX <= Math.floorDiv(maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(minZ, 16); chunkZ <= Math.floorDiv(maxZ, 16); chunkZ++) {
                if (!capitalChunk(chunkX, chunkZ)) continue;
                result.add(pack(chunkX, chunkZ));
            }
        }
        return result;
    }

    private static boolean capitalChunk(int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int maxX = minX + 15;
        int minZ = chunkZ * 16;
        int maxZ = minZ + 15;
        return maxX >= ErdenCapitalStreamingBuilder.WEST_WALL_X - 48
                && minX <= ErdenCapitalStreamingBuilder.EAST_WALL_X + 48
                && maxZ >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 48
                && minZ <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 48;
    }

    private static void retainChunk(ServerLevel level, long packed) {
        int refs = TICKET_REFS.getOrDefault(packed, 0);
        if (refs == 0) {
            ChunkPos chunk = new ChunkPos(unpackX(packed), unpackZ(packed));
            level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
            ErdenCapitalStreamingBuilder.requestChunk(level, chunk.x(), chunk.z());
        }
        TICKET_REFS.put(packed, refs + 1);
    }

    private static void releaseChunk(ServerLevel level, long packed) {
        Integer refs = TICKET_REFS.get(packed);
        if (refs == null) return;
        if (refs > 1) {
            TICKET_REFS.put(packed, refs - 1);
            return;
        }
        TICKET_REFS.remove(packed);
        level.getChunkSource().removeTicketWithRadius(
                TicketType.PORTAL, new ChunkPos(unpackX(packed), unpackZ(packed)), 0);
    }

    private static void processActive(ServerLevel level) {
        Iterator<ActiveEntry> iterator = ACTIVE.iterator();
        while (iterator.hasNext() && !failed) {
            ActiveEntry active = iterator.next();
            active.ageTicks++;
            if (active.ageTicks > MAX_ENTRY_AGE_TICKS) {
                fail(level, active.entry, "chunk_or_interior_timeout",
                        "age_ticks=" + active.ageTicks + " chunks=" + active.chunks.size()
                                + " last_failure=" + active.lastFailure
                                + " chunk_states=" + chunkStateSummary(level, active.chunks));
                return;
            }

            String chunkWait = chunkWaitSummary(level, active.chunks);
            if (chunkWait != null) {
                active.lastFailure = "chunks_not_ready";
                if (active.ageTicks % CHUNK_WAIT_REPORT_INTERVAL == 0) {
                    LivingKingdoms.LOGGER.warn(
                            "LK_ERDEN_ENTRY_CHUNK_WAIT kind={} role={} entrance={},{} road={},{} age_ticks={} states={}",
                            active.entry.kind.id, active.entry.role,
                            active.entry.x, active.entry.z,
                            active.entry.roadX, active.entry.roadZ,
                            active.ageTicks, chunkWait);
                }
                continue;
            }
            if (!ErdenEntranceThresholdManager.isComplete(
                    level, active.entry.x, active.entry.z)) {
                active.lastFailure = "threshold_not_normalized";
                continue;
            }
            if (active.entry.kind == EntryKind.URBAN && !urbanInteriorReady(level, active.entry)) {
                active.lastFailure = "urban_interior_not_ready";
                continue;
            }

            Verification verification = verify(level, active.entry);
            if (!verification.ok) {
                active.verifyRetries++;
                active.lastFailure = verification.detail;
                if (active.verifyRetries >= MAX_VERIFY_RETRIES) {
                    fail(level, active.entry, verification.reason,
                            verification.detail + " verify_retries=" + active.verifyRetries);
                    return;
                }
                continue;
            }

            for (long packed : active.chunks) releaseChunk(level, packed);
            iterator.remove();
            if (active.entry.kind == EntryKind.URBAN) completedUrban++;
            else completedDistrict++;
            int complete = completedUrban + completedDistrict;
            if (complete == 1 || complete % 25 == 0 || complete == EXPECTED_TOTAL) {
                LivingKingdoms.LOGGER.info(
                        "Erden entry traversal audit progress {}/{} urban={} district={} active={} retained_chunks={}",
                        complete, EXPECTED_TOTAL, completedUrban, completedDistrict,
                        ACTIVE.size(), TICKET_REFS.size());
            }
        }
    }

    private static String chunkWaitSummary(ServerLevel level, Set<Long> chunks) {
        boolean allReady = true;
        StringBuilder summary = new StringBuilder();
        for (long packed : chunks) {
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            boolean loaded = level.hasChunk(chunkX, chunkZ);
            boolean built = ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ);

            // The streamer and this audit historically used the same keyless PORTAL ticket. When a
            // streamed build finished, its remove call could therefore also remove the physical
            // ticket this audit still logically owned. Reassert only completed-but-unloaded cells;
            // the streamer no longer needs a construction ticket for those cells, so ownership is
            // unambiguous from this point until releaseChunk removes the audit lease.
            if (!loaded && built && TICKET_REFS.containsKey(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }

            if (!loaded || !built) allReady = false;
            if (summary.length() > 0) summary.append(';');
            summary.append(ErdenCapitalStreamingBuilder.diagnosticChunkState(level, chunkX, chunkZ));
        }
        return allReady ? null : summary.toString();
    }

    private static String chunkStateSummary(ServerLevel level, Set<Long> chunks) {
        StringBuilder summary = new StringBuilder();
        for (long packed : chunks) {
            if (summary.length() > 0) summary.append(';');
            summary.append(ErdenCapitalStreamingBuilder.diagnosticChunkState(
                    level, unpackX(packed), unpackZ(packed)));
        }
        return summary.toString();
    }

    private static boolean urbanInteriorReady(ServerLevel level, Entry entry) {
        long key = (((long) entry.x) << 32) ^ (entry.z & 0xffffffffL);
        return level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                .isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION);
    }

    private static Verification verify(ServerLevel level, Entry entry) {
        if (ErdenCapitalStreamingBuilder.roadClassAt(entry.roadX, entry.roadZ)
                == ErdenCapitalStreamingBuilder.RoadClass.NONE) {
            return Verification.fail("runtime_road_missing", "road_class=NONE");
        }

        int doorY = findLowestDoorY(level, entry.x, entry.z);
        if (doorY == Integer.MIN_VALUE) {
            return Verification.fail("actual_door_missing", "door=false");
        }
        WalkNode road = findRoadStart(level, entry);
        if (road == null) {
            return Verification.fail("road_not_walkable", "road_start=false door_y=" + doorY);
        }
        WalkNode interior = findInteriorTarget(level, entry, doorY);
        if (interior == null) {
            return Verification.fail("interior_not_walkable", "interior_target=false door_y=" + doorY);
        }

        Bounds bounds = Bounds.around(entry, interior, BOUNDS_MARGIN);
        SearchResult result = bfs(level, bounds, road, interior);
        if (!result.reached) {
            return Verification.fail(
                    "road_to_interior_blocked",
                    "door_y=" + doorY + " visited=" + result.visited
                            + " road_y=" + road.y + " interior_y=" + interior.y);
        }
        return Verification.pass();
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return Integer.MIN_VALUE;
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        int minimum = Math.max(level.getMinY(), designed - 18);
        int maximum = Math.min(level.getMaxY() - 1, designed + 82);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int lowest = Integer.MAX_VALUE;
        for (int y = minimum; y <= maximum; y++) {
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) {
                lowest = Math.min(lowest, y);
            }
        }
        return lowest == Integer.MAX_VALUE ? Integer.MIN_VALUE : lowest;
    }

    private static WalkNode findRoadStart(ServerLevel level, Entry entry) {
        if (!level.hasChunk(entry.roadX >> 4, entry.roadZ >> 4)) return null;
        int surface = RealmSitePlanner.surfaceY(level, entry.roadX, entry.roadZ);
        for (int delta = 0; delta <= 5; delta++) {
            for (int sign : delta == 0 ? new int[]{1} : new int[]{1, -1}) {
                int feetY = surface + 1 + delta * sign;
                if (walkable(level, entry.roadX, feetY, entry.roadZ)) {
                    return new WalkNode(entry.roadX, feetY, entry.roadZ);
                }
            }
        }
        return null;
    }

    private static WalkNode findInteriorTarget(ServerLevel level, Entry entry, int doorY) {
        Vector inward = inward(entry);
        Vector right = new Vector(-inward.z, inward.x);
        for (int depth = 2; depth <= 8; depth++) {
            for (int lateral = -2; lateral <= 2; lateral++) {
                int x = entry.x + inward.x * depth + right.x * lateral;
                int z = entry.z + inward.z * depth + right.z * lateral;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int delta = 0; delta <= 4; delta++) {
                    for (int sign : delta == 0 ? new int[]{1} : new int[]{1, -1}) {
                        int feetY = doorY + delta * sign;
                        if (walkable(level, x, feetY, z)) return new WalkNode(x, feetY, z);
                    }
                }
            }
        }
        return null;
    }

    private static SearchResult bfs(
            ServerLevel level,
            Bounds bounds,
            WalkNode start,
            WalkNode target) {
        ArrayDeque<WalkNode> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(start);
        visited.add(nodeKey(start));
        int count = 0;
        while (!queue.isEmpty() && count < MAX_BFS_NODES) {
            WalkNode node = queue.removeFirst();
            count++;
            if (node.equals(target)) return new SearchResult(true, count);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int nextX = node.x + direction.getStepX();
                int nextZ = node.z + direction.getStepZ();
                if (nextX < bounds.minX || nextX > bounds.maxX
                        || nextZ < bounds.minZ || nextZ > bounds.maxZ) continue;
                for (int nextY = node.y - 1; nextY <= node.y + 1; nextY++) {
                    if (nextY < bounds.minY || nextY > bounds.maxY) continue;
                    if (!walkable(level, nextX, nextY, nextZ)) continue;
                    WalkNode next = new WalkNode(nextX, nextY, nextZ);
                    if (visited.add(nodeKey(next))) queue.addLast(next);
                }
            }
        }
        return new SearchResult(false, count);
    }

    private static boolean walkable(ServerLevel level, int x, int feetY, int z) {
        if (feetY <= level.getMinY() || feetY + 1 >= level.getMaxY()) return false;
        if (!level.hasChunk(x >> 4, z >> 4)) return false;
        BlockPos feet = new BlockPos(x, feetY, z);
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        if (!bodyPassable(level, feet) || !bodyPassable(level, head)) return false;
        return !level.getBlockState(floor).isAir()
                && level.getFluidState(feet).isEmpty()
                && level.getFluidState(head).isEmpty()
                && level.getFluidState(floor).isEmpty();
    }

    private static boolean bodyPassable(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.isAir()
                || state.getBlock() instanceof DoorBlock
                || state.getCollisionShape(level, pos).isEmpty();
    }

    private static Vector inward(Entry entry) {
        int deltaX = entry.roadX - entry.x;
        int deltaZ = entry.roadZ - entry.z;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return new Vector(deltaX >= 0 ? -1 : 1, 0);
        }
        return new Vector(0, deltaZ >= 0 ? -1 : 1);
    }

    private static void fail(ServerLevel level, Entry entry, String reason, String detail) {
        if (failed) return;
        failed = true;
        LivingKingdoms.LOGGER.error(
                "LK_ERDEN_ENTRY_TRAVERSAL_FAIL kind={} role={} entrance={},{} road={},{} reason={} detail={} completed_urban={} completed_district={} retained_chunks={}",
                entry == null ? "none" : entry.kind.id,
                entry == null ? "none" : entry.role,
                entry == null ? 0 : entry.x,
                entry == null ? 0 : entry.z,
                entry == null ? 0 : entry.roadX,
                entry == null ? 0 : entry.roadZ,
                reason, detail, completedUrban, completedDistrict, TICKET_REFS.size());
        releaseAllTickets(level);
        ACTIVE.clear();
        PENDING.clear();
    }

    private static void releaseAllTickets() {
        if (activeServer == null) return;
        ServerLevel level = activeServer.getLevel(StarterRealmManager.REALM_KEY);
        if (level != null) releaseAllTickets(level);
    }

    private static void releaseAllTickets(ServerLevel level) {
        for (long packed : List.copyOf(TICKET_REFS.keySet())) {
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL,
                    new ChunkPos(unpackX(packed), unpackZ(packed)), 0);
        }
        TICKET_REFS.clear();
    }

    private static long nodeKey(WalkNode node) {
        long x = node.x & 0x3ffffffL;
        long z = node.z & 0x3ffffffL;
        long y = node.y & 0xfffL;
        return (x << 38) | (z << 12) | y;
    }

    private static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private enum EntryKind {
        URBAN("urban"),
        DISTRICT("district");

        private final String id;

        EntryKind(String id) {
            this.id = id;
        }
    }

    private record Entry(EntryKind kind, String role, int x, int z, int roadX, int roadZ) {
    }

    private static final class ActiveEntry {
        private final Entry entry;
        private final Set<Long> chunks;
        private int ageTicks;
        private int verifyRetries;
        private String lastFailure = "waiting";

        private ActiveEntry(Entry entry, Set<Long> chunks) {
            this.entry = entry;
            this.chunks = chunks;
        }
    }

    private record Vector(int x, int z) {
    }

    private record WalkNode(int x, int y, int z) {
    }

    private record SearchResult(boolean reached, int visited) {
    }

    private record Verification(boolean ok, String reason, String detail) {
        static Verification pass() {
            return new Verification(true, "none", "ok");
        }

        static Verification fail(String reason, String detail) {
            return new Verification(false, reason, detail);
        }
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
        static Bounds around(Entry entry, WalkNode interior, int margin) {
            int minX = Math.min(Math.min(entry.x, entry.roadX), interior.x) - margin;
            int maxX = Math.max(Math.max(entry.x, entry.roadX), interior.x) + margin;
            int minZ = Math.min(Math.min(entry.z, entry.roadZ), interior.z) - margin;
            int maxZ = Math.max(Math.max(entry.z, entry.roadZ), interior.z) + margin;
            int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(entry.x, entry.z));
            return new Bounds(minX, maxX, minZ, maxZ, designed - 12, designed + 72);
        }
    }
}
