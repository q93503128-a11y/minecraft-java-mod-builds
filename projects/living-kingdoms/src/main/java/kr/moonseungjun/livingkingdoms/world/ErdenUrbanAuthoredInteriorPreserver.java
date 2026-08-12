package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Preserves imported room topology around the converter's guaranteed navigation lanes.
 *
 * <p>The existing functional converter is intentionally conservative and carves a fixed 7 x 9
 * rectangle so every shop/house remains usable. That also erases source partitions, beams, stairs
 * and trim. Before conversion this manager snapshots architectural blocks inside that same envelope.
 * Once both functional floors are complete it restores only cells outside the guaranteed central
 * aisle and synthetic stair lane. Generated fixtures always win, and the operation rolls itself back
 * if the ground-floor aisle is no longer walkable. No extra chunks are loaded.</p>
 */
public final class ErdenUrbanAuthoredInteriorPreserver {
    public static final int PRESERVE_REVISION = 1;

    private static final int HALF_WIDTH = 3;
    private static final int DEPTH = 9;
    private static final int VERTICAL_SCAN = 16;
    private static final int CAPTURE_BUDGET = 4;
    private static final int RESTORE_BUDGET = 1;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static MinecraftServer activeServer;
    private static final Map<Long, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final Set<Long> RESTORED = new HashSet<>();
    private static boolean completionLogged;

    private ErdenUrbanAuthoredInteriorPreserver() {
    }

