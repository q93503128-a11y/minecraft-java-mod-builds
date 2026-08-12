package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Makes the authored capital entrance catalogue self-healing after streamed construction.
 *
 * <p>Most entrances retain a real door from their licensed source building. A later terrain/path
 * pass, an older save made before the door-protection fixes, or a source such as the church that
 * contains no retained exterior door can still leave the catalogue pointing at a wall/air column.
 * This manager runs only after the owning capital chunk is completely built. Existing doors are
 * never touched. A genuinely missing door gets a minimal two-block opening at the lowest plausible
 * facade floor, plus a one-cell throat on the door normal so the adaptation connects to both the
 * imported interior and the threshold system. No chunk is synchronously loaded.</p>
 */
public final class ErdenEntranceDoorRepairManager {
    private static final int EXPECTED_ENTRANCES = 273;
    private static final int PROCESS_INTERVAL = 5;
    private static final int PROCESS_BUDGET = 12;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static MinecraftServer activeServer;
    private static volatile List<Entry> cachedEntries;
    private static final Set<Long> COMPLETE = new HashSet<>();
    private static final Set<Long> REPORTED_STALLS = new HashSet<>();
    private static boolean completionLogged;

    private ErdenEntranceDoorRepairManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (level.getGameTime() % PROCESS_INTERVAL != 0L) return;

        int processed = 0;
        for (Entry entry : entries()) {
            if (processed >= PROCESS_BUDGET) break;
            long key = key(entry.x, entry.z);
            if (COMPLETE.contains(key)) continue;
            if (!chunkReady(level, entry.x, entry.z)) continue;

            int existingDoorY = findLowestDoorY(level, entry.x, entry.z);
            if (existingDoorY != Integer.MIN_VALUE) {
                COMPLETE.add(key);
                REPORTED_STALLS.remove(key);
                processed++;
                continue;
            }

            int repairedDoorY = repairMissingDoor(level, entry);
            if (repairedDoorY == Integer.MIN_VALUE) {
                reportStall(entry);
                continue;
            }

            COMPLETE.add(key);
            REPORTED_STALLS.remove(key);
            processed++;
            LivingKingdoms.LOGGER.info(
                    "Repaired missing Erden entrance door kind={} role={} entrance={},{} road={},{} door_y={} source_geometry_preserved=true generated_adaptation=true loaded_only=true",
                    entry.kind, entry.role, entry.x, entry.z,
                    entry.roadX, entry.roadZ, repairedDoorY);
        }

