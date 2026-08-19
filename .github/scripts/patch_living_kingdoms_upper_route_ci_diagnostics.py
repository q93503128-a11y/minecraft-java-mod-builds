from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanAuthoredUpperRouteManager.java')
text = path.read_text(encoding='utf-8')


def replace_once(old: str, new: str, label: str) -> None:
    global text
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'{label}: expected source block not found')
    text = text.replace(old, new, 1)


replace_once(
    '''        verifyCiIfReady(level, routes);\n    }\n\n    public static int eligibleCount() {\n''',
    '''        logCiStateIfNeeded(level, routes, ground);\n        verifyCiIfReady(level, routes);\n    }\n\n    public static int eligibleCount() {\n''',
    'upper-route CI state hook')

marker = '''    private static void verifyCiIfReady(\n            ServerLevel level, ErdenUrbanAuthoredUpperRouteSavedData data) {\n'''
helper = r'''    private static void logCiStateIfNeeded(
            ServerLevel level,
            ErdenUrbanAuthoredUpperRouteSavedData data,
            ErdenUrbanInteriorSavedData ground) {
        if (ciPassed || ciRouteKey == Long.MIN_VALUE
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        long tick = level.getGameTime();
        if (tick % 40L != 0L) return;
        PlacementRoute route = ROUTES.get(ciRouteKey);
        if (route == null) return;
        boolean groundComplete = ground.isComplete(
                ciRouteKey, ErdenUrbanInteriorBuilder.INTERIOR_REVISION);
        boolean routeChunksReady = chunksReady(level, route.bounds());
        BlockState doorState = level.getBlockState(new BlockPos(
                route.entranceX(), route.expectedDoorY(), route.entranceZ()));
        boolean doorPresent = doorState.getBlock() instanceof DoorBlock;
        boolean prepared = data.isPrepared(ciRouteKey, ROUTE_REVISION);
        boolean completed = data.isCompleted(ciRouteKey, ROUTE_REVISION);
        String stage;
        if (!groundComplete) {
            stage = "ground_incomplete";
        } else if (!routeChunksReady) {
            stage = "route_chunks_not_ready";
        } else if (!doorPresent) {
            stage = "door_missing:" + doorState;
        } else if (!prepared) {
            stage = prepareFailureReason(level, route, true);
        } else if (!completed) {
            stage = verifyFailureReason(level, route);
        } else {
            stage = verifyFailureReason(level, route);
        }
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_AUTHORED_UPPER_ROUTE_CI_STATE role={} entrance={},{} ground_complete={} chunks_ready={} door_present={} prepared={} completed={} stage={} tick={} persistent_forced_chunks=false",
                route.role(), route.entranceX(), route.entranceZ(), groundComplete,
                routeChunksReady, doorPresent, prepared, completed, stage, tick);
    }

    private static String prepareFailureReason(
            ServerLevel level, PlacementRoute route, boolean allowFreshConversionClear) {
        BlockState door = level.getBlockState(new BlockPos(
                route.entranceX(), route.expectedDoorY(), route.entranceZ()));
        if (!(door.getBlock() instanceof DoorBlock)) return "prepare_door_missing:" + door;
        for (RouteNode node : route.nodes()) {
            StairIntent stair = route.stairs().get(node.world());
            BlockState feet = level.getBlockState(node.world().pos());
            if (stair == null) {
                if (!runtimeBodyClearable(feet, allowFreshConversionClear)) {
                    return "prepare_feet_blocked@" + node.world() + ":" + feet;
                }
            } else if (!matchesStair(feet, route.role(), stair.facing())
                    && !runtimeBodyClearable(feet, allowFreshConversionClear)) {
                return "prepare_stair_blocked@" + node.world() + ":" + feet;
            }
            BlockPos headPos = new BlockPos(
                    node.world().x(), node.world().y() + 1, node.world().z());
            BlockState head = level.getBlockState(headPos);
            if (!runtimeBodyClearable(head, allowFreshConversionClear)) {
                return "prepare_head_blocked@" + headPos + ":" + head;
            }
        }
        return "prepare_ready";
    }

    private static String verifyFailureReason(ServerLevel level, PlacementRoute route) {
        BlockState door = level.getBlockState(new BlockPos(
                route.entranceX(), route.expectedDoorY(), route.entranceZ()));
        if (!(door.getBlock() instanceof DoorBlock)) return "verify_door_missing:" + door;
        for (RouteNode node : route.nodes()) {
            BlockState feet = level.getBlockState(node.world().pos());
            StairIntent stair = route.stairs().get(node.world());
            if (stair == null) {
                if (!feet.isAir()) return "verify_feet_blocked@" + node.world() + ":" + feet;
            } else if (!matchesStair(feet, route.role(), stair.facing())) {
                return "verify_stair_mismatch@" + node.world() + ":" + feet;
            }
            BlockPos headPos = new BlockPos(
                    node.world().x(), node.world().y() + 1, node.world().z());
            BlockState head = level.getBlockState(headPos);
            if (!head.isAir()) return "verify_head_blocked@" + headPos + ":" + head;
            BlockPos floorPos = new BlockPos(
                    node.world().x(), node.world().y() - 1, node.world().z());
            BlockState floor = level.getBlockState(floorPos);
            if (floor.isAir()) return "verify_floor_air@" + floorPos;
            if (!floor.getFluidState().isEmpty()) return "verify_floor_fluid@" + floorPos + ":" + floor;
        }
        RouteNode endpoint = route.nodes().getLast();
        if (!endpoint.world().equals(route.target())) return "verify_endpoint_mismatch";
        if (!groundCanReachRouteBase(level, route)) return "verify_ground_to_route_base_unreachable";
        return "verify_ready";
    }

'''
if helper not in text:
    if marker not in text:
        raise SystemExit('upper-route verify marker not found')
    text = text.replace(marker, helper + marker, 1)

if 'LK_ERDEN_AUTHORED_UPPER_ROUTE_CI_STATE' not in text:
    raise SystemExit('upper-route diagnostic state marker missing')
if 'prepareFailureReason' not in text or 'verifyFailureReason' not in text:
    raise SystemExit('upper-route failure-reason helpers missing')

path.write_text(text, encoding='utf-8')
print('Living Kingdoms upper-route CI stage diagnostics installed')
