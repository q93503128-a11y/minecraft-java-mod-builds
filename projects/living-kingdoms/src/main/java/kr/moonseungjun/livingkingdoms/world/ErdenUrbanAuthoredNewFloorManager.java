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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Materializes the upper floors that the source-only structural audit approved inside genuine voids.
 *
 * <p>The immutable source schematic remains authoritative. Candidate floor cells were previously
 * proven to be enclosed source-air with roof clearance and structural support, and the route planner
 * independently proved a zero-cut stair route to that same level. This manager only transforms those
 * proven local coordinates into world coordinates. It never cuts a retained source block. A small
 * stairwell opening is retained wherever the verified route body crosses the proposed floor plane.</p>
 */
public final class ErdenUrbanAuthoredNewFloorManager {
    public static final int FLOOR_REVISION = 2;

    private static final int PROCESS_BUDGET = 1;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<Long, PlacementPlan> PLANS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private static MinecraftServer activeServer;
    private static boolean completionLogged;
    private static boolean ciChunksRequested;
    private static boolean ciPassed;
    private static long ciPlanKey = Long.MIN_VALUE;

    private ErdenUrbanAuthoredNewFloorManager() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PLANS.clear();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile> opportunities =
                ErdenUrbanUpperRoomOpportunityCatalog.profiles();
        Map<String, ErdenUrbanSourceAirRoutePlanner.RoutePlan> routes =
                ErdenUrbanSourceAirRoutePlanner.plans();
        Map<Long, ErdenUrbanNewFloorStructuralApprovalCatalog.PlacementApproval> approvals =
                ErdenUrbanNewFloorStructuralApprovalCatalog.placements();

        Map<String, Integer> roles = new LinkedHashMap<>();
        int examined = 0;
        int totalAuthoredCells = 0;
        int totalOpenings = 0;

        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            examined++;
            long key = entranceKey(placement.entrance().x(), placement.entrance().z());
            ErdenUrbanNewFloorStructuralApprovalCatalog.PlacementApproval approval = approvals.get(key);
            if (approval == null
                    || approval.decision()
                    != ErdenUrbanNewFloorStructuralApprovalCatalog.Decision.APPROVED_FOR_AUTHORING) {
                continue;
            }

            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot =
                    snapshots.get(placement.fragmentKey());
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity =
                    opportunities.get(placement.fragmentKey());
            ErdenUrbanSourceAirRoutePlanner.RoutePlan route = routes.get(placement.fragmentKey());
            if (snapshot == null || opportunity == null || route == null) {
                throw new IllegalStateException(
                        "Missing Erden source inputs for approved new floor " + placement.fragmentKey());
            }
            if (opportunity.recommendation()
                    != ErdenUrbanUpperRoomOpportunityCatalog.Recommendation.AUTHOR_NEW_FLOOR_IN_VOID
                    || route.classification()
                    != ErdenUrbanSourceAirRoutePlanner.RouteClassification.ZERO_CUT_ROUTE
                    || route.targetMode()
                    != ErdenUrbanUpperRoomOpportunityCatalog.FloorMode.NEW_AUTHORED_FLOOR
                    || route.path().isEmpty()) {
                throw new IllegalStateException(
                        "Approved Erden new floor lost its zero-cut route " + placement.fragmentKey());
            }

