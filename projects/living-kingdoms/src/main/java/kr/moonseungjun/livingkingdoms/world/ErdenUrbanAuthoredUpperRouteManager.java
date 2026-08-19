package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Materializes the source-air routes that lead from Erden's retained urban entrances to real,
 * already-supported upper rooms in the imported structures.
 *
 * <p>The source planner proves that every route body cell is air in the immutable raw source. This
 * manager rotates those local coordinates with the exact placement rotation and authors only stair
 * or support blocks into cells that were source air. It is intentionally run after the authored
 * interior preservation pass: retained source floors, walls and fixtures have already been restored,
 * while only a tightly bounded palette of generated conversion blocks may be reconciled inside cells
 * that the immutable source proves were air. Crop-face seal blocks are treated as generated rather
 * than authored only when {@link ErdenUrbanSyntheticSealProvenance} independently proves the raw
 * schematic cell was AIR and the runtime block is in the approved clear palette. No authored wall,
 * roof, floor or fixture is cut.</p>
 */
public final class ErdenUrbanAuthoredUpperRouteManager {
    public static final int ROUTE_REVISION = 2;

    private static final int PROCESS_BUDGET = 1;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<Long, PlacementRoute> ROUTES = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private static MinecraftServer activeServer;
    private static boolean completionLogged;
    private static boolean ciChunksRequested;
    private static boolean ciPassed;
    private static long ciRouteKey = Long.MIN_VALUE;
    private static long lastCiChunkRefreshTick = Long.MIN_VALUE;

    private ErdenUrbanAuthoredUpperRouteManager() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        ROUTES.clear();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, ErdenUrbanSourceAirRoutePlanner.RoutePlan> sourceRoutes =
                ErdenUrbanSourceAirRoutePlanner.plans();
        Map<String, Integer> roles = new LinkedHashMap<>();
        int examined = 0;

        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            examined++;
            ErdenUrbanSourceAirRoutePlanner.RoutePlan route =
                    sourceRoutes.get(placement.fragmentKey());
            if (route == null
                    || route.classification()
                    != ErdenUrbanSourceAirRoutePlanner.RouteClassification.ZERO_CUT_ROUTE
                    || route.targetMode()
                    != ErdenUrbanUpperRoomOpportunityCatalog.FloorMode.EXISTING_SOURCE_FLOOR
                    || route.path().isEmpty()) {
                continue;
            }
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot =
                    snapshots.get(placement.fragmentKey());
            if (snapshot == null) {
                throw new IllegalStateException("Missing Erden source fragment for authored route "
                        + placement.fragmentKey());
            }