    public static void captureBeforeConversion(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenUrbanInteriorSavedData ground = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        int captured = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ExternalUrbanFabricBuilder.entrances()) {
            if (captured >= CAPTURE_BUDGET) break;
            long key = key(entrance.x(), entrance.z());
            if (SNAPSHOTS.containsKey(key) || RESTORED.contains(key)) continue;
            if (ground.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) continue;
            if (!level.hasChunk(entrance.x() >> 4, entrance.z() >> 4)) continue;
            Snapshot snapshot = capture(level, entrance);
            if (snapshot == null) continue;
            SNAPSHOTS.put(key, snapshot);
            captured++;
        }
    }

    public static void restoreAfterConversion(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || SNAPSHOTS.isEmpty()) return;

        ErdenUrbanInteriorSavedData ground = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        ErdenUrbanLifeSavedData upper = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);
        int restoredThisTick = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ExternalUrbanFabricBuilder.entrances()) {
            if (restoredThisTick >= RESTORE_BUDGET) break;
            long key = key(entrance.x(), entrance.z());
            if (RESTORED.contains(key)) continue;
            Snapshot snapshot = SNAPSHOTS.get(key);
            if (snapshot == null) continue;
            if (!ground.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)
                    || !upper.isUpperFloorComplete(key, ErdenUrbanLifeManager.UPPER_FLOOR_REVISION)) {
                continue;
            }
            if (!snapshotChunksReady(level, snapshot)) continue;

            int restored = restore(level, snapshot);
            RESTORED.add(key);
            SNAPSHOTS.remove(key);
            restoredThisTick++;
            if (diagnosticMode()) {
                LivingKingdoms.LOGGER.debug(
                        "Restored Erden authored interior structure role={} entrance={},{} cells={} safe_aisle_preserved=true synthetic_stair_preserved=true fixtures_preserved=true",
                        entrance.role(), entrance.x(), entrance.z(), restored);
            }
        }

        if (!completionLogged && RESTORED.size() == ExternalUrbanFabricBuilder.plotCount()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden authored interior preservation buildings={} source_structure_restored=true safe_aisles=true generated_fixtures=true rollback_guard=true loaded_only=true revision={}",
                    RESTORED.size(), PRESERVE_REVISION);
        }
    }

    public static int restoredCount() {
        return RESTORED.size();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        SNAPSHOTS.clear();
        RESTORED.clear();
        completionLogged = false;
    }

    private static Snapshot capture(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return null;
        Vector inward = inward(entrance);
        Bounds bounds = bounds(entrance, inward);
        if (!chunksReady(level, bounds)) return null;

        List<Cell> cells = new ArrayList<>();
        int minimumY = Math.max(level.getMinY(), doorY - 1);
        int maximumY = Math.min(level.getMaxY() - 1, doorY + VERTICAL_SCAN);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                for (int y = minimumY; y <= maximumY; y++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!authoredStructuralBlock(state)) continue;
                    cells.add(new Cell(x, y, z, state));
                }
            }
        }
        return new Snapshot(
                entrance.x(), entrance.z(), doorY, inward,
                bounds, List.copyOf(cells));
    }

    private static int restore(ServerLevel level, Snapshot snapshot) {
        List<ChangedCell> changed = new ArrayList<>();
        for (Cell cell : snapshot.cells) {
            if (protectedNavigationCell(snapshot, cell.x, cell.y, cell.z)) continue;
            BlockPos pos = new BlockPos(cell.x, cell.y, cell.z);
            BlockState current = level.getBlockState(pos);
            if (!replaceableConversionCell(current)) continue;
            if (current.equals(cell.state)) continue;
            changed.add(new ChangedCell(pos, current));
            level.setBlock(pos, cell.state, UPDATE_FLAGS);
        }

        if (!groundAisleWalkable(level, snapshot)) {
            for (int index = changed.size() - 1; index >= 0; index--) {
                ChangedCell cell = changed.get(index);
                level.setBlock(cell.pos, cell.previous, UPDATE_FLAGS);
            }
            LivingKingdoms.LOGGER.warn(
                    "Rolled back Erden authored interior restoration entrance={},{} attempted_cells={} reason=ground_aisle_blocked",
                    snapshot.entranceX, snapshot.entranceZ, changed.size());
            return 0;
        }
        return changed.size();
    }

    /**
     * Keep the 3m central lane open on both functional floors and reserve the generated stair strip.
     * Source detail is restored around these lanes rather than through them.
     */
    private static boolean protectedNavigationCell(
            Snapshot snapshot, int x, int y, int z) {
        int dx = x - snapshot.entranceX;
        int dz = z - snapshot.entranceZ;
        int depth = dx * snapshot.inward.x + dz * snapshot.inward.z;
        int lateral = dx * (-snapshot.inward.z) + dz * snapshot.inward.x;
        if (depth < 0 || depth > DEPTH) return true;
        int relativeY = y - snapshot.doorY;

        if (Math.abs(lateral) <= 1 && relativeY >= 0 && relativeY <= 8) return true;
        return lateral >= 1 && lateral <= 3
                && depth >= 1 && depth <= 6
                && relativeY >= 0 && relativeY <= 6;
    }

    private static boolean replaceableConversionCell(BlockState current) {
        if (current.isAir()) return true;
        Block block = current.getBlock();
        if (block instanceof DoorBlock) return false;
        return block == Blocks.OAK_PLANKS
                || block == Blocks.SPRUCE_PLANKS
                || block == Blocks.SMOOTH_STONE
                || block == Blocks.COARSE_DIRT
                || block == Blocks.STONE_BRICKS;
    }

    private static boolean groundAisleWalkable(ServerLevel level, Snapshot snapshot) {
        int previousFeetY = snapshot.doorY;
        for (int depth = 0; depth <= DEPTH; depth++) {
            int x = snapshot.entranceX + snapshot.inward.x * depth;
            int z = snapshot.entranceZ + snapshot.inward.z * depth;
            int feetY = findWalkableFeetY(level, x, z, previousFeetY);
            if (feetY == Integer.MIN_VALUE || Math.abs(feetY - previousFeetY) > 1) return false;
            previousFeetY = feetY;
        }
        return true;
    }

    private static boolean snapshotChunksReady(ServerLevel level, Snapshot snapshot) {
        return chunksReady(level, snapshot.bounds);
    }

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
        if (!level.hasChunk(x >> 4, z >> 4)) return Integer.MIN_VALUE;
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
        if (!level.hasChunk(x >> 4, z >> 4)) return Integer.MIN_VALUE;
        int[] offsets = {0, 1, -1};
        for (int offset : offsets) {
            int feetY = preferredFeetY + offset;
            if (walkable(level, x, feetY, z)) return feetY;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean walkable(ServerLevel level, int x, int feetY, int z) {
        if (feetY <= level.getMinY() || feetY + 1 >= level.getMaxY()) return false;
        if (!level.hasChunk(x >> 4, z >> 4)) return false;
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
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir()
                || state.getBlock() instanceof DoorBlock
                || state.getCollisionShape(level, pos).isEmpty();
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
        if (id.endsWith("_leaves")
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

    private record Snapshot(
            int entranceX,
            int entranceZ,
            int doorY,
            Vector inward,
            Bounds bounds,
            List<Cell> cells) {
    }

    private record Cell(int x, int y, int z, BlockState state) {
    }

    private record ChangedCell(BlockPos pos, BlockState previous) {
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }

    private record Vector(int x, int z) {
    }
}
