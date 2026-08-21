package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Layers government functions into the imported 124 x 80 x 124 Erden citadel without carving or
 * replacing the attributed architecture. A room is accepted only when it already has a floor,
 * headroom, a nearby ceiling and enclosing walls. Fixtures are placed only into empty supported
 * cells, so doors, facade, roof and authored decoration remain authoritative.
 */
public final class ErdenCitadelInteriorManager {
    public static final int INTERIOR_REVISION = 4;

    private static final int PROCESS_INTERVAL = 20;
    private static final int PROCESS_BUDGET = 1;
    private static final int SEARCH_RADIUS = 16;
    private static final int FIXTURE_SEARCH_RADIUS = 3;
    private static final int FURNISHING_ROOM_RADIUS = 8;
    private static final int MAX_ROOM_HEIGHT = 14;
    private static final int MAX_WALL_DISTANCE = 14;
    private static final int MIN_ZONE_FIXTURES = 4;
    private static final int CITADEL_HALF_SIZE = 62;
    private static final int MAX_WALK_NODES = 160_000;
    private static final int CI_CHUNK_RADIUS = 4;
    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final List<Zone> ZONES = List.of(
            new Zone("audience_hall", 0, -34, List.of(
                    new Fixture(0, 3, Blocks.DARK_OAK_STAIRS),
                    new Fixture(-2, 3, Blocks.DARK_OAK_FENCE),
                    new Fixture(2, 3, Blocks.DARK_OAK_FENCE),
                    new Fixture(-3, 0, Blocks.CHISELED_STONE_BRICKS),
                    new Fixture(3, 0, Blocks.CHISELED_STONE_BRICKS),
                    new Fixture(0, 1, Blocks.LECTERN))),
            new Zone("royal_council", 0, 28, civic(
                    Blocks.LECTERN, Blocks.BOOKSHELF, Blocks.CARTOGRAPHY_TABLE,
                    Blocks.CHEST, Blocks.BOOKSHELF, Blocks.BARREL)),
            new Zone("royal_chancery", -32, -6, civic(
                    Blocks.LECTERN, Blocks.CARTOGRAPHY_TABLE, Blocks.BOOKSHELF,
                    Blocks.BARREL, Blocks.CHEST, Blocks.BOOKSHELF)),
            new Zone("royal_archives", 32, -6, civic(
                    Blocks.BOOKSHELF, Blocks.BOOKSHELF, Blocks.CHEST,
                    Blocks.LECTERN, Blocks.BARREL, Blocks.BOOKSHELF)),
            new Zone("guard_command", -38, 32, civic(
                    Blocks.TARGET, Blocks.ANVIL, Blocks.GRINDSTONE,
                    Blocks.BARREL, Blocks.CHEST, Blocks.IRON_BARS)),
            new Zone("service_quarter", 38, 32, civic(
                    Blocks.CRAFTING_TABLE, Blocks.SMOKER, Blocks.FURNACE,
                    Blocks.BARREL, Blocks.CAULDRON, Blocks.CHEST))
    );

    private static MinecraftServer activeServer;
    private static boolean preparedLogged;
    private static boolean completionLogged;
    private static boolean ciPassLogged;
    private static boolean ciTicketHeld;
    private static ChunkPos ciTicketCenter;
    private static final Map<String, ZoneResult> SESSION_RESULTS = new HashMap<>();
    private static final Map<String, Integer> FAILED_SCANS = new HashMap<>();

    private ErdenCitadelInteriorManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (level.getGameTime() % PROCESS_INTERVAL != 0L) return;

        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, "erden_kingdom");
        if (site == null || !site.built()) return;
        retainCitadelForCi(level, site);
        logPreparedOnce(site);

        ErdenCitadelInteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenCitadelInteriorSavedData.TYPE);
        int processed = 0;
        int processBudget = ciMode() ? ZONES.size() : PROCESS_BUDGET;
        for (Zone zone : ZONES) {
            if (processed >= processBudget) break;
            if (data.isComplete(zone.id, INTERIOR_REVISION)) continue;
            try {
                ZoneResult result = tryFurnishZone(level, site, zone);
                int required = requiredFixtures(zone);
                if (result == null || result.fixtures < required) {
                    logFailedScanIfUseful(level, site, zone, result);
                    continue;
                }
                data.markComplete(zone.id, INTERIOR_REVISION);
                SESSION_RESULTS.put(zone.id, result);
                FAILED_SCANS.remove(zone.id);
                processed++;
                LivingKingdoms.LOGGER.info(
                        "Completed Erden citadel zone={} anchor={},{},{} fixtures={} required={} connected_cells={} enclosed=true furnishing_connected=true stepped_furnishing=true unique_fixture_cells=true facade_replaced=false",
                        zone.id, result.anchor.x, result.anchor.floorY + 1, result.anchor.z,
                        result.fixtures, required, result.connectedCells);
            } catch (Throwable throwable) {
                LivingKingdoms.LOGGER.error("Unable to furnish Erden citadel zone={}", zone.id, throwable);
            }
        }

        int completed = data.completedCount(INTERIOR_REVISION);
        if (!completionLogged && completed == ZONES.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden functional citadel zones={} standard_minimum_fixtures={} guard_minimum_fixtures=3 non_destructive=true revision={}",
                    completed, MIN_ZONE_FIXTURES, INTERIOR_REVISION);
        }
        verifyCiIfReady(level, site, completed);
    }

    private static int requiredFixtures(Zone zone) {
        // The imported citadel has two deliberately compact working rooms. Their first three
        // fixtures are the role-defining workstations; storage/utility furniture is optional and
        // must not justify carving or spilling furniture through authored walls just to reach a
        // generic four-fixture quota.
        return switch (zone.id) {
            case "guard_command", "service_quarter" -> 3;
            default -> MIN_ZONE_FIXTURES;
        };
    }

    private static void reset(MinecraftServer server) {
        if (activeServer != null && ciTicketHeld && ciTicketCenter != null) {
            ServerLevel oldLevel = activeServer.getLevel(StarterRealmManager.REALM_KEY);
            if (oldLevel != null) {
                oldLevel.getChunkSource().removeTicketWithRadius(
                        TicketType.PORTAL, ciTicketCenter, CI_CHUNK_RADIUS);
            }
        }
        activeServer = server;
        preparedLogged = false;
        completionLogged = false;
        ciPassLogged = false;
        ciTicketHeld = false;
        ciTicketCenter = null;
        SESSION_RESULTS.clear();
        FAILED_SCANS.clear();
    }

    private static void retainCitadelForCi(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site) {
        if (!ciMode()) return;
        ChunkPos requestedCenter = new ChunkPos(site.centerX() >> 4, site.centerZ() >> 4);
        if (ciTicketHeld && ciTicketCenter != null && !ciTicketCenter.equals(requestedCenter)) {
            releaseCiTicket(level);
        }
        ciTicketCenter = requestedCenter;
        // PORTAL tickets are transient. Refresh the same bounded ticket every zoning pass so a
        // long fresh-world audit cannot lose the final zone after the ticket timeout expires.
        level.getChunkSource().addTicketAndLoadWithRadius(
                TicketType.PORTAL, ciTicketCenter, CI_CHUNK_RADIUS);
        if (!ciTicketHeld) {
            LivingKingdoms.LOGGER.info(
                    "Retained Erden citadel for CI zoning audit centre_chunk={},{} radius={} transient_ticket=portal refreshed_until_verification=true forced_chunks=false synchronous_get_chunk=false",
                    ciTicketCenter.x(), ciTicketCenter.z(), CI_CHUNK_RADIUS);
        }
        ciTicketHeld = true;
    }

    private static void releaseCiTicket(ServerLevel level) {
        if (!ciTicketHeld || ciTicketCenter == null) return;
        level.getChunkSource().removeTicketWithRadius(
                TicketType.PORTAL, ciTicketCenter, CI_CHUNK_RADIUS);
        LivingKingdoms.LOGGER.info(
                "Released Erden citadel CI zoning ticket centre_chunk={},{} radius={} transient_ticket=portal",
                ciTicketCenter.x(), ciTicketCenter.z(), CI_CHUNK_RADIUS);
        ciTicketHeld = false;
        ciTicketCenter = null;
    }

    private static void logPreparedOnce(RealmSiteLayoutSavedData.RealmSite site) {
        if (preparedLogged) return;
        preparedLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden citadel functional zoning zones={} centre={},{} imported_architecture_preserved=true",
                ZONES.size(), site.centerX(), site.centerZ());
    }

    private static ZoneResult tryFurnishZone(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            Zone zone) {
        Anchor anchor = findInteriorAnchor(
                level,
                site,
                site.centerX() + zone.offsetX,
                site.centerZ() + zone.offsetZ);
        if (anchor == null) return null;

        Map<Long, Integer> furnishingFloors = collectConnectedFurnishingFloors(level, site, anchor);
        Set<Long> claimedFixtureCells = new HashSet<>();
        int fixtures = 0;
        for (Fixture fixture : zone.fixtures) {
            if (ensureFixture(level, site, anchor, fixture, furnishingFloors, claimedFixtureCells)) {
                fixtures++;
            }
        }
        return new ZoneResult(anchor, fixtures, furnishingFloors.size());
    }

    private static Anchor findInteriorAnchor(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            int preferredX,
            int preferredZ) {
        int minY = Math.max(level.getMinY() + 1, site.baseY() - 2);
        int maxY = Math.min(level.getMaxY() - 16, site.baseY() + 44);
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                Anchor top = findVerticalAnchor(level, site, preferredX + dx, preferredZ - radius, minY, maxY);
                if (top != null) return top;
                if (radius > 0) {
                    Anchor bottom = findVerticalAnchor(level, site, preferredX + dx, preferredZ + radius, minY, maxY);
                    if (bottom != null) return bottom;
                }
            }
            for (int dz = -radius + 1; dz < radius; dz++) {
                Anchor left = findVerticalAnchor(level, site, preferredX - radius, preferredZ + dz, minY, maxY);
                if (left != null) return left;
                if (radius > 0) {
                    Anchor right = findVerticalAnchor(level, site, preferredX + radius, preferredZ + dz, minY, maxY);
                    if (right != null) return right;
                }
            }
        }
        return null;
    }

    private static Anchor findVerticalAnchor(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            int x,
            int z,
            int minY,
            int maxY) {
        if (!insideCitadel(site, x, z) || !columnLoaded(level, x, z)) return null;
        for (int floorY = minY; floorY <= maxY; floorY++) {
            if (isEnclosedWalkableFloor(level, x, floorY, z)) return new Anchor(x, floorY, z);
        }
        return null;
    }

    private static Map<Long, Integer> collectConnectedFurnishingFloors(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            Anchor anchor) {
        Map<Long, Integer> floors = new HashMap<>();
        if (!isOpenFurnishingCell(level, site, anchor.x, anchor.floorY, anchor.z)) return floors;

        ArrayDeque<FloorNode> queue = new ArrayDeque<>();
        queue.addLast(new FloorNode(anchor.x, anchor.floorY, anchor.z));
        floors.put(furnishingKey(anchor.x, anchor.z), anchor.floorY);
        int[] verticalOffsets = {0, -1, 1};
        while (!queue.isEmpty()) {
            FloorNode node = queue.removeFirst();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int x = node.x + direction.getStepX();
                int z = node.z + direction.getStepZ();
                if (Math.max(Math.abs(x - anchor.x), Math.abs(z - anchor.z)) > FURNISHING_ROOM_RADIUS) {
                    continue;
                }
                long key = furnishingKey(x, z);
                if (floors.containsKey(key)) continue;
                for (int verticalOffset : verticalOffsets) {
                    int floorY = node.floorY + verticalOffset;
                    if (Math.abs(floorY - anchor.floorY) > 1) continue;
                    if (!isOpenFurnishingCell(level, site, x, floorY, z)) continue;
                    floors.put(key, floorY);
                    queue.addLast(new FloorNode(x, floorY, z));
                    break;
                }
            }
        }
        return floors;
    }

    private static boolean isOpenFurnishingCell(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            int x,
            int floorY,
            int z) {
        if (!insideCitadel(site, x, z) || !columnLoaded(level, x, z)) return false;
        BlockPos floor = new BlockPos(x, floorY, z);
        BlockPos feet = floor.above();
        BlockPos head = feet.above();
        return !level.getBlockState(floor).isAir()
                && level.getFluidState(floor).isEmpty()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(head).isAir()
                && level.getFluidState(feet).isEmpty()
                && level.getFluidState(head).isEmpty()
                && hasCeiling(level, x, floorY, z);
    }

    private static boolean ensureFixture(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            Anchor anchor,
            Fixture fixture,
            Map<Long, Integer> furnishingFloors,
            Set<Long> claimedFixtureCells) {
        int preferredX = anchor.x + fixture.dx;
        int preferredZ = anchor.z + fixture.dz;
        for (int radius = 0; radius <= FIXTURE_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = preferredX + dx;
                    int z = preferredZ + dz;
                    if (x == anchor.x && z == anchor.z) continue;
                    if (!insideCitadel(site, x, z) || !columnLoaded(level, x, z)) continue;
                    long fixtureKey = furnishingKey(x, z);
                    if (claimedFixtureCells.contains(fixtureKey)) continue;

                    Integer floorY = furnishingFloors.get(fixtureKey);
                    if (floorY == null) {
                        Integer existingFloorY = findConnectedExistingFixtureFloor(
                                level, site, anchor, fixture, x, z, furnishingFloors);
                        if (existingFloorY != null) {
                            claimedFixtureCells.add(fixtureKey);
                            return true;
                        }
                        continue;
                    }

                    BlockPos floor = new BlockPos(x, floorY, z);
                    BlockPos feet = floor.above();
                    BlockPos head = feet.above();
                    if (level.getBlockState(floor).isAir() || !level.getFluidState(floor).isEmpty()) continue;
                    if (!level.getBlockState(head).isAir()) continue;
                    Block current = level.getBlockState(feet).getBlock();
                    if (current == fixture.block) {
                        claimedFixtureCells.add(fixtureKey);
                        return true;
                    }
                    if (!level.getBlockState(feet).isAir()) continue;
                    level.setBlock(feet, fixture.block.defaultBlockState(), UPDATE_FLAGS);
                    claimedFixtureCells.add(fixtureKey);
                    return true;
                }
            }
        }
        return false;
    }

    private static Integer findConnectedExistingFixtureFloor(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            Anchor anchor,
            Fixture fixture,
            int x,
            int z,
            Map<Long, Integer> furnishingFloors) {
        for (int floorY = anchor.floorY - 1; floorY <= anchor.floorY + 1; floorY++) {
            if (!insideCitadel(site, x, z) || !columnLoaded(level, x, z)) return null;
            BlockPos floor = new BlockPos(x, floorY, z);
            BlockPos feet = floor.above();
            BlockPos head = feet.above();
            if (level.getBlockState(feet).getBlock() != fixture.block) continue;
            if (level.getBlockState(floor).isAir() || !level.getFluidState(floor).isEmpty()) continue;
            if (!level.getBlockState(head).isAir() || !hasCeiling(level, x, floorY, z)) continue;
            if (touchesConnectedFurnishingCell(x, floorY, z, furnishingFloors)) return floorY;
        }
        return null;
    }

    private static boolean touchesConnectedFurnishingCell(
            int x,
            int floorY,
            int z,
            Map<Long, Integer> furnishingFloors) {
        Integer ownFloor = furnishingFloors.get(furnishingKey(x, z));
        if (ownFloor != null && Math.abs(ownFloor - floorY) <= 1) return true;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Integer adjacentFloor = furnishingFloors.get(furnishingKey(
                    x + direction.getStepX(), z + direction.getStepZ()));
            if (adjacentFloor != null && Math.abs(adjacentFloor - floorY) <= 1) return true;
        }
        return false;
    }

    private static boolean isEnclosedWalkableFloor(ServerLevel level, int x, int floorY, int z) {
        if (!columnLoaded(level, x, z)) return false;
        BlockPos floor = new BlockPos(x, floorY, z);
        if (level.getBlockState(floor).isAir() || !level.getFluidState(floor).isEmpty()) return false;
        if (!level.getBlockState(floor.above()).isAir()
                || !level.getBlockState(floor.above(2)).isAir()) return false;
        return hasCeiling(level, x, floorY, z)
                && enclosingWallCount(level, x, floorY + 1, z) >= 3;
    }

    private static boolean hasCeiling(ServerLevel level, int x, int floorY, int z) {
        if (!columnLoaded(level, x, z)) return false;
        for (int y = floorY + 3; y <= floorY + MAX_ROOM_HEIGHT; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty()) return true;
        }
        return false;
    }

    private static int enclosingWallCount(ServerLevel level, int x, int y, int z) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= MAX_WALL_DISTANCE; distance++) {
                int checkX = x + direction.getStepX() * distance;
                int checkZ = z + direction.getStepZ() * distance;
                if (!columnLoaded(level, checkX, checkZ)) break;
                BlockPos pos = new BlockPos(checkX, y, checkZ);
                if (!level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty()) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static void logFailedScanIfUseful(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            Zone zone,
            ZoneResult result) {
        if (!ciMode()) return;
        int attempts = FAILED_SCANS.merge(zone.id, 1, Integer::sum);
        if (attempts != 5 && attempts % 20 != 0) return;
        int preferredX = site.centerX() + zone.offsetX;
        int preferredZ = site.centerZ() + zone.offsetZ;
        int loadedColumns = 0;
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx += 8) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz += 8) {
                if (columnLoaded(level, preferredX + dx, preferredZ + dz)) loadedColumns++;
            }
        }
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_CITADEL_ZONE_WAIT zone={} attempts={} loaded_probe_columns={} anchor_found={} fixtures={} connected_cells={} required={}",
                zone.id, attempts, loadedColumns, result != null,
                result == null ? 0 : result.fixtures,
                result == null ? 0 : result.connectedCells,
                requiredFixtures(zone));
    }

    private static void verifyCiIfReady(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            int completed) {
        if (ciPassLogged || completed != ZONES.size() || !ciMode()) return;
        if (SESSION_RESULTS.size() != ZONES.size()) return;

        ZoneResult audience = SESSION_RESULTS.get("audience_hall");
        if (audience == null) return;
        Traversal traversal = verifyAudienceTraversal(level, site, audience.anchor);
        if (!traversal.reachable) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_CITADEL_TRAVERSAL_WAIT door_blocks={} perimeter_starts={} walk_nodes={} audience_reachable=false",
                    traversal.doorBlocks, traversal.perimeterStarts, traversal.visitedNodes);
            return;
        }

        int fixtureCount = SESSION_RESULTS.values().stream().mapToInt(result -> result.fixtures).sum();
        ciPassLogged = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_CITADEL_INTERIOR_PASS zones={} fixtures={} audience_reachable=true door_blocks={} perimeter_starts={} walk_nodes={} enclosed=true furnishing_connected=true stepped_furnishing=true unique_fixture_cells=true facade_replaced=false loaded_only=true",
                ZONES.size(), fixtureCount, traversal.doorBlocks, traversal.perimeterStarts,
                traversal.visitedNodes);
        releaseCiTicket(level);
    }

    private static Traversal verifyAudienceTraversal(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            Anchor audience) {
        List<WalkNode> starts = new ArrayList<>();
        int doorBlocks = 0;
        int minY = Math.max(level.getMinY() + 1, site.baseY() - 2);
        int maxY = Math.min(level.getMaxY() - 2, site.baseY() + 36);
        int minX = site.centerX() - CITADEL_HALF_SIZE;
        int maxX = site.centerX() + CITADEL_HALF_SIZE - 1;
        int minZ = site.centerZ() - CITADEL_HALF_SIZE;
        int maxZ = site.centerZ() + CITADEL_HALF_SIZE - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!columnLoaded(level, x, z)) continue;
                for (int y = minY; y <= maxY; y++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof DoorBlock) {
                        doorBlocks++;
                    }
                }
            }
        }

        for (int x = minX; x <= maxX; x++) {
            addPerimeterStarts(level, site, starts, x, minZ, minY, maxY);
            addPerimeterStarts(level, site, starts, x, maxZ, minY, maxY);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            addPerimeterStarts(level, site, starts, minX, z, minY, maxY);
            addPerimeterStarts(level, site, starts, maxX, z, minY, maxY);
        }
        if (starts.isEmpty()) return new Traversal(false, doorBlocks, 0, 0);

        WalkNode target = new WalkNode(audience.x, audience.floorY + 1, audience.z);
        ArrayDeque<WalkNode> queue = new ArrayDeque<>(starts);
        Set<Long> visited = new HashSet<>();
        for (WalkNode start : starts) visited.add(walkKey(start));
        int visitedNodes = 0;
        while (!queue.isEmpty() && visitedNodes < MAX_WALK_NODES) {
            WalkNode node = queue.removeFirst();
            visitedNodes++;
            if (node.equals(target)) return new Traversal(true, doorBlocks, starts.size(), visitedNodes);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int nx = node.x + direction.getStepX();
                int nz = node.z + direction.getStepZ();
                for (int ny = node.y - 1; ny <= node.y + 1; ny++) {
                    if (!isWalkableFeet(level, site, nx, ny, nz)) continue;
                    WalkNode next = new WalkNode(nx, ny, nz);
                    long key = walkKey(next);
                    if (visited.add(key)) queue.addLast(next);
                }
            }
        }
        return new Traversal(false, doorBlocks, starts.size(), visitedNodes);
    }

    private static void addPerimeterStarts(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            List<WalkNode> starts,
            int x,
            int z,
            int minY,
            int maxY) {
        if (!columnLoaded(level, x, z)) return;
        for (int y = minY; y <= maxY; y++) {
            if (isWalkableFeet(level, site, x, y, z)) {
                starts.add(new WalkNode(x, y, z));
            }
        }
    }

    private static boolean isWalkableFeet(
            ServerLevel level,
            RealmSiteLayoutSavedData.RealmSite site,
            int x,
            int feetY,
            int z) {
        if (!insideCitadel(site, x, z) || !columnLoaded(level, x, z)) return false;
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
        return level.getBlockState(pos).isAir()
                || level.getBlockState(pos).getBlock() instanceof DoorBlock;
    }

    private static boolean insideCitadel(
            RealmSiteLayoutSavedData.RealmSite site,
            int x,
            int z) {
        return x >= site.centerX() - CITADEL_HALF_SIZE
                && x < site.centerX() + CITADEL_HALF_SIZE
                && z >= site.centerZ() - CITADEL_HALF_SIZE
                && z < site.centerZ() + CITADEL_HALF_SIZE;
    }

    private static boolean columnLoaded(ServerLevel level, int x, int z) {
        return level.hasChunk(x >> 4, z >> 4);
    }

    private static boolean ciMode() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private static long walkKey(WalkNode node) {
        long x = node.x & 0x3ffffffL;
        long z = node.z & 0x3ffffffL;
        long y = node.y & 0xfffL;
        return (x << 38) | (z << 12) | y;
    }

    private static long furnishingKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static List<Fixture> civic(Block a, Block b, Block c, Block d, Block e, Block f) {
        return List.of(
                new Fixture(-2, -1, a), new Fixture(2, -1, b),
                new Fixture(-2, 1, c), new Fixture(2, 1, d),
                new Fixture(-2, 3, e), new Fixture(2, 3, f));
    }

    private record Zone(String id, int offsetX, int offsetZ, List<Fixture> fixtures) {
    }

    private record Fixture(int dx, int dz, Block block) {
    }

    private record Anchor(int x, int floorY, int z) {
    }

    private record ZoneResult(Anchor anchor, int fixtures, int connectedCells) {
    }

    private record FloorNode(int x, int floorY, int z) {
    }

    private record WalkNode(int x, int y, int z) {
    }

    private record Traversal(boolean reachable, int doorBlocks, int perimeterStarts, int visitedNodes) {
    }
}
