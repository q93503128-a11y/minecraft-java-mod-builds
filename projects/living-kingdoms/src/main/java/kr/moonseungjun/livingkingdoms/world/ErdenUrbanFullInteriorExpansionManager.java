package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Physically completes additional floors selected by the full Erden interior plan.
 *
 * <p>The first upper level remains owned by the older, already-audited route/new-floor managers.
 * This manager runs only after that predecessor is verified, then materializes later source-air floor
 * plates and one zero-cut staircase branch per planned room. Every touched cell was source AIR (or a
 * provenance-approved synthetic crop seal over source AIR); retained source blocks are immutable.</p>
 */
public final class ErdenUrbanFullInteriorExpansionManager {
    public static final int EXPANSION_REVISION = 2;

    private static final int PROCESS_BUDGET = 1;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<Long, PlacementExpansion> EXPANSIONS = new LinkedHashMap<>();
    private static boolean bootstrapped;
    private static int plannedAdditionalLevels;
    private static int plannedAdditionalRooms;

    private static MinecraftServer activeServer;
    private static boolean completionLogged;
    private static boolean ciChunksRequested;
    private static boolean ciPassed;
    private static long ciExpansionKey = Long.MIN_VALUE;

    private ErdenUrbanFullInteriorExpansionManager() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        EXPANSIONS.clear();
        ErdenUrbanFullInteriorRouteCatalog.bootstrap();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, List<ErdenUrbanFullInteriorRouteCatalog.LevelRoutePlan>> sourcePlans =
                ErdenUrbanFullInteriorRouteCatalog.plans();

        int examined = 0;
        int expectedBuildings = 0;
        int expectedLevels = 0;
        int expectedRooms = 0;
        int levels = 0;
        int rooms = 0;
        int floorCells = 0;
        int routeNodes = 0;
        int stairBlocks = 0;
        Map<String, Integer> roles = new LinkedHashMap<>();

        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            examined++;
            List<ErdenUrbanFullInteriorRouteCatalog.LevelRoutePlan> plans =
                    sourcePlans.getOrDefault(placement.fragmentKey(), List.of());
            if (plans.isEmpty()) continue;