            PlacementPlan plan = transformAndValidate(placement, snapshot, opportunity, route);
            if (PLANS.put(key, plan) != null) {
                throw new IllegalStateException("Duplicate Erden authored-new-floor entrance "
                        + placement.entrance().x() + "," + placement.entrance().z());
            }
            roles.merge(placement.role(), 1, Integer::sum);
            totalAuthoredCells += plan.floorBlocks().size();
            totalOpenings += plan.stairwellOpenings();
        }

        int approved = ErdenUrbanNewFloorStructuralApprovalCatalog.approvedCount();
        if (examined != ExternalUrbanFabricBuilder.plotCount() || examined != 233) {
            throw new IllegalStateException("Erden authored-new-floor placement count drifted: " + examined);
        }
        if (approved <= 0 || approvals.size() != approved || PLANS.size() != approved) {
            throw new IllegalStateException("Erden authored-new-floor approval drift approvals="
                    + approvals.size() + " approved=" + approved + " materializers=" + PLANS.size());
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden authored new floors eligible={} examined={} roles={} authored_floor_cells={} stairwell_openings={} approval_truth=true zero_cut_routes=true source_air_floor=true source_blocks_cut=0 placement_counts_unchanged=true revision={}",
                PLANS.size(), examined, roles, totalAuthoredCells, totalOpenings, FLOOR_REVISION);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        bootstrap();
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        requestCiSampleChunks(level);
        ErdenUrbanAuthoredNewFloorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanAuthoredNewFloorSavedData.TYPE);
        ErdenUrbanInteriorSavedData ground = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);

        int processed = 0;
        for (PlacementPlan plan : PLANS.values()) {
            if (processed >= PROCESS_BUDGET) break;
            long key = plan.entranceKey();
            if (data.isCompleted(key, FLOOR_REVISION)) continue;
            if (!ground.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) {
                logCiStage(level, plan, "ground_not_complete");
                continue;
            }
            if (!chunksReady(level, plan.bounds())) {
                logCiStage(level, plan, "chunks_not_ready");
                continue;
            }

            if (!data.isPrepared(key, FLOOR_REVISION)) {
                if (!materialize(level, plan, true)) {
                    logCiStage(level, plan, "materialize_rejected");
                    continue;
                }
                data.markPrepared(key, FLOOR_REVISION);
                if (plan.entranceKey() == ciPlanKey) {
                    LivingKingdoms.LOGGER.info(
                            "LK_ERDEN_AUTHORED_NEW_FLOOR_STAGE role={} entrance={},{} stage=materialized floor_cells={} route_nodes={} stairs={}",
                            plan.role(), plan.entranceX(), plan.entranceZ(),
                            plan.floorBlocks().size(), plan.routeNodes().size(), plan.stairs().size());
                }
                LivingKingdoms.LOGGER.debug(
                        "Prepared Erden authored new floor role={} entrance={},{} floor_cells={} route_nodes={} stairs={} openings={} source_blocks_cut=0",
                        plan.role(), plan.entranceX(), plan.entranceZ(), plan.floorBlocks().size(),
                        plan.routeNodes().size(), plan.stairs().size(), plan.stairwellOpenings());
            }
            boolean verified = verify(level, plan);
            if (!verified) logCiStage(level, plan, "verify_rejected");
            if (verified) {
                data.markCompleted(key, FLOOR_REVISION);
            }
            processed++;
        }

        int complete = data.completedCount(FLOOR_REVISION);
        if (!completionLogged && complete == PLANS.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden authored new floors buildings={} zero_cut_stairs=true enclosed_upper_rooms=true source_blocks_cut=0 synthetic_floor_required=false revision={}",
                    complete, FLOOR_REVISION);
        }
        verifyCiIfReady(level, data);
    }

    public static int eligibleCount() {
        bootstrap();
        return PLANS.size();
    }

    public static boolean isEligible(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        return PLANS.containsKey(entranceKey(entrance.x(), entrance.z()));
    }

    public static boolean isPrepared(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        long key = entranceKey(entrance.x(), entrance.z());
        if (!PLANS.containsKey(key)) return false;
        return level.getDataStorage().computeIfAbsent(ErdenUrbanAuthoredNewFloorSavedData.TYPE)
                .isPrepared(key, FLOOR_REVISION);
    }

    public static boolean isCompleted(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        long key = entranceKey(entrance.x(), entrance.z());
        if (!PLANS.containsKey(key)) return false;
        return level.getDataStorage().computeIfAbsent(ErdenUrbanAuthoredNewFloorSavedData.TYPE)
                .isCompleted(key, FLOOR_REVISION);
    }

    public static BlockPos verifiedUpperTarget(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        long key = entranceKey(entrance.x(), entrance.z());
        PlacementPlan plan = PLANS.get(key);
        if (plan == null) return null;
        ErdenUrbanAuthoredNewFloorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanAuthoredNewFloorSavedData.TYPE);
        if (!data.isCompleted(key, FLOOR_REVISION)) return null;
        return plan.target().pos();
    }

    public static void verifyOrThrow(
            ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        PlacementPlan plan = PLANS.get(entranceKey(entrance.x(), entrance.z()));
        if (plan == null) {
            throw new IllegalStateException("Entrance has no authored new floor: "
                    + entrance.x() + "," + entrance.z());
        }
        if (!verify(level, plan)) {
            throw new IllegalStateException("Erden authored new-floor verification failed role="
                    + entrance.role() + " entrance=" + entrance.x() + "," + entrance.z());
        }
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        completionLogged = false;
        ciChunksRequested = false;
        ciPassed = false;
        ciPlanKey = Long.MIN_VALUE;
    }

    private static PlacementPlan transformAndValidate(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity,
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
            throw new IllegalStateException("Missing retained source door for new floor "
                    + placement.fragmentKey());
        }

        RotatedPoint localEntrance = rotate(
                snapshot.entranceX(), snapshot.entranceZ(),
                snapshot.width(), snapshot.length(), placement.rotation());
        int expectedEntranceX = placement.minX() + localEntrance.x();
        int expectedEntranceZ = placement.minZ() + localEntrance.z();
        if (expectedEntranceX != placement.entrance().x()
                || expectedEntranceZ != placement.entrance().z()) {
            throw new IllegalStateException("Erden new-floor transform drift fragment="
                    + placement.fragmentKey());
        }

        List<RouteNode> routeNodes = new ArrayList<>();
        for (ErdenUrbanSourceAirRoutePlanner.Node local : sourceRoute.path()) {
            if (sourceOccupied.contains(localKey(local.x(), local.y(), local.z()))
                    || sourceOccupied.contains(localKey(local.x(), local.y() + 1, local.z()))) {
                throw new IllegalStateException("New-floor route intersects retained source block fragment="
                        + placement.fragmentKey() + " local=" + local);
            }
            routeNodes.add(new RouteNode(local, worldNode(placement, snapshot, local)));
        }
        if (routeNodes.isEmpty()) {
            throw new IllegalStateException("Empty Erden new-floor route " + placement.fragmentKey());
        }

        ErdenUrbanUpperRoomOpportunityCatalog.LevelOpportunity floor = opportunity.newFloorVoid();
        ErdenUrbanUpperRoomOpportunityCatalog.Region region = floor.regions().stream()
                .max(Comparator.comparingInt(value -> value.cells().size()))
                .orElseThrow(() -> new IllegalStateException(
                        "Approved Erden new floor has no region " + placement.fragmentKey()));
        if (floor.feetY() != sourceRoute.targetFeetY()) {
            throw new IllegalStateException("Erden new-floor target level drift fragment="
                    + placement.fragmentKey() + " opportunity=" + floor.feetY()
                    + " route=" + sourceRoute.targetFeetY());
        }

        Set<Long> targetCells = new HashSet<>(region.cells());
        RouteNode target = routeNodes.getLast();
        if (target.local().y() != floor.feetY()
                || !targetCells.contains(cellKey(target.local().x(), target.local().z()))) {
            throw new IllegalStateException("Erden new-floor route misses approved target region "
                    + placement.fragmentKey());
        }

        Map<WorldNode, StairIntent> stairs = stairIntents(placement, routeNodes, sourceOccupied);
        Set<BlockPos> routeFeet = new HashSet<>();
        Set<BlockPos> routeHeads = new HashSet<>();
        for (RouteNode node : routeNodes) {
            routeFeet.add(node.world().pos());
            routeHeads.add(new BlockPos(node.world().x(), node.world().y() + 1, node.world().z()));
        }

        Set<BlockPos> routeBody = new HashSet<>(routeFeet);
        routeBody.addAll(routeHeads);
        for (RouteNode node : routeNodes) {
            if (stairs.containsKey(node.world())) continue;
            int supportLocalY = node.local().y() - 1;
            if (sourceOccupied.contains(localKey(
                    node.local().x(), supportLocalY, node.local().z()))) continue;
            BlockPos supportPos = new BlockPos(
                    node.world().x(), node.world().y() - 1, node.world().z());
            if (routeBody.contains(supportPos)) {
                throw new IllegalStateException(
                        "Erden required flat-route support collides with route body fragment="
                                + placement.fragmentKey() + " local=" + node.local()
                                + " support=" + supportPos);
            }
        }

        Block floorBlock = floorBlock(placement.role());
        Set<BlockPos> floorBlocks = new LinkedHashSet<>();
        int openings = 0;
        int localFloorY = floor.feetY() - 1;
        for (long cell : region.cells()) {
            int localX = cellX(cell);
            int localZ = cellZ(cell);
            if (sourceOccupied.contains(localKey(localX, localFloorY, localZ))) {
                throw new IllegalStateException("Approved Erden floor would overwrite source block fragment="
                        + placement.fragmentKey() + " local=" + localX + "," + localFloorY + "," + localZ);
            }
            RotatedPoint rotated = rotate(
                    localX, localZ, snapshot.width(), snapshot.length(), placement.rotation());
            BlockPos world = new BlockPos(
                    placement.minX() + rotated.x(),
                    placement.baseY() + localFloorY,
                    placement.minZ() + rotated.z());
            if (routeFeet.contains(world) || routeHeads.contains(world)) {
                openings++;
                continue;
            }
            floorBlocks.add(world);
        }
        if (floorBlocks.size() < 24) {
            throw new IllegalStateException("Erden authored new floor became too small after stairwell "
                    + placement.fragmentKey() + " cells=" + floorBlocks.size());
        }

        BlockPos targetFloor = new BlockPos(
                target.world().x(), target.world().y() - 1, target.world().z());
        if (!floorBlocks.contains(targetFloor) && !stairs.containsKey(worldNode(targetFloor))) {
            throw new IllegalStateException("Erden new-floor target has no authored support fragment="
                    + placement.fragmentKey());
        }

        Bounds bounds = bounds(placement, floorBlocks, routeNodes);
        return new PlacementPlan(
                entranceKey(placement.entrance().x(), placement.entrance().z()),
                placement.role(), placement.fragmentKey(),
                placement.entrance().x(), placement.entrance().z(),
                placement.baseY() + doorLocalY,
                List.copyOf(floorBlocks), floorBlock,
                List.copyOf(routeNodes), Map.copyOf(stairs), Set.copyOf(sourceOccupied),
                target.world(), openings, bounds);
    }

    private static Map<WorldNode, StairIntent> stairIntents(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            List<RouteNode> nodes,
            Set<Long> sourceOccupied) {
        Map<WorldNode, StairIntent> stairs = new LinkedHashMap<>();
        for (int index = 1; index < nodes.size(); index++) {
            RouteNode previous = nodes.get(index - 1);
            RouteNode current = nodes.get(index);
            int dx = current.world().x() - previous.world().x();
            int dz = current.world().z() - previous.world().z();
            int dy = current.world().y() - previous.world().y();
            if (Math.abs(dx) + Math.abs(dz) != 1 || Math.abs(dy) > 1) {
                throw new IllegalStateException("Invalid Erden new-floor route edge fragment="
                        + placement.fragmentKey() + " edge=" + previous.world() + "->" + current.world());
            }
            if (dy == 0) continue;

            RouteNode lower = dy > 0 ? previous : current;
            RouteNode higher = dy > 0 ? current : previous;
            if (sourceOccupied.contains(localKey(
                    lower.local().x(), lower.local().y(), lower.local().z()))) {
                throw new IllegalStateException("Erden new-floor stair would overwrite source fragment="
                        + placement.fragmentKey() + " local=" + lower.local());
            }
            Direction facing = horizontalDirection(
                    higher.world().x() - lower.world().x(),
                    higher.world().z() - lower.world().z());
            StairIntent old = stairs.putIfAbsent(
                    lower.world(), new StairIntent(lower.world(), facing));
            if (old != null && old.facing() != facing) {
                throw new IllegalStateException("Conflicting Erden new-floor stair intents fragment="
                        + placement.fragmentKey());
            }
        }
        if (stairs.isEmpty()) {
            throw new IllegalStateException("Erden new-floor route has no stair edge "
                    + placement.fragmentKey());
        }
        return stairs;
    }

    private static boolean materialize(
            ServerLevel level, PlacementPlan plan, boolean allowLegacyConversionClear) {
        if (!(level.getBlockState(new BlockPos(
                plan.entranceX(), plan.expectedDoorY(), plan.entranceZ())).getBlock()
                instanceof DoorBlock)) {
            return false;
        }

        for (BlockPos floor : plan.floorBlocks()) {
            BlockState state = level.getBlockState(floor);
            if (state.getBlock() != plan.floorBlock()
                    && !runtimeBodyClearable(state, allowLegacyConversionClear)) return false;
        }
        for (RouteNode node : plan.routeNodes()) {
            StairIntent stair = plan.stairs().get(node.world());
            BlockState feet = level.getBlockState(node.world().pos());
            if (stair == null) {
                if (!runtimeBodyClearable(feet, allowLegacyConversionClear)) return false;
            } else if (!matchesStair(feet, plan.role(), stair.facing())
                    && !runtimeBodyClearable(feet, allowLegacyConversionClear)) {
                return false;
            }
            BlockState head = level.getBlockState(new BlockPos(
                    node.world().x(), node.world().y() + 1, node.world().z()));
            if (!runtimeBodyClearable(head, allowLegacyConversionClear)) return false;
        }

        for (BlockPos floor : plan.floorBlocks()) {
            if (level.getBlockState(floor).getBlock() != plan.floorBlock()) {
                level.setBlock(floor, plan.floorBlock().defaultBlockState(), UPDATE_FLAGS);
            }
        }

        Block support = supportBlock(plan.role());
        Set<BlockPos> protectedRouteBody = new HashSet<>();
        for (RouteNode routeNode : plan.routeNodes()) {
            protectedRouteBody.add(routeNode.world().pos());
            protectedRouteBody.add(new BlockPos(
                    routeNode.world().x(), routeNode.world().y() + 1, routeNode.world().z()));
        }
        for (RouteNode node : plan.routeNodes()) {
            StairIntent stair = plan.stairs().get(node.world());
            BlockPos feetPos = node.world().pos();
            if (stair == null) {
                if (!level.getBlockState(feetPos).isAir()) {
                    level.setBlock(feetPos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                }
            } else {
                BlockState desired = stairBlock(plan.role()).defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, stair.facing());
                if (!level.getBlockState(feetPos).equals(desired)) {
                    level.setBlock(feetPos, desired, UPDATE_FLAGS);
                }
            }
            BlockPos head = new BlockPos(
                    node.world().x(), node.world().y() + 1, node.world().z());
            if (!level.getBlockState(head).isAir()) {
                level.setBlock(head, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            }

            int localFloorY = node.local().y() - 1;
            boolean sourceFloorAir = !plan.sourceOccupied().contains(localKey(
                    node.local().x(), localFloorY, node.local().z()));
            BlockPos supportPos = new BlockPos(
                    node.world().x(), node.world().y() - 1, node.world().z());
            boolean supportHitsRouteBody = protectedRouteBody.contains(supportPos);
            if (sourceFloorAir && supportHitsRouteBody && stair == null) {
                return false;
            }
            if (sourceFloorAir && !supportHitsRouteBody
                    && level.getBlockState(supportPos).isAir()) {
                level.setBlock(supportPos, support.defaultBlockState(), UPDATE_FLAGS);
            }
        }
        return true;
    }

    private static boolean verify(ServerLevel level, PlacementPlan plan) {
        BlockPos doorPos = new BlockPos(
                plan.entranceX(), plan.expectedDoorY(), plan.entranceZ());
        BlockState door = level.getBlockState(doorPos);
        if (!(door.getBlock() instanceof DoorBlock)) {
            logCiVerifyFailure(level, plan, "door pos=" + doorPos + " state=" + door);
            return false;
        }

        for (BlockPos floor : plan.floorBlocks()) {
            BlockState floorState = level.getBlockState(floor);
            if (floorState.getBlock() != plan.floorBlock()) {
                logCiVerifyFailure(level, plan,
                        "floor pos=" + floor + " state=" + floorState
                                + " expected=" + plan.floorBlock());
                return false;
            }
            BlockPos headPos = floor.above();
            BlockState head = level.getBlockState(headPos);
            if (!head.isAir()) {
                logCiVerifyFailure(level, plan,
                        "floor_head pos=" + headPos + " state=" + head);
                return false;
            }
        }
        for (RouteNode node : plan.routeNodes()) {
            BlockPos feetPos = node.world().pos();
            BlockState feet = level.getBlockState(feetPos);
            StairIntent stair = plan.stairs().get(node.world());
            if (stair == null) {
                if (!feet.isAir()) {
                    logCiVerifyFailure(level, plan,
                            "route_feet local=" + node.local() + " pos=" + feetPos
                                    + " state=" + feet);
                    return false;
                }
            } else if (!matchesStair(feet, plan.role(), stair.facing())) {
                logCiVerifyFailure(level, plan,
                        "stair local=" + node.local() + " pos=" + feetPos
                                + " state=" + feet + " expected_facing=" + stair.facing());
                return false;
            }
            BlockPos routeHeadPos = new BlockPos(
                    node.world().x(), node.world().y() + 1, node.world().z());
            BlockState routeHead = level.getBlockState(routeHeadPos);
            if (!routeHead.isAir()) {
                logCiVerifyFailure(level, plan,
                        "route_head local=" + node.local() + " pos=" + routeHeadPos
                                + " state=" + routeHead);
                return false;
            }
        }

        BlockPos targetSupportPos = new BlockPos(
                plan.target().x(), plan.target().y() - 1, plan.target().z());
        BlockState targetSupport = level.getBlockState(targetSupportPos);
        if (targetSupport.isAir()) {
            logCiVerifyFailure(level, plan,
                    "target_support pos=" + targetSupportPos + " state=" + targetSupport);
            return false;
        }
        BlockState targetFeet = level.getBlockState(plan.target().pos());
        if (!targetFeet.isAir()) {
            logCiVerifyFailure(level, plan,
                    "target_feet pos=" + plan.target().pos() + " state=" + targetFeet);
            return false;
        }
        BlockState targetHead = level.getBlockState(plan.target().pos().above());
        if (!targetHead.isAir()) {
            logCiVerifyFailure(level, plan,
                    "target_head pos=" + plan.target().pos().above() + " state=" + targetHead);
            return false;
        }
        return true;
    }

    private static void logCiVerifyFailure(
            ServerLevel level, PlacementPlan plan, String reason) {
        if (plan.entranceKey() != ciPlanKey || level.getGameTime() % 20L != 0L) return;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_AUTHORED_NEW_FLOOR_VERIFY_FAIL role={} entrance={},{} reason={}",
                plan.role(), plan.entranceX(), plan.entranceZ(), reason);
    }

    private static void requestCiSampleChunks(ServerLevel level) {
        if (ciChunksRequested
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ExternalUrbanFabricBuilder.UrbanEntrance diagnostic =
                ExternalUrbanFabricBuilder.diagnosticEntrance();
        PlacementPlan sample = diagnostic == null ? null
                : PLANS.get(entranceKey(diagnostic.x(), diagnostic.z()));
        if (sample == null) {
            sample = PLANS.values().stream().findFirst().orElse(null);
        }
        if (sample == null) return;
        for (int chunkX = Math.floorDiv(sample.bounds().minX(), 16);
             chunkX <= Math.floorDiv(sample.bounds().maxX(), 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(sample.bounds().minZ(), 16);
                 chunkZ <= Math.floorDiv(sample.bounds().maxZ(), 16); chunkZ++) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                // Capital streaming releases its construction ticket as soon as this cell is
                // built. The authored-floor sample spans several cells, so retain only this
                // deliberate CI footprint until every cell can be verified together.
                level.setChunkForced(chunkX, chunkZ, true);
            }
        }
        ciPlanKey = sample.entranceKey();
        ciChunksRequested = true;
        LivingKingdoms.LOGGER.info(
                "Retained Erden authored-new-floor CI sample role={} entrance={},{} bounds={}..{} x {}..{} floor_cells={} route_nodes={}",
                sample.role(), sample.entranceX(), sample.entranceZ(),
                sample.bounds().minX(), sample.bounds().maxX(),
                sample.bounds().minZ(), sample.bounds().maxZ(),
                sample.floorBlocks().size(), sample.routeNodes().size());
    }

    private static void verifyCiIfReady(
            ServerLevel level, ErdenUrbanAuthoredNewFloorSavedData data) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || ciPlanKey == Long.MIN_VALUE
                || !data.isCompleted(ciPlanKey, FLOOR_REVISION)) return;
        PlacementPlan sample = PLANS.get(ciPlanKey);
        if (sample == null || !verify(level, sample)) return;
        ciPassed = true;
        for (int chunkX = Math.floorDiv(sample.bounds().minX(), 16);
             chunkX <= Math.floorDiv(sample.bounds().maxX(), 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(sample.bounds().minZ(), 16);
                 chunkZ <= Math.floorDiv(sample.bounds().maxZ(), 16); chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, false);
            }
        }
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_AUTHORED_NEW_FLOOR_PASS candidates={} sample_role={} sample_floor_cells={} sample_route_nodes={} sample_stairs={} stairwell_openings={} target_y={} source_blocks_cut=0 source_air_floor=true structural_approval=true zero_cut_route=true synthetic_floor_required=false ci_chunks_released=true revision={}",
                PLANS.size(), sample.role(), sample.floorBlocks().size(),
                sample.routeNodes().size(), sample.stairs().size(), sample.stairwellOpenings(),
                sample.target().y(), FLOOR_REVISION);
    }

    private static void logCiStage(
            ServerLevel level, PlacementPlan plan, String stage) {
        if (plan.entranceKey() != ciPlanKey || level.getGameTime() % 40L != 0L) return;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_AUTHORED_NEW_FLOOR_WAIT role={} entrance={},{} stage={}",
                plan.role(), plan.entranceX(), plan.entranceZ(), stage);
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
            List<RouteNode> routeNodes) {
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
        for (RouteNode node : routeNodes) {
            minX = Math.min(minX, node.world().x());
            maxX = Math.max(maxX, node.world().x());
            minZ = Math.min(minZ, node.world().z());
            maxZ = Math.max(maxZ, node.world().z());
        }
        return new Bounds(minX, maxX, minZ, maxZ);
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

    private static Block stairBlock(String role) {
        return switch (role) {
            case "warehouse" -> Blocks.SPRUCE_STAIRS;
            default -> Blocks.OAK_STAIRS;
        };
    }

    private static Block supportBlock(String role) {
        return switch (role) {
            case "warehouse" -> Blocks.SPRUCE_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private static Block floorBlock(String role) {
        return switch (role) {
            case "warehouse" -> Blocks.SPRUCE_PLANKS;
            case "tenement", "shop", "bakery" -> Blocks.OAK_PLANKS;
            default -> throw new IllegalArgumentException("Unsupported Erden new-floor role " + role);
        };
    }

    private static boolean matchesStair(BlockState state, String role, Direction facing) {
        return state.getBlock() == stairBlock(role)
                && state.getValue(HorizontalDirectionalBlock.FACING) == facing;
    }

    private static boolean runtimeBodyClearable(
            BlockState state, boolean allowLegacyConversionClear) {
        if (state.isAir()) return true;
        if (!allowLegacyConversionClear) return false;
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

    private record RotatedPoint(int x, int z) {
    }

    private record WorldNode(int x, int y, int z) {
        private BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private record RouteNode(
            ErdenUrbanSourceAirRoutePlanner.Node local,
            WorldNode world) {
    }

    private record StairIntent(WorldNode world, Direction facing) {
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }

    private record PlacementPlan(
            long entranceKey,
            String role,
            String fragmentKey,
            int entranceX,
            int entranceZ,
            int expectedDoorY,
            List<BlockPos> floorBlocks,
            Block floorBlock,
            List<RouteNode> routeNodes,
            Map<WorldNode, StairIntent> stairs,
            Set<Long> sourceOccupied,
            WorldNode target,
            int stairwellOpenings,
            Bounds bounds) {
    }
}