        if (!completionLogged && COMPLETE.size() == EXPECTED_ENTRANCES) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden entrance door integrity pass entrances={} existing_or_repaired=true actual_doors=true loaded_only=true synchronous_get_chunk=false",
                    COMPLETE.size());
        }
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        COMPLETE.clear();
        REPORTED_STALLS.clear();
        completionLogged = false;
    }

    private static List<Entry> entries() {
        List<Entry> result = cachedEntries;
        if (result != null) return result;
        synchronized (ErdenEntranceDoorRepairManager.class) {
            result = cachedEntries;
            if (result == null) {
                List<Entry> built = new ArrayList<>(EXPECTED_ENTRANCES);
                for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                        : ExternalUrbanFabricBuilder.entrances()) {
                    built.add(new Entry(
                            "urban", entrance.role(), entrance.x(), entrance.z(),
                            entrance.roadX(), entrance.roadZ(), true));
                }
                for (ExternalDistrictBuildingBuilder.BuildingEntrance entrance
                        : ExternalDistrictBuildingBuilder.entrances()) {
                    built.add(new Entry(
                            "district", entrance.role(), entrance.x(), entrance.z(),
                            entrance.roadX(), entrance.roadZ(), entrance.residential()));
                }
                if (built.size() != EXPECTED_ENTRANCES) {
                    throw new IllegalStateException(
                            "Erden entrance door integrity count mismatch " + built.size()
                                    + " != " + EXPECTED_ENTRANCES);
                }
                result = List.copyOf(built);
                cachedEntries = result;
                LivingKingdoms.LOGGER.info(
                        "Prepared Erden entrance door integrity pass entrances={} actual_doors=true persistent_save_repair=true",
                        result.size());
            }
            return result;
        }
    }

    private static int repairMissingDoor(ServerLevel level, Entry entry) {
        int feetY = resolveDoorFeetY(level, entry);
        if (feetY == Integer.MIN_VALUE) return Integer.MIN_VALUE;
        Direction facing = facingTowardRoad(entry);

        // A one-cell throat is enough to make a source with no retained exterior doorway usable
        // without replacing its facade. Existing floors remain untouched.
        if (!clearThroat(level, entry.x + facing.getStepX(), feetY,
                entry.z + facing.getStepZ())) return Integer.MIN_VALUE;
        if (!clearThroat(level, entry.x - facing.getStepX(), feetY,
                entry.z - facing.getStepZ())) return Integer.MIN_VALUE;

        Block doorBlock = entry.urbanOrResidential ? Blocks.SPRUCE_DOOR : Blocks.DARK_OAK_DOOR;
        BlockState lower = doorBlock.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = doorBlock.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        set(level, new BlockPos(entry.x, feetY, entry.z), lower);
        set(level, new BlockPos(entry.x, feetY + 1, entry.z), upper);
        return level.getBlockState(new BlockPos(entry.x, feetY, entry.z)).getBlock()
                instanceof DoorBlock ? feetY : Integer.MIN_VALUE;
    }

    /** Select the lowest plausible facade floor near authored terrain, not a roof heightmap. */
    private static int resolveDoorFeetY(ServerLevel level, Entry entry) {
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(entry.x, entry.z));
        int[] offsets = {1, 0, 2, -1, 3, -2, 4, -3, 5, -4, 6, 7, 8, 9, 10, 11, 12};
        for (int offset : offsets) {
            int feetY = designed + offset;
            if (feetY <= level.getMinY() || feetY + 1 >= level.getMaxY()) continue;
            BlockPos feet = new BlockPos(entry.x, feetY, entry.z);
            BlockPos head = feet.above();
            BlockPos floor = feet.below();
            if (level.getBlockEntity(feet) != null || level.getBlockEntity(head) != null) continue;
            BlockState floorState = level.getBlockState(floor);
            if (floorState.isAir() || !level.getFluidState(floor).isEmpty()) continue;
            if (floorState.getCollisionShape(level, floor).isEmpty()) continue;
            if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(head).isEmpty()) continue;
            return feetY;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean clearThroat(ServerLevel level, int x, int feetY, int z) {
        if (!chunkReady(level, x, z)) return false;
        for (int y = feetY; y <= feetY + 1; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockEntity(pos) != null) return false;
            if (level.getBlockState(pos).getBlock() instanceof DoorBlock) continue;
            set(level, pos, Blocks.AIR.defaultBlockState());
        }
        BlockPos floor = new BlockPos(x, feetY - 1, z);
        if (level.getBlockState(floor).isAir() && level.getBlockEntity(floor) == null) {
            set(level, floor, Blocks.STONE_BRICKS.defaultBlockState());
        }
        return true;
    }

    private static Direction facingTowardRoad(Entry entry) {
        int deltaX = entry.roadX - entry.x;
        int deltaZ = entry.roadZ - entry.z;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return deltaX >= 0 ? Direction.EAST : Direction.WEST;
        }
        return deltaZ >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
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

    private static boolean chunkReady(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return level.hasChunk(chunkX, chunkZ)
                && ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) return;
        if (level.getBlockState(pos).equals(state)) return;
        level.setBlock(pos, state, UPDATE_FLAGS);
    }

    private static void reportStall(Entry entry) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_ENTRY_TRAVERSAL"))) return;
        long key = key(entry.x, entry.z);
        if (!REPORTED_STALLS.add(key)) return;
        LivingKingdoms.LOGGER.warn(
                "LK_ERDEN_ENTRANCE_DOOR_REPAIR_STALL kind={} role={} entrance={},{} road={},{} reason=no_safe_facade_floor",
                entry.kind, entry.role, entry.x, entry.z, entry.roadX, entry.roadZ);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private record Entry(
            String kind,
            String role,
            int x,
            int z,
            int roadX,
            int roadZ,
            boolean urbanOrResidential) {
    }
}