            PlacementRoute placed = transformAndValidate(placement, snapshot, route);
            long key = entranceKey(placement.entrance().x(), placement.entrance().z());
            if (ROUTES.put(key, placed) != null) {
                throw new IllegalStateException("Duplicate Erden authored upper-route entrance "
                        + placement.entrance().x() + "," + placement.entrance().z());
            }
            roles.merge(placement.role(), 1, Integer::sum);
        }

        if (examined != ExternalUrbanFabricBuilder.plotCount() || examined != 233) {
            throw new IllegalStateException("Erden authored-route placement count drifted: " + examined);
        }
        if (ROUTES.isEmpty()) {
            throw new IllegalStateException(
                    "Erden source audit found no zero-cut route to an existing upper room");
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden authored upper-route materialization eligible={} examined={} roles={} source_blocks_cut=0 source_air_only=true world_transform_verified=true synthetic_seal_provenance=true placement_counts_unchanged=true revision={}",
                ROUTES.size(), examined, roles, ROUTE_REVISION);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        bootstrap();
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        requestCiSampleChunks(level);
        ErdenUrbanAuthoredUpperRouteSavedData routes = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanAuthoredUpperRouteSavedData.TYPE);
        ErdenUrbanInteriorSavedData ground = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);

        int processed = 0;
        for (PlacementRoute route : ROUTES.values()) {
            if (processed >= PROCESS_BUDGET) break;
            long key = route.entranceKey();
            if (routes.isCompleted(key, ROUTE_REVISION)) continue;
            if (!ground.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) continue;
            if (!chunksReady(level, route.bounds())) continue;

            boolean prepared = routes.isPrepared(key, ROUTE_REVISION);
            if (!prepared) {
                // The preservation pass has already restored every retained source block. Clearing is
                // therefore restricted to the known generated conversion palette, and only at cells
                // that immutable raw source topology proved were air.
                if (!prepare(level, route, true)) continue;
                routes.markPrepared(key, ROUTE_REVISION);
                prepared = true;
                LivingKingdoms.LOGGER.debug(
                        "Prepared zero-cut Erden authored upper route role={} entrance={},{} nodes={} stairs={} source_blocks_cut=0",
                        route.role(), route.entranceX(), route.entranceZ(),
                        route.nodes().size(), route.stairs().size());
            }

            if (prepared && verify(level, route)) {
                routes.markCompleted(key, ROUTE_REVISION);
            }
            processed++;
        }

        int complete = routes.completedCount(ROUTE_REVISION);
        if (!completionLogged && complete == ROUTES.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden authored upper routes buildings={} door_to_ground=true stairs=true existing_upper_rooms=true source_blocks_cut=0 synthetic_route_cells_reconciled=true revision={}",
                    complete, ROUTE_REVISION);
        }
        verifyCiIfReady(level, routes);
    }

    public static int eligibleCount() {
        bootstrap();
        return ROUTES.size() + ErdenUrbanAuthoredNewFloorManager.eligibleCount();
    }

    public static boolean isEligible(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        long key = entranceKey(entrance.x(), entrance.z());
        return ROUTES.containsKey(key)
                || ErdenUrbanAuthoredNewFloorManager.isEligible(entrance);
    }

    public static boolean isPrepared(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        long key = entranceKey(entrance.x(), entrance.z());
        if (!ROUTES.containsKey(key)) {
            return ErdenUrbanAuthoredNewFloorManager.isPrepared(level, entrance);
        }
        return level.getDataStorage().computeIfAbsent(ErdenUrbanAuthoredUpperRouteSavedData.TYPE)
                .isPrepared(key, ROUTE_REVISION);
    }

    public static boolean isCompleted(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        long key = entranceKey(entrance.x(), entrance.z());
        if (!ROUTES.containsKey(key)) {
            return ErdenUrbanAuthoredNewFloorManager.isCompleted(level, entrance);
        }
        return level.getDataStorage().computeIfAbsent(ErdenUrbanAuthoredUpperRouteSavedData.TYPE)
                .isCompleted(key, ROUTE_REVISION);
    }

    /** Returns the verified player-feet position in the real source upper room, or {@code null}. */
    public static BlockPos verifiedUpperTarget(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        long key = entranceKey(entrance.x(), entrance.z());
        PlacementRoute route = ROUTES.get(key);
        if (route == null) {
            return ErdenUrbanAuthoredNewFloorManager.verifiedUpperTarget(level, entrance);
        }
        ErdenUrbanAuthoredUpperRouteSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanAuthoredUpperRouteSavedData.TYPE);
        if (!data.isCompleted(key, ROUTE_REVISION)) return null;
        return route.target().pos();
    }

    public static void verifyOrThrow(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        PlacementRoute route = ROUTES.get(entranceKey(entrance.x(), entrance.z()));
        if (route == null) {
            if (ErdenUrbanAuthoredNewFloorManager.isEligible(entrance)) {
                ErdenUrbanAuthoredNewFloorManager.verifyOrThrow(level, entrance);
                return;
            }
            throw new IllegalStateException("Entrance has no authored upper route: "
                    + entrance.x() + "," + entrance.z());
        }
        if (!verify(level, route)) {
            throw new IllegalStateException("Erden authored upper route verification failed role="
                    + entrance.role() + " entrance=" + entrance.x() + "," + entrance.z());
        }
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        completionLogged = false;
        ciChunksRequested = false;
        ciPassed = false;
        ciRouteKey = Long.MIN_VALUE;
        lastCiChunkRefreshTick = Long.MIN_VALUE;
    }

    private static PlacementRoute transformAndValidate(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanSourceAirRoutePlanner.RoutePlan sourceRoute) {
        Set<Long> sourceOccupied = new HashSet<>();
        int doorLocalY = Integer.MAX_VALUE;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            if (!block.state().isAir()
                    && !ErdenUrbanSyntheticSealProvenance.isClearableSourceAirSeal(
                    snapshot.fragmentKey(), block.x(), block.y(), block.z())) {
                sourceOccupied.add(localKey(block.x(), block.y(), block.z()));
            }
            if (block.x() == snapshot.entranceX()
                    && block.z() == snapshot.entranceZ()
                    && block.state().getBlock() instanceof DoorBlock) {
                doorLocalY = Math.min(doorLocalY, block.y());
            }
        }
        if (doorLocalY == Integer.MAX_VALUE) {
            throw new IllegalStateException("Missing retained source door for authored route "
                    + placement.fragmentKey());
        }

        RotatedPoint entrance = rotate(
                snapshot.entranceX(), snapshot.entranceZ(),
                snapshot.width(), snapshot.length(), placement.rotation());
        int expectedEntranceX = placement.minX() + entrance.x();
        int expectedEntranceZ = placement.minZ() + entrance.z();
        if (expectedEntranceX != placement.entrance().x()
                || expectedEntranceZ != placement.entrance().z()) {
            throw new IllegalStateException("Erden authored-route transform drifted for "
                    + placement.fragmentKey() + " expected_entrance="
                    + expectedEntranceX + "," + expectedEntranceZ + " actual="
                    + placement.entrance().x() + "," + placement.entrance().z());
        }

        List<RouteNode> nodes = new ArrayList<>();
        for (ErdenUrbanSourceAirRoutePlanner.Node local : sourceRoute.path()) {
            if (sourceOccupied.contains(localKey(local.x(), local.y(), local.z()))
                    || sourceOccupied.contains(localKey(local.x(), local.y() + 1, local.z()))) {
                throw new IllegalStateException("Source-air route intersects authored body block fragment="
                        + placement.fragmentKey() + " local=" + local);
            }
            nodes.add(new RouteNode(local, worldNode(placement, snapshot, local)));
        }
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Empty authored upper route " + placement.fragmentKey());
        }

        RouteNode last = nodes.getLast();
        if (last.local().y() != sourceRoute.targetFeetY()
                || !sourceOccupied.contains(localKey(
                last.local().x(), last.local().y() - 1, last.local().z()))) {
            throw new IllegalStateException("Erden authored route does not terminate on a retained source floor "
                    + placement.fragmentKey());
        }

        Map<WorldNode, StairIntent> stairs = new LinkedHashMap<>();
        for (int index = 1; index < nodes.size(); index++) {
            RouteNode previous = nodes.get(index - 1);
            RouteNode current = nodes.get(index);
            int dx = current.world().x() - previous.world().x();
            int dz = current.world().z() - previous.world().z();
            int dy = current.world().y() - previous.world().y();
            if (Math.abs(dx) + Math.abs(dz) != 1 || Math.abs(dy) > 1) {
                throw new IllegalStateException("Invalid transformed Erden route edge fragment="
                        + placement.fragmentKey() + " edge=" + previous.world() + "->" + current.world());
            }
            if (dy == 0) continue;

            RouteNode lower = dy > 0 ? previous : current;
            RouteNode higher = dy > 0 ? current : previous;
            if (sourceOccupied.contains(localKey(
                    lower.local().x(), lower.local().y(), lower.local().z()))) {
                throw new IllegalStateException("Erden stair would overwrite a source block fragment="
                        + placement.fragmentKey() + " local=" + lower.local());
            }
            Direction facing = horizontalDirection(
                    higher.world().x() - lower.world().x(),
                    higher.world().z() - lower.world().z());
            StairIntent intent = new StairIntent(lower.world(), facing);
            StairIntent old = stairs.putIfAbsent(lower.world(), intent);
            if (old != null && old.facing() != facing) {
                throw new IllegalStateException("Conflicting Erden stair intents fragment="
                        + placement.fragmentKey() + " at=" + lower.world());
            }
        }
        if (stairs.isEmpty()) {
            throw new IllegalStateException("Zero-cut Erden upper route has no vertical stair edge fragment="
                    + placement.fragmentKey());
        }

        for (RouteNode node : nodes) {
            WorldNode head = new WorldNode(
                    node.world().x(), node.world().y() + 1, node.world().z());
            if (stairs.containsKey(head)) {
                throw new IllegalStateException("Erden route stair collides with another route head fragment="
                        + placement.fragmentKey() + " at=" + head);
            }
        }

        Bounds bounds = bounds(nodes, placement);
        return new PlacementRoute(
                entranceKey(placement.entrance().x(), placement.entrance().z()),
                placement.role(), placement.fragmentKey(),
                placement.entrance().x(), placement.entrance().z(),
                placement.baseY() + doorLocalY,
                placement.minX(), placement.maxX(), placement.minZ(), placement.maxZ(),
                List.copyOf(nodes), Map.copyOf(stairs), Set.copyOf(sourceOccupied),
                last.world(), bounds);
    }

    private static boolean prepare(
            ServerLevel level, PlacementRoute route, boolean allowFreshConversionClear) {
        if (!(level.getBlockState(new BlockPos(
                route.entranceX(), route.expectedDoorY(), route.entranceZ())).getBlock()
                instanceof DoorBlock)) {
            return false;
        }

        // Preflight the entire body first: never leave a half-built staircase after discovering a
        // player block or an unknown runtime obstruction halfway through the path.
        for (RouteNode node : route.nodes()) {
            StairIntent stair = route.stairs().get(node.world());
            BlockState feet = level.getBlockState(node.world().pos());
            if (stair == null) {
                if (!runtimeBodyClearable(feet, allowFreshConversionClear)) return false;
            } else if (!matchesStair(feet, route.role(), stair.facing())
                    && !runtimeBodyClearable(feet, allowFreshConversionClear)) {
                return false;
            }
            BlockState head = level.getBlockState(new BlockPos(
                    node.world().x(), node.world().y() + 1, node.world().z()));
            if (!runtimeBodyClearable(head, allowFreshConversionClear)) return false;
        }

        Block support = supportBlock(route.role());
        for (RouteNode node : route.nodes()) {
            StairIntent stair = route.stairs().get(node.world());
            BlockPos feetPos = node.world().pos();
            if (stair == null) {
                BlockState feet = level.getBlockState(feetPos);
                if (!feet.isAir()) level.setBlock(feetPos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            } else {
                BlockState desired = stairBlock(route.role()).defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, stair.facing());
                level.setBlock(feetPos, desired, UPDATE_FLAGS);
            }

            BlockPos headPos = new BlockPos(
                    node.world().x(), node.world().y() + 1, node.world().z());
            BlockState head = level.getBlockState(headPos);
            if (!head.isAir()) level.setBlock(headPos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);

            int floorLocalY = node.local().y() - 1;
            boolean sourceFloorAir = !route.sourceOccupied().contains(localKey(
                    node.local().x(), floorLocalY, node.local().z()));
            BlockPos floorPos = new BlockPos(
                    node.world().x(), node.world().y() - 1, node.world().z());
            // A stair block is itself the walkable surface. Adding a synthetic full support
            // beneath every stair can occupy the head cell of a lower switchback segment when
            // the source-air route passes under itself two metres below. Keep stair undersides
            // open; only flat air-foot nodes need an authored support floor.
            if (stair == null && sourceFloorAir && level.getBlockState(floorPos).isAir()) {
                level.setBlock(floorPos, support.defaultBlockState(), UPDATE_FLAGS);
            }
        }
        return true;
    }

    private static boolean verify(ServerLevel level, PlacementRoute route) {
        BlockState door = level.getBlockState(new BlockPos(
                route.entranceX(), route.expectedDoorY(), route.entranceZ()));
        if (!(door.getBlock() instanceof DoorBlock)) return false;

        for (RouteNode node : route.nodes()) {
            BlockState feet = level.getBlockState(node.world().pos());
            StairIntent stair = route.stairs().get(node.world());
            if (stair == null) {
                if (!feet.isAir()) return false;
            } else if (!matchesStair(feet, route.role(), stair.facing())) {
                return false;
            }
            BlockState head = level.getBlockState(new BlockPos(
                    node.world().x(), node.world().y() + 1, node.world().z()));
            if (!head.isAir()) return false;

            BlockState floor = level.getBlockState(new BlockPos(
                    node.world().x(), node.world().y() - 1, node.world().z()));
            // Flat air-foot nodes require a real floor. A stair node does not: the stair in the
            // feet cell is already its collision/walking surface, and forcing a block below it
            // can destroy the headroom of a lower switchback segment. Fluids below either form
            // are still rejected.
            if (!floor.getFluidState().isEmpty()) return false;
            if (stair == null && floor.isAir()) return false;
        }

        RouteNode endpoint = route.nodes().getLast();
        if (!endpoint.world().equals(route.target())) return false;
        if (!groundCanReachRouteBase(level, route)) return false;
        return true;
    }

    private static boolean groundCanReachRouteBase(ServerLevel level, PlacementRoute route) {
        WorldNode routeBase = route.nodes().getFirst().world();
        Node start = new Node(route.entranceX(), route.expectedDoorY(), route.entranceZ());
        if (!walkableGround(level, start.x(), start.y(), start.z())) return false;

        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        pending.add(start);
        visited.add(nodeKey(start.x(), start.y(), start.z()));
        while (!pending.isEmpty() && visited.size() <= 4_096) {
            Node current = pending.removeFirst();
            if (reachesRouteBase(current, routeBase, route.stairs().containsKey(routeBase))) {
                return true;
            }
            for (int[] direction : DIRECTIONS) {
                int x = current.x() + direction[0];
                int z = current.z() + direction[1];
                if (x < route.bounds().minX() || x > route.bounds().maxX()
                        || z < route.bounds().minZ() || z > route.bounds().maxZ()) continue;
                for (int dy : GROUND_STEP_HEIGHTS) {
                    int y = current.y() + dy;
                    if (Math.abs(y - route.expectedDoorY()) > 2) continue;
                    if (!walkableGround(level, x, y, z)) continue;
                    long key = nodeKey(x, y, z);
                    if (visited.add(key)) pending.addLast(new Node(x, y, z));
                    break;
                }
            }
        }
        return false;
    }

    private static boolean reachesRouteBase(Node current, WorldNode base, boolean baseIsStair) {
        if (!baseIsStair) {
            return current.x() == base.x() && current.y() == base.y() && current.z() == base.z();
        }
        return current.y() == base.y()
                && Math.abs(current.x() - base.x()) + Math.abs(current.z() - base.z()) == 1;
    }

    private static boolean walkableGround(ServerLevel level, int x, int y, int z) {
        BlockState feet = level.getBlockState(new BlockPos(x, y, z));
        BlockState head = level.getBlockState(new BlockPos(x, y + 1, z));
        BlockState floor = level.getBlockState(new BlockPos(x, y - 1, z));
        return bodyPassable(feet) && bodyPassable(head)
                && !floor.isAir() && floor.getFluidState().isEmpty();
    }

    private static boolean bodyPassable(BlockState state) {
        return state.isAir() || state.getBlock() instanceof DoorBlock;
    }

    private static boolean runtimeBodyClearable(
            BlockState state, boolean allowFreshConversionClear) {
        if (state.isAir()) return true;
        if (!allowFreshConversionClear) return false;
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

    private static boolean matchesStair(
            BlockState state, String role, Direction facing) {
        return state.getBlock() == stairBlock(role)
                && state.getValue(HorizontalDirectionalBlock.FACING) == facing;
    }

    private static Block stairBlock(String role) {
        return switch (role) {
            case "stable", "warehouse" -> Blocks.SPRUCE_STAIRS;
            case "guard_post", "bathhouse" -> Blocks.STONE_BRICK_STAIRS;
            default -> Blocks.OAK_STAIRS;
        };
    }

    private static Block supportBlock(String role) {
        return switch (role) {
            case "stable", "warehouse" -> Blocks.SPRUCE_PLANKS;
            case "guard_post", "bathhouse" -> Blocks.STONE_BRICKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private static WorldNode worldNode(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanSourceAirRoutePlanner.Node local) {
        RotatedPoint rotated = rotate(
                local.x(), local.z(), snapshot.width(), snapshot.length(), placement.rotation());
        return new WorldNode(
                placement.minX() + rotated.x(),
                placement.baseY() + local.y(),
                placement.minZ() + rotated.z());
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
        throw new IllegalArgumentException("Non-horizontal Erden route direction " + dx + "," + dz);
    }

    private static Bounds bounds(
            List<RouteNode> nodes, ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement) {
        int minX = placement.entrance().x();
        int maxX = minX;
        int minZ = placement.entrance().z();
        int maxZ = minZ;
        for (RouteNode node : nodes) {
            minX = Math.min(minX, node.world().x());
            maxX = Math.max(maxX, node.world().x());
            minZ = Math.min(minZ, node.world().z());
            maxZ = Math.max(maxZ, node.world().z());
        }
        return new Bounds(minX, maxX, minZ, maxZ);
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

    private static void requestCiSampleChunks(ServerLevel level) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        long tick = level.getGameTime();
        if (lastCiChunkRefreshTick != Long.MIN_VALUE
                && tick - lastCiChunkRefreshTick < 40L) return;
        lastCiChunkRefreshTick = tick;

        PlacementRoute sample = ROUTES.values().stream()
                .filter(route -> {
                    ExternalUrbanFabricBuilder.UrbanEntrance diagnostic =
                            ExternalUrbanFabricBuilder.diagnosticEntrance();
                    return route.entranceX() == diagnostic.x()
                            && route.entranceZ() == diagnostic.z();
                })
                .findFirst()
                .orElseGet(() -> ROUTES.values().iterator().next());
        ExternalUrbanFabricBuilder.UrbanEntrance sampleEntrance =
                ExternalUrbanFabricBuilder.entrances().stream()
                        .filter(entrance -> entrance.x() == sample.entranceX()
                                && entrance.z() == sample.entranceZ())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing Erden authored upper-route CI entrance "
                                        + sample.entranceX() + "," + sample.entranceZ()));

        // Ground materialization is a hard predecessor for the upper route. Refresh the complete
        // authored-ground placement lease and the route body together until the real-world route
        // proof succeeds. These are transient PORTAL leases only; once ciPassed becomes true no
        // further refresh occurs and the chunks naturally unload.
        ErdenUrbanInteriorBuilder.requestPlanChunksForCi(level, sampleEntrance);
        for (int chunkX = Math.floorDiv(sample.bounds().minX(), 16);
             chunkX <= Math.floorDiv(sample.bounds().maxX(), 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(sample.bounds().minZ(), 16);
                 chunkZ <= Math.floorDiv(sample.bounds().maxZ(), 16); chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                ErdenCapitalStreamingBuilder.retainDiagnosticChunk(level, chunkX, chunkZ);
            }
        }
        ciRouteKey = sample.entranceKey();
        if (!ciChunksRequested) {
            LivingKingdoms.LOGGER.info(
                    "Requested Erden authored upper-route CI sample role={} entrance={},{} bounded_route_chunks=true authored_ground_plan=true refreshed_until_verification=true refresh_ticks=40 loaded_lease=true persistent_forced_chunks=false",
                    sample.role(), sample.entranceX(), sample.entranceZ());
        }
        ciChunksRequested = true;
    }

    private static void verifyCiIfReady(
            ServerLevel level, ErdenUrbanAuthoredUpperRouteSavedData data) {
        if (ciPassed || ciRouteKey == Long.MIN_VALUE
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        if (!data.isCompleted(ciRouteKey, ROUTE_REVISION)) return;
        PlacementRoute route = ROUTES.get(ciRouteKey);
        if (route == null || !verify(level, route)) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_AUTHORED_UPPER_ROUTE_PASS role={} entrance={},{} path_nodes={} stair_blocks={} door_retained=true ground_to_stair=true existing_upper_room=true source_blocks_cut=0 source_air_only=true world_transform_verified=true synthetic_seal_provenance=true",
                route.role(), route.entranceX(), route.entranceZ(),
                route.nodes().size(), route.stairs().size());
    }

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long localKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
    }

    private static long nodeKey(int x, int y, int z) {
        long a = ((long) x & 0x1fffffL) << 43;
        long b = ((long) y & 0x3fffffL) << 21;
        long c = (long) z & 0x1fffffL;
        return a ^ b ^ c;
    }

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    private static final int[] GROUND_STEP_HEIGHTS = {0, 1, -1};

    private record PlacementRoute(
            long entranceKey,
            String role,
            String fragmentKey,
            int entranceX,
            int entranceZ,
            int expectedDoorY,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            List<RouteNode> nodes,
            Map<WorldNode, StairIntent> stairs,
            Set<Long> sourceOccupied,
            WorldNode target,
            Bounds bounds) {
    }

    private record RouteNode(
            ErdenUrbanSourceAirRoutePlanner.Node local,
            WorldNode world) {
    }

    private record StairIntent(WorldNode at, Direction facing) {
    }

    private record WorldNode(int x, int y, int z) {
        BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private record RotatedPoint(int x, int z) {
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }

    private record Node(int x, int y, int z) {
    }
}
