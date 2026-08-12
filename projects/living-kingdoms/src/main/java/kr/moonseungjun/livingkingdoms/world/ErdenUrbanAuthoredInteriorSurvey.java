package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures the real imported interior topology before the legacy fixed 7 x 9 room converter touches it.
 *
 * <p>This is deliberately read-only. It gives the authored-interior overhaul a finished-world source
 * of truth: reachable floor area, vertical connectivity, retained stairs, doors and functional
 * fixtures are measured from the actual rotated schematic after streamed construction. The scan uses
 * exactly the same horizontal envelope as the current room converter, so it never requires chunks
 * that the converter itself would not already need and never synchronously loads terrain.</p>
 */
public final class ErdenUrbanAuthoredInteriorSurvey {
    public static final int SURVEY_REVISION = 1;

    private static final int HALF_WIDTH = 3;
    private static final int DEPTH = 9;
    private static final int VERTICAL_SCAN = 16;
    private static final int PROCESS_BUDGET = 4;
    private static final int EXPECTED_BUILDINGS = 233;

    private static MinecraftServer activeServer;
    private static final Map<Long, Profile> PROFILES = new HashMap<>();
    private static boolean completionLogged;

    private ErdenUrbanAuthoredInteriorSurvey() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenUrbanInteriorSavedData converted = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances =
                ExternalUrbanFabricBuilder.entrances();
        int processed = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : entrances) {
            if (processed >= PROCESS_BUDGET) break;
            long key = key(entrance.x(), entrance.z());
            if (PROFILES.containsKey(key)) continue;

            // On an old save the fixed converter has already erased part of the source topology.
            // Do not pretend that altered geometry is an authored baseline.
            if (converted.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) continue;
            Profile profile = survey(level, entrance);
            if (profile == null) continue;
            PROFILES.put(key, profile);
            processed++;

            if (diagnosticMode() && profile.authoredGroundCandidate()) {
                LivingKingdoms.LOGGER.debug(
                        "LK_ERDEN_AUTHORED_INTERIOR_SAMPLE role={} entrance={},{} reachable={} vertical_span={} max_depth={} stairs={} doors={} fixtures={} authored_blocks={} multilevel_candidate={}",
                        entrance.role(), entrance.x(), entrance.z(), profile.reachableCells(),
                        profile.verticalSpan(), profile.maxDepth(), profile.stairs(), profile.doors(),
                        profile.functionalFixtures(), profile.authoredBlocks(),
                        profile.authoredMultilevelCandidate());
            }
        }

        if (!completionLogged && PROFILES.size() == EXPECTED_BUILDINGS) {
            completionLogged = true;
            int ground = 0;
            int multi = 0;
            int stairs = 0;
            int fixtures = 0;
            int authored = 0;
            for (Profile profile : PROFILES.values()) {
                if (profile.authoredGroundCandidate()) ground++;
                if (profile.authoredMultilevelCandidate()) multi++;
                stairs += profile.stairs();
                fixtures += profile.functionalFixtures();
                authored += profile.authoredBlocks();
            }
            LivingKingdoms.LOGGER.info(
                    "Completed Erden pre-conversion authored interior survey buildings={} authored_ground_candidates={} authored_multilevel_candidates={} retained_stairs={} retained_fixtures={} authored_blocks={} read_only=true loaded_only=true revision={}",
                    PROFILES.size(), ground, multi, stairs, fixtures, authored, SURVEY_REVISION);
        }
    }

    public static Profile profile(int x, int z) {
        return PROFILES.get(key(x, z));
    }

    public static int surveyedCount() {
        return PROFILES.size();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        PROFILES.clear();
        completionLogged = false;
    }

    private static Profile survey(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return null;
        Vector inward = inward(entrance);
        Bounds bounds = bounds(entrance, inward);
        if (!chunksReady(level, bounds)) return null;

        int reachable = 0;
        int minFeetY = doorY;
        int maxFeetY = doorY;
        int maxDepth = 0;
        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        if (walkable(level, entrance.x(), doorY, entrance.z())) {
            pending.add(new Node(entrance.x(), doorY, entrance.z()));
            visited.add(nodeKey(entrance.x(), doorY, entrance.z()));
        }
        while (!pending.isEmpty()) {
            Node current = pending.removeFirst();
            reachable++;
            minFeetY = Math.min(minFeetY, current.y);
            maxFeetY = Math.max(maxFeetY, current.y);
            maxDepth = Math.max(maxDepth,
                    depth(entrance.x(), entrance.z(), inward, current.x, current.z));
            for (int[] offset : NEIGHBORS) {
                int x = current.x + offset[0];
                int z = current.z + offset[1];
                if (!insideEnvelope(entrance, inward, x, z)) continue;
                int feetY = findWalkableFeetY(level, x, z, current.y);
                if (feetY == Integer.MIN_VALUE || Math.abs(feetY - current.y) > 1) continue;
                long nodeKey = nodeKey(x, feetY, z);
                if (visited.add(nodeKey)) pending.addLast(new Node(x, feetY, z));
            }
        }

        int stairs = 0;
        int doors = 0;
        int fixtures = 0;
        int authoredBlocks = 0;
        int minimumY = Math.max(level.getMinY(), doorY - 1);
        int maximumY = Math.min(level.getMaxY() - 1, doorY + VERTICAL_SCAN);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                for (int y = minimumY; y <= maximumY; y++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    Block block = state.getBlock();
                    if (block instanceof StairBlock) stairs++;
                    if (block instanceof DoorBlock) doors++;
                    if (functionalFixture(block)) fixtures++;
                    if (authoredStructuralBlock(state)) authoredBlocks++;
                }
            }
        }

        int verticalSpan = maxFeetY - minFeetY;
        boolean groundCandidate = reachable >= 18 && maxDepth >= 4 && authoredBlocks >= 20;
        boolean multilevelCandidate = groundCandidate
                && verticalSpan >= 3
                && stairs >= 2;
        return new Profile(
                entrance.role(), reachable, verticalSpan, maxDepth,
                stairs, doors, fixtures, authoredBlocks,
                groundCandidate, multilevelCandidate);
    }

    private static final int[][] NEIGHBORS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private static Vector inward(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        int deltaX = entrance.roadX() - entrance.x();
        int deltaZ = entrance.roadZ() - entrance.z();
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return new Vector(deltaX >= 0 ? -1 : 1, 0);
        }
        return new Vector(0, deltaZ >= 0 ? -1 : 1);
    }

    private static Bounds bounds(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance, Vector inward) {
        int lateralX = -inward.z;
        int lateralZ = inward.x;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int depth = 0; depth <= DEPTH; depth++) {
            for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
                int x = entrance.x() + inward.x * depth + lateralX * lateral;
                int z = entrance.z() + inward.z * depth + lateralZ * lateral;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    private static boolean insideEnvelope(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            Vector inward,
            int x,
            int z) {
        int dx = x - entrance.x();
        int dz = z - entrance.z();
        int depth = dx * inward.x + dz * inward.z;
        int lateral = dx * (-inward.z) + dz * inward.x;
        return depth >= 0 && depth <= DEPTH && Math.abs(lateral) <= HALF_WIDTH;
    }

    private static int depth(
            int originX, int originZ, Vector inward, int x, int z) {
        return (x - originX) * inward.x + (z - originZ) * inward.z;
    }

    private static boolean chunksReady(ServerLevel level, Bounds bounds) {
        for (int chunkX = Math.floorDiv(bounds.minX, 16);
             chunkX <= Math.floorDiv(bounds.maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(bounds.minZ, 16);
                 chunkZ <= Math.floorDiv(bounds.maxZ, 16); chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)
                        || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        int minimum = Math.max(level.getMinY(), designed - 8);
        int maximum = Math.min(level.getMaxY() - 1, designed + 64);
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

    private static int findWalkableFeetY(
            ServerLevel level, int x, int z, int preferredFeetY) {
        int[] offsets = {0, 1, -1};
        for (int offset : offsets) {
            int feetY = preferredFeetY + offset;
            if (walkable(level, x, feetY, z)) return feetY;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean walkable(ServerLevel level, int x, int feetY, int z) {
        if (feetY <= level.getMinY() || feetY + 1 >= level.getMaxY()) return false;
        BlockPos feet = new BlockPos(x, feetY, z);
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        return bodyPassable(level, feet)
                && bodyPassable(level, head)
                && !level.getBlockState(floor).isAir()
                && level.getFluidState(feet).isEmpty()
                && level.getFluidState(head).isEmpty()
                && level.getFluidState(floor).isEmpty();
    }

    private static boolean bodyPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir()
                || state.getBlock() instanceof DoorBlock
                || state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean functionalFixture(Block block) {
        return block instanceof BedBlock
                || block == Blocks.CHEST
                || block == Blocks.BARREL
                || block == Blocks.CRAFTING_TABLE
                || block == Blocks.FURNACE
                || block == Blocks.SMOKER
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL
                || block == Blocks.LECTERN
                || block == Blocks.BOOKSHELF
                || block == Blocks.STONECUTTER
                || block == Blocks.SMITHING_TABLE
                || block == Blocks.LOOM
                || block == Blocks.CARTOGRAPHY_TABLE
                || block == Blocks.FLETCHING_TABLE
                || block == Blocks.GRINDSTONE
                || block == Blocks.COMPOSTER
                || block == Blocks.CAULDRON
                || block == Blocks.WATER_CAULDRON;
    }

    private static boolean authoredStructuralBlock(BlockState state) {
        if (state.isAir()) return false;
        Block block = state.getBlock();
        if (block instanceof DoorBlock) return true;
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        if (id.equals("minecraft:grass_block")
                || id.equals("minecraft:dirt")
                || id.equals("minecraft:coarse_dirt")
                || id.equals("minecraft:rooted_dirt")
                || id.equals("minecraft:stone")
                || id.equals("minecraft:deepslate")
                || id.equals("minecraft:sand")
                || id.equals("minecraft:gravel")
                || id.equals("minecraft:water")
                || id.equals("minecraft:lava")) {
            return false;
        }
        if (id.endsWith("_leaves") || id.endsWith("_log") || id.endsWith("_wood")
                || id.contains("grass") || id.contains("flower") || id.contains("fern")
                || id.contains("sapling") || id.contains("vine")) {
            return false;
        }
        return true;
    }

    private static boolean diagnosticMode() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || "1".equals(System.getenv("LIVING_KINGDOMS_CI_ENTRY_TRAVERSAL"));
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long nodeKey(int x, int y, int z) {
        long a = ((long) x & 0x1fffffL) << 43;
        long b = ((long) y & 0x3fffffL) << 21;
        long c = (long) z & 0x1fffffL;
        return a ^ b ^ c;
    }

    public record Profile(
            String role,
            int reachableCells,
            int verticalSpan,
            int maxDepth,
            int stairs,
            int doors,
            int functionalFixtures,
            int authoredBlocks,
            boolean authoredGroundCandidate,
            boolean authoredMultilevelCandidate) {
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }

    private record Node(int x, int y, int z) {
    }

    private record Vector(int x, int z) {
    }
}