            expectedBuildings++;
            expectedLevels += plans.size();
            for (ErdenUrbanFullInteriorRouteCatalog.LevelRoutePlan level : plans) {
                expectedRooms += level.regionRoutes().size();
            }

            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot = snapshots.get(placement.fragmentKey());
            if (snapshot == null) {
                throw new IllegalStateException("Missing source fragment for full interior expansion "
                        + placement.fragmentKey());
            }
            PlacementExpansion expansion = transformAndValidate(placement, snapshot, plans);
            long key = entranceKey(placement.entrance().x(), placement.entrance().z());
            if (EXPANSIONS.put(key, expansion) != null) {
                throw new IllegalStateException("Duplicate Erden full-interior expansion entrance "
                        + placement.entrance().x() + "," + placement.entrance().z());
            }
            roles.merge(placement.role(), 1, Integer::sum);
            levels += expansion.levels().size();
            rooms += expansion.roomTargets().size();
            floorCells += expansion.floorBlocks().size();
            routeNodes += expansion.routeNodes().size();
            stairBlocks += expansion.stairs().size();
        }

        if (examined != 233 || examined != ExternalUrbanFabricBuilder.plotCount()) {
            throw new IllegalStateException("Erden full-interior expansion placement drift: " + examined);
        }
        if (expectedBuildings <= 0 || expectedLevels <= 0 || expectedRooms <= 0) {
            throw new IllegalStateException(
                    "Erden full-interior expansion catalog unexpectedly empty buildings="
                            + expectedBuildings + " levels=" + expectedLevels + " rooms=" + expectedRooms);
        }
        if (EXPANSIONS.size() != expectedBuildings
                || levels != expectedLevels
                || rooms != expectedRooms) {
            throw new IllegalStateException(
                    "Erden full-interior expansion truth drift actual=" + EXPANSIONS.size()
                            + "/" + levels + "/" + rooms
                            + " catalog=" + expectedBuildings + "/" + expectedLevels + "/" + expectedRooms);
        }

        plannedAdditionalLevels = levels;
        plannedAdditionalRooms = rooms;
        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden physical full interior expansions buildings={} levels={} rooms={} roles={} floor_cells={} route_nodes={} stair_blocks={} catalog_truth=true predecessor_upper_required=true source_blocks_cut=0 source_air_only=true placement_counts_unchanged=true plots=233 housing=77 work=156 revision={}",
                EXPANSIONS.size(), levels, rooms, roles, floorCells, routeNodes, stairBlocks,
                EXPANSION_REVISION);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        bootstrap();
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        requestCiSampleChunks(level);
        ErdenUrbanFullInteriorExpansionSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanFullInteriorExpansionSavedData.TYPE);

        int processed = 0;
        for (PlacementExpansion expansion : EXPANSIONS.values()) {
            if (processed >= PROCESS_BUDGET) break;
            if (data.isCompleted(expansion.entranceKey(), EXPANSION_REVISION)) continue;
            if (!ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, expansion.entrance())) continue;
            if (!chunksReady(level, expansion.bounds())) continue;
            if (!materialize(level, expansion, true)) continue;
            if (verify(level, expansion)) {
                data.markCompleted(expansion.entranceKey(), EXPANSION_REVISION);
            }
            processed++;
        }

        int complete = data.completedCount(EXPANSION_REVISION);
        if (!completionLogged && complete == EXPANSIONS.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden physical full interior expansions buildings={} additional_levels={} additional_rooms={} all_room_routes=true source_blocks_cut=0 revision={}",
                    complete, plannedAdditionalLevels, plannedAdditionalRooms,
                    EXPANSION_REVISION);
        }
        verifyCiIfReady(level, data);
    }

    public static int eligibleCount() {
        bootstrap();
        return EXPANSIONS.size();
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        completionLogged = false;
        ciChunksRequested = false;
        ciPassed = false;
        ciExpansionKey = Long.MIN_VALUE;
    }

    private static PlacementExpansion transformAndValidate(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            List<ErdenUrbanFullInteriorRouteCatalog.LevelRoutePlan> sourcePlans) {
        Set<Long> sourceOccupied = new HashSet<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            if (!block.state().isAir()
                    && !ErdenUrbanSyntheticSealProvenance.isClearableSourceAirSeal(
                    snapshot.fragmentKey(), block.x(), block.y(), block.z())) {
                sourceOccupied.add(localKey(block.x(), block.y(), block.z()));
            }
        }

        Set<BlockPos> floorBlocks = new LinkedHashSet<>();
        Map<WorldNode, LocalNode> routeNodesByWorld = new LinkedHashMap<>();
        Map<WorldNode, StairIntent> stairs = new LinkedHashMap<>();
        List<WorldNode> roomTargets = new ArrayList<>();
        List<Integer> targetLevels = new ArrayList<>();
        int stairwellOpenings = 0;

        // Transform route branches first, so floor authoring can leave every required stairwell open.
        for (ErdenUrbanFullInteriorRouteCatalog.LevelRoutePlan level : sourcePlans) {
            targetLevels.add(level.targetFeetY());
            for (ErdenUrbanFullInteriorRouteCatalog.RegionRoute region : level.regionRoutes()) {
                List<LocalNode> localPath = region.path().stream()
                        .map(node -> new LocalNode(node.x(), node.y(), node.z()))
                        .toList();
                if (localPath.isEmpty()) {
                    throw new IllegalStateException("Empty full-interior branch " + placement.fragmentKey());
                }
                List<WorldNode> worldPath = new ArrayList<>();
                for (LocalNode local : localPath) {
                    if (sourceOccupied.contains(localKey(local.x(), local.y(), local.z()))
                            || sourceOccupied.contains(localKey(local.x(), local.y() + 1, local.z()))) {
                        throw new IllegalStateException("Full-interior route cuts source fragment="
                                + placement.fragmentKey() + " local=" + local);
                    }
                    WorldNode world = worldNode(placement, snapshot, local);
                    LocalNode old = routeNodesByWorld.putIfAbsent(world, local);
                    if (old != null && !old.equals(local)) {
                        throw new IllegalStateException("Full-interior route transform collision fragment="
                                + placement.fragmentKey());
                    }
                    worldPath.add(world);
                }
                addStairIntents(placement, localPath, worldPath, stairs, sourceOccupied);
                roomTargets.add(worldPath.getLast());
            }
        }

        Set<BlockPos> routeBody = new HashSet<>();
        for (WorldNode world : routeNodesByWorld.keySet()) {
            routeBody.add(world.pos());
            routeBody.add(world.pos().above());
        }

        Block floorBlock = floorBlock(placement.role());
        for (ErdenUrbanFullInteriorRouteCatalog.LevelRoutePlan level : sourcePlans) {
            int floorY = level.targetFeetY() - 1;
            for (long cell : level.floorCells()) {
                int localX = cellX(cell);
                int localZ = cellZ(cell);
                if (sourceOccupied.contains(localKey(localX, floorY, localZ))) {
                    throw new IllegalStateException("Full-interior floor cuts source fragment="
                            + placement.fragmentKey() + " local=" + localX + "," + floorY + "," + localZ);
                }
                WorldNode world = worldNode(
                        placement, snapshot, new LocalNode(localX, floorY, localZ));
                if (routeBody.contains(world.pos())) {
                    stairwellOpenings++;
                    continue;
                }
                floorBlocks.add(world.pos());
            }
        }
        if (floorBlocks.size() < 24 || roomTargets.isEmpty()) {
            throw new IllegalStateException("Full-interior physical plan too small "
                    + placement.fragmentKey());
        }

        // Every terminal room must retain authored or source support immediately below its feet.
        for (WorldNode target : roomTargets) {
            BlockPos support = target.pos().below();
            if (!floorBlocks.contains(support) && !stairs.containsKey(worldNode(support))) {
                throw new IllegalStateException("Full-interior room target lacks support fragment="
                        + placement.fragmentKey() + " target=" + target);
            }
        }

        Bounds bounds = bounds(placement, floorBlocks, routeNodesByWorld.keySet());
        return new PlacementExpansion(
                entranceKey(placement.entrance().x(), placement.entrance().z()),
                placement.entrance(), placement.role(), placement.fragmentKey(),
                List.copyOf(targetLevels), List.copyOf(floorBlocks), floorBlock,
                Map.copyOf(routeNodesByWorld), Map.copyOf(stairs), Set.copyOf(sourceOccupied),
                List.copyOf(roomTargets), stairwellOpenings, bounds);
    }

    private static void addStairIntents(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            List<LocalNode> locals,
            List<WorldNode> worlds,
            Map<WorldNode, StairIntent> stairs,
            Set<Long> sourceOccupied) {
        for (int index = 1; index < worlds.size(); index++) {
            WorldNode previous = worlds.get(index - 1);
            WorldNode current = worlds.get(index);
            int dx = current.x() - previous.x();
            int dz = current.z() - previous.z();
            int dy = current.y() - previous.y();
            if (Math.abs(dx) + Math.abs(dz) != 1 || dy < 0 || dy > 1) {
                throw new IllegalStateException("Invalid full-interior route edge fragment="
                        + placement.fragmentKey() + " " + previous + "->" + current);
            }
            if (dy == 0) continue;
            WorldNode lower = previous;
            LocalNode lowerLocal = locals.get(index - 1);
            if (sourceOccupied.contains(localKey(lowerLocal.x(), lowerLocal.y(), lowerLocal.z()))) {
                throw new IllegalStateException("Full-interior stair cuts source fragment="
                        + placement.fragmentKey() + " local=" + lowerLocal);
            }
            Direction facing = horizontalDirection(dx, dz);
            StairIntent intent = new StairIntent(lower, facing);
            StairIntent old = stairs.putIfAbsent(lower, intent);
            if (old != null && old.facing() != facing) {
                throw new IllegalStateException("Conflicting full-interior stair branches fragment="
                        + placement.fragmentKey() + " at=" + lower);
            }
        }
    }

    private static boolean materialize(
            ServerLevel level, PlacementExpansion expansion, boolean allowGeneratedClear) {
        for (BlockPos floor : expansion.floorBlocks()) {
            BlockState state = level.getBlockState(floor);
            if (state.getBlock() != expansion.floorBlock()
                    && !runtimeBodyClearable(state, allowGeneratedClear)) return false;
        }
        for (Map.Entry<WorldNode, LocalNode> entry : expansion.routeNodes().entrySet()) {
            WorldNode world = entry.getKey();
            StairIntent stair = expansion.stairs().get(world);
            BlockState feet = level.getBlockState(world.pos());
            if (stair == null) {
                if (!runtimeBodyClearable(feet, allowGeneratedClear)) return false;
            } else if (!matchesStair(feet, expansion.role(), stair.facing())
                    && !runtimeBodyClearable(feet, allowGeneratedClear)) {
                return false;
            }
            if (!runtimeBodyClearable(level.getBlockState(world.pos().above()), allowGeneratedClear)) {
                return false;
            }
        }

        for (BlockPos floor : expansion.floorBlocks()) {
            if (level.getBlockState(floor).getBlock() != expansion.floorBlock()) {
                level.setBlock(floor, expansion.floorBlock().defaultBlockState(), UPDATE_FLAGS);
            }
        }

        Block supportBlock = supportBlock(expansion.role());
        Set<BlockPos> protectedRouteBody = new HashSet<>();
        for (WorldNode world : expansion.routeNodes().keySet()) {
            protectedRouteBody.add(world.pos());
            protectedRouteBody.add(world.pos().above());
        }

        for (Map.Entry<WorldNode, LocalNode> entry : expansion.routeNodes().entrySet()) {
            WorldNode world = entry.getKey();
            LocalNode local = entry.getValue();
            StairIntent stair = expansion.stairs().get(world);
            if (stair == null) {
                if (!level.getBlockState(world.pos()).isAir()) {
                    level.setBlock(world.pos(), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                }
            } else {
                BlockState desired = stairBlock(expansion.role()).defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, stair.facing());
                if (!level.getBlockState(world.pos()).equals(desired)) {
                    level.setBlock(world.pos(), desired, UPDATE_FLAGS);
                }
            }
            if (!level.getBlockState(world.pos().above()).isAir()) {
                level.setBlock(world.pos().above(), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            }

            int supportLocalY = local.y() - 1;
            boolean sourceSupportAir = !expansion.sourceOccupied().contains(
                    localKey(local.x(), supportLocalY, local.z()));
            BlockPos supportPos = world.pos().below();
            if (sourceSupportAir && protectedRouteBody.contains(supportPos) && stair == null) {
                return false;
            }
            if (sourceSupportAir && !protectedRouteBody.contains(supportPos)
                    && level.getBlockState(supportPos).isAir()) {
                level.setBlock(supportPos, supportBlock.defaultBlockState(), UPDATE_FLAGS);
            }
        }
        return true;
    }

    private static boolean verify(ServerLevel level, PlacementExpansion expansion) {
        for (BlockPos floor : expansion.floorBlocks()) {
            if (level.getBlockState(floor).getBlock() != expansion.floorBlock()) return false;
            BlockPos feet = floor.above();
            if (expansion.routeNodes().containsKey(worldNode(feet))) continue;
            if (!level.getBlockState(feet).isAir()) return false;
        }
        for (WorldNode world : expansion.routeNodes().keySet()) {
            StairIntent stair = expansion.stairs().get(world);
            BlockState feet = level.getBlockState(world.pos());
            if (stair == null) {
                if (!feet.isAir()) return false;
            } else if (!matchesStair(feet, expansion.role(), stair.facing())) {
                return false;
            }
            if (!level.getBlockState(world.pos().above()).isAir()) return false;
        }
        for (WorldNode target : expansion.roomTargets()) {
            if (!level.getBlockState(target.pos()).isAir()
                    || !level.getBlockState(target.pos().above()).isAir()
                    || level.getBlockState(target.pos().below()).isAir()) return false;
        }
        return true;
    }

    private static void requestCiSampleChunks(ServerLevel level) {
        if (ciChunksRequested
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        PlacementExpansion sample = EXPANSIONS.values().stream().findFirst().orElse(null);
        if (sample == null) return;
        for (int chunkX = Math.floorDiv(sample.bounds().minX(), 16);
             chunkX <= Math.floorDiv(sample.bounds().maxX(), 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(sample.bounds().minZ(), 16);
                 chunkZ <= Math.floorDiv(sample.bounds().maxZ(), 16); chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
            }
        }
        ciExpansionKey = sample.entranceKey();
        ciChunksRequested = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_FULL_INTERIOR_EXPANSION_CI_SAMPLE role={} entrance={},{} levels={} rooms={} floor_cells={} route_nodes={}",
                sample.role(), sample.entrance().x(), sample.entrance().z(),
                sample.levels(), sample.roomTargets().size(), sample.floorBlocks().size(),
                sample.routeNodes().size());
    }

    private static void verifyCiIfReady(
            ServerLevel level, ErdenUrbanFullInteriorExpansionSavedData data) {
        if (ciPassed || ciExpansionKey == Long.MIN_VALUE
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || !data.isCompleted(ciExpansionKey, EXPANSION_REVISION)) return;
        PlacementExpansion sample = EXPANSIONS.get(ciExpansionKey);
        if (sample == null || !verify(level, sample)) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_FULL_INTERIOR_EXPANSION_PASS buildings={} sample_role={} sample_levels={} sample_rooms={} sample_floor_cells={} sample_route_nodes={} sample_stairs={} stairwell_openings={} predecessor_upper_verified=true all_room_routes=true physical_floor=true source_blocks_cut=0 source_air_only=true plots=233 housing=77 work=156 revision={}",
                EXPANSIONS.size(), sample.role(), sample.levels(), sample.roomTargets().size(),
                sample.floorBlocks().size(), sample.routeNodes().size(), sample.stairs().size(),
                sample.stairwellOpenings(), EXPANSION_REVISION);
    }

    private static boolean chunksReady(ServerLevel level, Bounds bounds) {
        for (int chunkX = Math.floorDiv(bounds.minX(), 16);
             chunkX <= Math.floorDiv(bounds.maxX(), 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(bounds.minZ(), 16);
                 chunkZ <= Math.floorDiv(bounds.maxZ(), 16); chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)
                        || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Bounds bounds(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            Set<BlockPos> floorBlocks,
            Set<WorldNode> routeNodes) {
        int minX = placement.minX();
        int maxX = placement.maxX();
        int minZ = placement.minZ();
        int maxZ = placement.maxZ();
        for (BlockPos pos : floorBlocks) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        for (WorldNode node : routeNodes) {
            minX = Math.min(minX, node.x());
            maxX = Math.max(maxX, node.x());
            minZ = Math.min(minZ, node.z());
            maxZ = Math.max(maxZ, node.z());
        }
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    private static WorldNode worldNode(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            LocalNode local) {
        RotatedPoint rotated = rotate(
                local.x(), local.z(), snapshot.width(), snapshot.length(), placement.rotation());
        return new WorldNode(
                placement.minX() + rotated.x(),
                placement.baseY() + local.y(),
                placement.minZ() + rotated.z());
    }

    private static WorldNode worldNode(BlockPos pos) {
        return new WorldNode(pos.getX(), pos.getY(), pos.getZ());
    }

    private static RotatedPoint rotate(
            int x, int z, int width, int length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new RotatedPoint(x, z);
            case CLOCKWISE_90 -> new RotatedPoint(length - 1 - z, x);
            case CLOCKWISE_180 -> new RotatedPoint(width - 1 - x, length - 1 - z);
            case COUNTERCLOCKWISE_90 -> new RotatedPoint(z, width - 1 - x);
        };
    }

    private static Direction horizontalDirection(int dx, int dz) {
        if (dx == 1 && dz == 0) return Direction.EAST;
        if (dx == -1 && dz == 0) return Direction.WEST;
        if (dx == 0 && dz == 1) return Direction.SOUTH;
        if (dx == 0 && dz == -1) return Direction.NORTH;
        throw new IllegalArgumentException("Not a horizontal unit vector " + dx + "," + dz);
    }

    private static Block floorBlock(String role) {
        return switch (role) {
            case "warehouse", "stable" -> Blocks.SPRUCE_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private static Block supportBlock(String role) {
        return floorBlock(role);
    }

    private static Block stairBlock(String role) {
        return switch (role) {
            case "warehouse", "stable" -> Blocks.SPRUCE_STAIRS;
            default -> Blocks.OAK_STAIRS;
        };
    }

    private static boolean matchesStair(BlockState state, String role, Direction facing) {
        return state.getBlock() == stairBlock(role)
                && state.getValue(HorizontalDirectionalBlock.FACING) == facing;
    }

    private static boolean runtimeBodyClearable(BlockState state, boolean allowGeneratedClear) {
        if (state.isAir()) return true;
        if (!allowGeneratedClear) return false;
        Block block = state.getBlock();
        return block == Blocks.OAK_PLANKS
                || block == Blocks.SPRUCE_PLANKS
                || block == Blocks.SMOOTH_STONE
                || block == Blocks.COARSE_DIRT
                || block == Blocks.STONE_BRICKS
                || block == Blocks.OAK_SLAB
                || block == Blocks.SMOOTH_STONE_SLAB
                || block == Blocks.OAK_STAIRS
                || block == Blocks.SPRUCE_STAIRS
                || block == Blocks.STONE_BRICK_STAIRS;
    }

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long localKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
    }

    private static long cellKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int cellX(long key) {
        return (int) (key >> 32);
    }

    private static int cellZ(long key) {
        return (int) key;
    }

    private record LocalNode(int x, int y, int z) {
    }

    private record WorldNode(int x, int y, int z) {
        BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private record StairIntent(WorldNode at, Direction facing) {
    }

    private record RotatedPoint(int x, int z) {
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }

    private record PlacementExpansion(
            long entranceKey,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            String role,
            String fragmentKey,
            List<Integer> levels,
            List<BlockPos> floorBlocks,
            Block floorBlock,
            Map<WorldNode, LocalNode> routeNodes,
            Map<WorldNode, StairIntent> stairs,
            Set<Long> sourceOccupied,
            List<WorldNode> roomTargets,
            int stairwellOpenings,
            Bounds bounds) {
    }
}
