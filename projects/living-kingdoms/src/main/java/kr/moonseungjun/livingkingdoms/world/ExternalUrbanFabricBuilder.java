package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fills the gaps between large capital anchors with entrance-centred facade fragments cut from
 * attributed external buildings. The result is streamed per chunk, keeps the original block states,
 * and connects every retained entrance to a planned street through the actual walkable facade exit.
 */
public final class ExternalUrbanFabricBuilder {
    private static final String MANOR =
            "/data/livingkingdoms/structures/external/medieval_manor.schem";
    private static final String HOUSE =
            "/data/livingkingdoms/structures/external/all_in_one_house.schem";
    private static final String CASTLE_HOUSE =
            "/data/livingkingdoms/structures/external/fantasy_castle_house.schem";
    private static final String PLAYER_CASTLE_HOUSE =
            "/data/livingkingdoms/structures/external/player_castle_house.schem";
    private static final String TAVERN_INN =
            "/data/livingkingdoms/structures/external/medieval_tavern_inn.schem";
    private static final String MARKET_HALL =
            "/data/livingkingdoms/structures/external/medieval_market_hall.schem";
    private static final String HORSE_STABLE =
            "/data/livingkingdoms/structures/external/medieval_horse_stable.schem";
    private static final List<String> URBAN_RESOURCES = List.of(
            HOUSE, CASTLE_HOUSE, MANOR,
            PLAYER_CASTLE_HOUSE, TAVERN_INN, MARKET_HALL, HORSE_STABLE);
    private static final Set<String> INDEPENDENT_URBAN_SOURCES = Set.of(
            PLAYER_CASTLE_HOUSE, TAVERN_INN, MARKET_HALL, HORSE_STABLE);

    private static final int MIN_COMPONENT_BLOCKS = 24;
    private static final int MAX_ROAD_SEARCH = 72;
    private static final int ACCESS_HALF_WIDTH = 1;
    private static final int SOURCE_EXIT_PROBE = 4;
    private static final int MIN_INDEPENDENT_SOURCE_REACHABLE = 160;
    private static final Map<String, SourceTemplate> TEMPLATE_CACHE = new HashMap<>();
    private static final Map<String, String> LEGACY_BLOCK_IDS = Map.of(
            "minecraft:chain", "minecraft:iron_chain",
            "minecraft:grass", "minecraft:short_grass"
    );
    private static final Set<String> SKIPPED_TERRAIN = Set.of(
            "minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void",
            "minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
            "minecraft:podzol", "minecraft:mycelium", "minecraft:moss_block", "minecraft:moss_carpet",
            "minecraft:farmland", "minecraft:dirt_path", "minecraft:mud", "minecraft:packed_mud",
            "minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:clay",
            "minecraft:water", "minecraft:lava", "minecraft:snow", "minecraft:snow_block"
    );
    private static final Set<String> SKIPPED_FLORA = Set.of(
            "minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern",
            "minecraft:dead_bush", "minecraft:rose_bush", "minecraft:peony", "minecraft:lilac",
            "minecraft:sunflower", "minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid",
            "minecraft:allium", "minecraft:azure_bluet", "minecraft:oxeye_daisy", "minecraft:cornflower",
            "minecraft:lily_of_the_valley", "minecraft:wither_rose", "minecraft:pink_petals",
            "minecraft:azalea", "minecraft:flowering_azalea", "minecraft:vine", "minecraft:lily_pad",
            "minecraft:sugar_cane", "minecraft:bamboo", "minecraft:cactus", "minecraft:wheat",
            "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots", "minecraft:pumpkin_stem",
            "minecraft:melon_stem", "minecraft:sweet_berry_bush", "minecraft:cocoa"
    );

    private static final List<Exclusion> EXCLUSIONS = List.of(
            new Exclusion(0, 0, 150, 150),
            new Exclusion(-390, -520, 90, 82), new Exclusion(390, -520, 90, 82),
            new Exclusion(-700, -610, 94, 90), new Exclusion(-700, -350, 94, 90),
            new Exclusion(710, -560, 110, 98), new Exclusion(720, -270, 110, 98),
            new Exclusion(-720, 540, 82, 82), new Exclusion(-400, 610, 82, 82),
            new Exclusion(760, 590, 82, 82), new Exclusion(360, 300, 92, 82),
            new Exclusion(650, 220, 92, 82), new Exclusion(620, 520, 92, 82),
            new Exclusion(-170, 600, 92, 82), new Exclusion(170, 600, 92, 82),
            new Exclusion(-990, -420, 92, 82), new Exclusion(-980, 250, 92, 82),

            new Exclusion(-1020, -720, 88, 80), new Exclusion(-820, -720, 82, 80),
            new Exclusion(-180, -720, 88, 80), new Exclusion(180, -720, 88, 80),
            new Exclusion(820, -720, 82, 80), new Exclusion(1020, -720, 88, 80),
            new Exclusion(-1020, -180, 88, 80), new Exclusion(-780, -180, 94, 86),
            new Exclusion(-360, -180, 88, 80), new Exclusion(360, -180, 88, 80),
            new Exclusion(780, -180, 94, 86), new Exclusion(1020, -180, 88, 80),
            new Exclusion(-1020, 180, 88, 80), new Exclusion(-780, 180, 94, 86),
            new Exclusion(-360, 180, 88, 80), new Exclusion(360, 180, 88, 80),
            new Exclusion(780, 180, 94, 86), new Exclusion(1020, 180, 88, 80),
            new Exclusion(-1020, 720, 88, 80), new Exclusion(-820, 720, 82, 80),
            new Exclusion(-180, 720, 88, 80), new Exclusion(180, 720, 88, 80),
            new Exclusion(820, 720, 82, 80), new Exclusion(1020, 720, 88, 80)
    );

    private static volatile List<UrbanPlacement> cachedPlacements;
    private static volatile List<UrbanEntrance> cachedEntrances;
    private static volatile Map<UrbanRole, Integer> cachedRoleCounts;

    private ExternalUrbanFabricBuilder() {
    }

    public static void addChunk(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {
        for (UrbanPlacement placement : placements()) {
            if (!placement.intersects(chunk)) continue;
            pasteClipped(plan, level, chunk, placement);
            addAccessPath(plan, level, chunk, placement.entrance);
        }
    }

    public static int plotCount() {
        return placements().size();
    }

    public static int facadeStyleCount() {
        int count = 0;
        for (String resource : URBAN_RESOURCES) {
            count += template(resource).fragments.size();
        }
        return count;
    }

    public static int roleCount(String roleId) {
        UrbanRole role = UrbanRole.fromId(roleId);
        return roleCounts().getOrDefault(role, 0);
    }

    public static Map<String, Integer> roleCountsForDiagnostics() {
        Map<String, Integer> result = new LinkedHashMap<>();
        roleCounts().forEach((role, count) -> result.put(role.id, count));
        return Map.copyOf(result);
    }

    public static List<UrbanEntrance> entrances() {
        List<UrbanEntrance> result = cachedEntrances;
        if (result != null) return result;
        placements();
        return cachedEntrances;
    }

    public static UrbanEntrance diagnosticEntrance() {
        return entrances().stream()
                .filter(entrance -> entrance.role.equals(UrbanRole.BAKERY.id))
                .findFirst()
                .orElseGet(() -> entrances().getFirst());
    }

    /**
     * Read-only placement metadata for source-native interior classification. This is derived from
     * the exact retained placement objects that construction uses, so diagnostics do not have to
     * reproduce the placement algorithm and cannot silently drift from the fixed 233 functional lots.
     */
    public static List<UrbanBuildingPlacement> buildingPlacementsForDiagnostics() {
        List<UrbanBuildingPlacement> result = new ArrayList<>();
        for (UrbanPlacement placement : placements()) {
            int width = placement.rotatedWidth();
            int length = placement.rotatedLength();
            int minX = placement.centerX - width / 2;
            int minZ = placement.centerZ - length / 2;
            int maxX = minX + width - 1;
            int maxZ = minZ + length - 1;
            int baseY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(
                    placement.centerX, placement.centerZ));
            result.add(new UrbanBuildingPlacement(
                    placement.role.id,
                    placement.resource,
                    fragmentKey(placement.resource, placement.fragment),
                    placement.rotation,
                    placement.entrance,
                    minX, maxX, minZ, maxZ,
                    baseY, placement.fragment.height,
                    width, length));
        }
        return List.copyOf(result);
    }

    /** Returns each retained cropped fragment once; block states are immutable and source-only. */
    public static Map<String, UrbanFragmentSnapshot> fragmentSnapshotsForDiagnostics() {
        Map<String, UrbanFragmentSnapshot> result = new LinkedHashMap<>();
        for (String resource : URBAN_RESOURCES) {
            SourceTemplate source = template(resource);
            for (int index = 0; index < source.fragments.size(); index++) {
                FacadeFragment fragment = source.fragments.get(index);
                List<UrbanSourceBlock> blocks = fragment.blocks.stream()
                        .map(block -> new UrbanSourceBlock(
                                block.x, block.y, block.z, block.state))
                        .toList();
                result.put(fragmentKey(resource, index), new UrbanFragmentSnapshot(
                        fragmentKey(resource, index), resource,
                        fragment.width, fragment.height, fragment.length,
                        fragment.entranceX, fragment.entranceZ,
                        fragment.exteriorSide.name(), List.copyOf(blocks)));
            }
        }
        return Map.copyOf(result);
    }

    /** Source-only audit of fixed-size crop direction candidates. Never mutates placements or world blocks. */
    public static void auditFixedFootprintCropCandidates() {
        SourceTemplate source = template(CASTLE_HOUSE);
        List<BuildingBlock> doors = findEntranceBlocks(source.blocks, source.width, source.length);
        int currentReachable = source.fragments.isEmpty()
                ? 0 : fragmentReachableCells(source.fragments.getFirst());
        int bestReachable = 0;
        FrontSide bestSide = null;
        int bestDoorX = -1;
        int bestDoorZ = -1;
        int candidates = 0;
        for (BuildingBlock door : doors) {
            for (FrontSide side : FrontSide.values()) {
                int depth = Math.min(38, side.horizontal ? source.width : source.length);
                if (edgeDistance(door.x, door.z, source.width, source.length, side) >= depth) continue;
                FacadeFragment fragment = withExteriorSide(
                        cropFragment(source.blocks, door, side,
                                source.width, source.height, source.length), side);
                if (!containsRealEntrance(fragment)) continue;
                int reachable = fragmentReachableCells(fragment);
                candidates++;
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_CROP_WINDOW_CANDIDATE resource={} door={},{} crop_side={} fragment={}x{} entrance_local={},{} resolved_side={} reachable={} source_only=true world_reads=false mutations=0",
                        CASTLE_HOUSE, door.x, door.z, side, fragment.width, fragment.length,
                        fragment.entranceX, fragment.entranceZ, fragment.exteriorSide, reachable);
                if (reachable > bestReachable) {
                    bestReachable = reachable;
                    bestSide = side;
                    bestDoorX = door.x;
                    bestDoorZ = door.z;
                }
            }
        }
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_CROP_WINDOW_AUDIT resource={} candidates={} current_reachable={} best_reachable={} best_side={} best_door={},{} same_footprint=true source_only=true world_reads=false mutations=0",
                CASTLE_HOUSE, candidates, currentReachable, bestReachable, bestSide, bestDoorX, bestDoorZ);
    }

    private static boolean containsRealEntrance(FacadeFragment fragment) {
        int y = lowestDoorY(fragmentBlockMap(fragment), fragment.entranceX, 64, fragment.entranceZ);
        if (y >= 64) {
            for (int testY = 0; testY < fragment.height; testY++) {
                BuildingBlock b = fragmentBlockMap(fragment).get(localKey(fragment.entranceX, testY, fragment.entranceZ));
                if (b != null && b.state.getBlock() instanceof DoorBlock) return true;
            }
            return false;
        }
        return true;
    }

    private static Map<Long, BuildingBlock> fragmentBlockMap(FacadeFragment fragment) {
        Map<Long, BuildingBlock> map = new HashMap<>();
        for (BuildingBlock block : fragment.blocks) map.put(localKey(block.x, block.y, block.z), block);
        return map;
    }

    /**
     * Proves that a resolved facade side is a real exit rather than a fallback direction. A door
     * sitting directly on the retained crop edge is already an exterior opening; otherwise at least
     * one supported walkable source step must exist on the resolved exterior side. Cut-face seals are
     * present in the candidate block map, so a sealed synthetic boundary cannot satisfy this proof.
     */
    private static int fragmentExteriorExitProof(FacadeFragment fragment) {
        Map<Long, BuildingBlock> blocks = fragmentBlockMap(fragment);
        int doorY = Integer.MAX_VALUE;
        for (int y = 0; y < fragment.height; y++) {
            BuildingBlock block = blocks.get(localKey(fragment.entranceX, y, fragment.entranceZ));
            if (block != null && block.state.getBlock() instanceof DoorBlock) {
                doorY = y;
                break;
            }
        }
        if (doorY == Integer.MAX_VALUE) return 0;
        int clearRun = sourceClearRun(
                blocks, fragment.entranceX, doorY, fragment.entranceZ, fragment.exteriorSide);
        if (clearRun > 0) return clearRun;
        return edgeDistance(
                fragment.entranceX, fragment.entranceZ,
                fragment.width, fragment.length, fragment.exteriorSide) <= 1 ? 1 : 0;
    }

    private static int fragmentReachableCells(FacadeFragment fragment) {
        Map<Long, BuildingBlock> blocks = fragmentBlockMap(fragment);
        int doorY = Integer.MAX_VALUE;
        for (int y = 0; y < fragment.height; y++) {
            BuildingBlock block = blocks.get(localKey(fragment.entranceX, y, fragment.entranceZ));
            if (block != null && block.state.getBlock() instanceof DoorBlock) { doorY = y; break; }
        }
        if (doorY == Integer.MAX_VALUE) return 0;
        record Walk(int x, int y, int z) {}
        java.util.ArrayDeque<Walk> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<Long> visited = new java.util.HashSet<>();
        int seedY = sourceWalkableFeetY(blocks, fragment.entranceX, fragment.entranceZ, doorY);
        if (seedY == Integer.MIN_VALUE) return 0;
        queue.add(new Walk(fragment.entranceX, seedY, fragment.entranceZ));
        visited.add(walkKey(fragment.entranceX, seedY, fragment.entranceZ));
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()) {
            Walk current = queue.removeFirst();
            for (int[] d : dirs) {
                int nx=current.x()+d[0], nz=current.z()+d[1];
                if (nx<0 || nx>=fragment.width || nz<0 || nz>=fragment.length) continue;
                if (!fragmentInteriorSide(fragment, nx, nz)) continue;
                int ny=sourceWalkableFeetY(blocks,nx,nz,current.y());
                if (ny==Integer.MIN_VALUE || Math.abs(ny-current.y())>1) continue;
                long key=walkKey(nx,ny,nz);
                if (visited.add(key)) queue.addLast(new Walk(nx,ny,nz));
            }
        }
        return visited.size();
    }

    private static boolean fragmentInteriorSide(FacadeFragment fragment, int x, int z) {
        return switch (fragment.exteriorSide) {
            case NORTH -> z >= fragment.entranceZ;
            case SOUTH -> z <= fragment.entranceZ;
            case WEST -> x >= fragment.entranceX;
            case EAST -> x <= fragment.entranceX;
        };
    }

    private static FacadeFragment withExteriorSide(FacadeFragment fragment, FrontSide exteriorSide) {
        if (fragment.exteriorSide == exteriorSide) return fragment;
        return new FacadeFragment(fragment.width, fragment.height, fragment.length,
                fragment.blocks, fragment.entranceX, fragment.entranceZ, exteriorSide);
    }

    private static long walkKey(int x, int y, int z) {
        return ((long)(x & 0xffff) << 48) ^ ((long)(y & 0xffff) << 32) ^ (z & 0xffffffffL);
    }

    private static String fragmentKey(String resource, FacadeFragment fragment) {
        SourceTemplate source = template(resource);
        for (int index = 0; index < source.fragments.size(); index++) {
            if (source.fragments.get(index) == fragment) return fragmentKey(resource, index);
        }
        throw new IllegalStateException("Placed Erden facade fragment is not retained by its source: "
                + resource);
    }

    private static String fragmentKey(String resource, int index) {
        return resource + "#" + index;
    }

    private static List<UrbanPlacement> placements() {
        List<UrbanPlacement> result = cachedPlacements;
        if (result != null) return result;
        synchronized (ExternalUrbanFabricBuilder.class) {
            result = cachedPlacements;
            if (result == null) {
                result = List.copyOf(createPlacements());
                cachedPlacements = result;
                List<UrbanEntrance> entrances = result.stream()
                        .map(UrbanPlacement::entrance)
                        .toList();
                cachedEntrances = List.copyOf(entrances);
                EnumMap<UrbanRole, Integer> counts = new EnumMap<>(UrbanRole.class);
                Map<String, Integer> resources = new LinkedHashMap<>();
                for (UrbanPlacement placement : result) {
                    counts.merge(placement.role, 1, Integer::sum);
                    resources.merge(placement.resource, 1, Integer::sum);
                }
                cachedRoleCounts = Map.copyOf(counts);
                LivingKingdoms.LOGGER.info(
                        "Prepared Erden continuous urban fabric plots={} entrances={} facade_styles={} roles={} resources={}",
                        result.size(), entrances.size(), facadeStyleCount(), roleCountsForDiagnostics(), resources
                );
            }
            return result;
        }
    }

    private static Map<UrbanRole, Integer> roleCounts() {
        placements();
        return cachedRoleCounts;
    }

    private static List<UrbanPlacement> createPlacements() {
        List<UrbanPlacement> result = new ArrayList<>();
        addBand(result, -1_145, 1_145, -850, -625, 48, 46, 0x31A9D3);
        addBand(result, -1_145, 1_145, -465, -255, 52, 48, 0x4D17B1);
        addBand(result, -1_145, 1_145, 255, 465, 52, 48, 0x62C59F);
        addBand(result, -1_145, 1_145, 625, 850, 48, 46, 0x7B2E11);
        addBand(result, -1_145, -255, -115, 115, 50, 48, 0x1585AF);
        addBand(result, 255, 1_145, -115, 115, 50, 48, 0x2F993D);

        if (result.size() < 180) {
            throw new IllegalStateException("Continuous Erden urban fabric is too sparse: "
                    + result.size());
        }
        return result;
    }

    private static void addBand(List<UrbanPlacement> result,
                                int minX, int maxX, int minZ, int maxZ,
                                int stepX, int stepZ, int salt) {
        int row = 0;
        for (int z = minZ; z <= maxZ; z += stepZ) {
            int shift = (row++ & 1) == 0 ? 0 : stepX / 2;
            for (int x = minX + shift; x <= maxX; x += stepX) {
                int hash = mix(x, z, salt);
                UrbanRole role = chooseRole(x, z, hash);
                String resource = chooseResource(role, hash);
                SourceTemplate source = template(resource);
                FacadeFragment fragment = source.fragments.get(
                        Math.floorMod(hash >>> 4, source.fragments.size()));
                UrbanPlacement placement = choosePlacement(
                        x, z, role, resource, fragment, hash);
                if (placement != null) result.add(placement);
            }
        }
    }

    private static UrbanRole chooseRole(int x, int z, int hash) {
        int selector = Math.floorMod(hash, 100);
        boolean marketBelt = Math.abs(z) <= 130 || Math.abs(x) >= 850;
        if (marketBelt) {
            if (selector < 30) return UrbanRole.SHOP;
            if (selector < 45) return UrbanRole.BAKERY;
            if (selector < 59) return UrbanRole.INN;
            if (selector < 72) return UrbanRole.WAREHOUSE;
            if (selector < 82) return UrbanRole.BATHHOUSE;
            if (selector < 91) return UrbanRole.STABLE;
            return UrbanRole.GUARD_POST;
        }
        if (selector < 52) return UrbanRole.TENEMENT;
        if (selector < 65) return UrbanRole.SHOP;
        if (selector < 75) return UrbanRole.BAKERY;
        if (selector < 84) return UrbanRole.INN;
        if (selector < 91) return UrbanRole.STABLE;
        if (selector < 96) return UrbanRole.BATHHOUSE;
        return UrbanRole.GUARD_POST;
    }

    private static String chooseResource(UrbanRole role, int hash) {
        return switch (role) {
            case TENEMENT -> selectResource(hash, HOUSE, CASTLE_HOUSE, PLAYER_CASTLE_HOUSE);
            case SHOP -> selectResource(hash, HOUSE, MARKET_HALL, PLAYER_CASTLE_HOUSE);
            case BAKERY -> selectResource(hash, HOUSE, PLAYER_CASTLE_HOUSE);
            case INN -> selectResource(hash, MANOR, CASTLE_HOUSE, TAVERN_INN);
            case STABLE -> selectResource(hash, MANOR, HORSE_STABLE);
            case GUARD_POST -> selectResource(hash, MANOR, CASTLE_HOUSE, PLAYER_CASTLE_HOUSE);
            case BATHHOUSE -> MANOR;
            case WAREHOUSE -> selectResource(hash, HOUSE, MARKET_HALL);
        };
    }

    private static String selectResource(int hash, String... resources) {
        return resources[Math.floorMod(hash >>> 5, resources.length)];
    }

    private static UrbanPlacement choosePlacement(int centerX, int centerZ,
                                                  UrbanRole role, String resource,
                                                  FacadeFragment fragment, int hash) {
        Rotation[] rotations = Rotation.values();
        UrbanPlacement best = null;
        int bestScore = Integer.MAX_VALUE;
        int start = Math.floorMod(hash >>> 9, rotations.length);
        for (int offset = 0; offset < rotations.length; offset++) {
            Rotation rotation = rotations[(start + offset) % rotations.length];
            int width = rotatedWidth(fragment, rotation);
            int length = rotatedLength(fragment, rotation);
            int minX = centerX - width / 2;
            int minZ = centerZ - length / 2;
            int maxX = minX + width - 1;
            int maxZ = minZ + length - 1;
            if (!insideCapital(minX, minZ, maxX, maxZ)) continue;
            if (overlapsExclusion(minX, minZ, maxX, maxZ)) continue;
            if (overlapsRoad(minX, minZ, maxX, maxZ)) continue;
            if (overlapsServiceNode(minX, minZ, maxX, maxZ)) continue;

            RotatedOffset entranceOffset = rotate(
                    fragment.entranceX, fragment.entranceZ,
                    fragment.width, fragment.length, rotation);
            int entranceX = minX + entranceOffset.x;
            int entranceZ = minZ + entranceOffset.z;

            // Preserve the legacy nearest-road score so the chosen rotations and the fixed 233
            // functional plots remain deterministic. The retained road target, however, follows
            // the side of the imported doorway that the source structure itself proves walkable.
            RoadTarget nearest = nearestRoad(
                    entranceX, entranceZ, minX, minZ, maxX, maxZ);
            if (nearest == null) continue;
            int distance = Math.abs(nearest.x - entranceX) + Math.abs(nearest.z - entranceZ);
            int score = distance * 10 + Math.floorMod(hash + offset * 17, 9);
            if (score < bestScore) {
                FrontSide front = rotateFrontSide(fragment.exteriorSide, rotation);
                RoadTarget frontRoad = nearestRoadOnSide(
                        entranceX, entranceZ, minX, minZ, maxX, maxZ, front);
                RoadTarget retained = frontRoad == null ? nearest : frontRoad;
                UrbanEntrance entrance = new UrbanEntrance(
                        role.id, entranceX, entranceZ, retained.x, retained.z);
                best = new UrbanPlacement(
                        resource, centerX, centerZ, rotation, role, fragment, entrance);
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean insideCapital(int minX, int minZ, int maxX, int maxZ) {
        return minX > ErdenCapitalStreamingBuilder.WEST_WALL_X + 18
                && maxX < ErdenCapitalStreamingBuilder.EAST_WALL_X - 18
                && minZ > ErdenCapitalStreamingBuilder.NORTH_WALL_Z + 18
                && maxZ < ErdenCapitalStreamingBuilder.SOUTH_WALL_Z - 18;
    }

    private static boolean overlapsExclusion(int minX, int minZ, int maxX, int maxZ) {
        for (Exclusion exclusion : EXCLUSIONS) {
            if (maxX >= exclusion.centerX - exclusion.halfWidth
                    && minX <= exclusion.centerX + exclusion.halfWidth
                    && maxZ >= exclusion.centerZ - exclusion.halfLength
                    && minZ <= exclusion.centerZ + exclusion.halfLength) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlapsRoad(int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX; x <= maxX; x += 2) {
            for (int z = minZ; z <= maxZ; z += 2) {
                if (ErdenCapitalStreamingBuilder.roadClassAt(x, z)
                        != ErdenCapitalStreamingBuilder.RoadClass.NONE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean overlapsServiceNode(int minX, int minZ, int maxX, int maxZ) {
        int[][] nodes = {
                {-280, -280}, {280, -280}, {-280, 280}, {280, 280},
                {-880, -280}, {880, -280}, {-880, 280}, {880, 280},
                {-280, -680}, {280, -680}, {-280, 680}, {280, 680},
                {-580, -280}, {580, -280}, {-580, 280}, {580, 280},
                {-1080, -280}, {1080, -280}, {-1080, 280}, {1080, 280}
        };
        for (int[] node : nodes) {
            if (node[0] >= minX - 8 && node[0] <= maxX + 8
                    && node[1] >= minZ - 8 && node[1] <= maxZ + 8) {
                return true;
            }
        }
        return false;
    }

    private static void pasteClipped(IncrementalWorldEditPlan plan, ServerLevel level,
                                     ChunkPos chunk, UrbanPlacement placement) {
        FacadeFragment fragment = placement.fragment;
        int rotatedWidth = rotatedWidth(fragment, placement.rotation);
        int rotatedLength = rotatedLength(fragment, placement.rotation);
        int originX = placement.centerX - rotatedWidth / 2;
        int originZ = placement.centerZ - rotatedLength / 2;
        int baseY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(
                placement.centerX, placement.centerZ));
        int minChunkX = chunk.getMinBlockX();
        int maxChunkX = minChunkX + 15;
        int minChunkZ = chunk.getMinBlockZ();
        int maxChunkZ = minChunkZ + 15;

        Map<Long, VerticalSpan> spans = new LinkedHashMap<>();
        List<PlacedBlock> placed = new ArrayList<>();
        for (BuildingBlock block : fragment.blocks) {
            RotatedOffset offset = rotate(
                    block.x, block.z, fragment.width, fragment.length, placement.rotation);
            int x = originX + offset.x;
            int z = originZ + offset.z;
            if (x < minChunkX || x > maxChunkX || z < minChunkZ || z > maxChunkZ) continue;
            int y = baseY + block.y;
            spans.merge(columnKey(x, z),
                    new VerticalSpan(x, z, y, y), VerticalSpan::merge);
            placed.add(new PlacedBlock(
                    x, y, z, block.state.rotate(placement.rotation)));
        }
        if (placed.isEmpty()) return;

        for (VerticalSpan span : spans.values()) {
            int surfaceY = RealmSitePlanner.surfaceY(level, span.x, span.z);
            plan.addFill(span.x, span.minY, span.z,
                    span.x, Math.max(surfaceY, span.maxY + 2), span.z, Blocks.AIR);
            if (surfaceY < span.minY - 1) {
                plan.addFill(span.x, surfaceY + 1, span.z,
                        span.x, span.minY - 1, span.z, Blocks.STONE_BRICKS);
            }
            plan.addSet(span.x, span.minY - 1, span.z, Blocks.STONE_BRICKS);
        }
        for (PlacedBlock block : placed) {
            plan.addSet(block.x, block.y, block.z, block.state);
        }
    }

    private static void addAccessPath(IncrementalWorldEditPlan plan, ServerLevel level,
                                      ChunkPos chunk, UrbanEntrance entrance) {
        if (!segmentIntersects(
                chunk, entrance.x, entrance.z, entrance.roadX, entrance.roadZ, 2)) {
            return;
        }
        int deltaX = entrance.roadX - entrance.x;
        int deltaZ = entrance.roadZ - entrance.z;
        int steps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        if (steps == 0) return;
        boolean eastWest = Math.abs(deltaX) >= Math.abs(deltaZ);
        for (int step = 0; step <= steps; step++) {
            int centerX = entrance.x + Math.round(deltaX * (step / (float) steps));
            int centerZ = entrance.z + Math.round(deltaZ * (step / (float) steps));
            for (int width = -ACCESS_HALF_WIDTH; width <= ACCESS_HALF_WIDTH; width++) {
                int x = eastWest ? centerX : centerX + width;
                int z = eastWest ? centerZ + width : centerZ;
                if (!contains(chunk, x, z)) continue;
                int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
                plan.addSet(x, surfaceY, z, Blocks.PACKED_MUD);
                if (step > 2) {
                    plan.addFill(x, surfaceY + 1, z, x, surfaceY + 3, z, Blocks.AIR);
                }
            }
        }
    }

    private static SourceTemplate template(String resource) {
        return TEMPLATE_CACHE.computeIfAbsent(resource, ExternalUrbanFabricBuilder::decode);
    }

    private static SourceTemplate decode(String resource) {
        SpongeStructureTemplate source = SpongeStructureTemplate.load(resource);
        int width = source.width();
        int height = source.height();
        int length = source.length();
        int layer = width * length;
        int volume = layer * height;
        BlockState[] palette = new BlockState[source.palette().size()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = parseState(source.palette().get(i));
        }

        BitSet candidates = new BitSet(volume);
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int paletteId = source.paletteIndex(x, y, z);
                    if (paletteId < 0 || paletteId >= palette.length) continue;
                    String id = blockId(source.palette().get(paletteId));
                    if (isSkippedSourceBlock(id) || palette[paletteId].isAir()) continue;
                    candidates.set(x + z * width + y * layer);
                }
            }
        }
        BitSet retained = retainStructuralComponents(candidates, width, height, length);
        if (retained.cardinality() < 900) {
            throw new IllegalStateException("External urban source is too sparse: "
                    + resource + " blocks=" + retained.cardinality());
        }

        int minX = width;
        int maxX = -1;
        int minY = height;
        int maxY = -1;
        int minZ = length;
        int maxZ = -1;
        List<RawBlock> raw = new ArrayList<>(retained.cardinality());
        for (int index = retained.nextSetBit(0);
             index >= 0;
             index = retained.nextSetBit(index + 1)) {
            int y = index / layer;
            int local = index - y * layer;
            int z = local / width;
            int x = local - z * width;
            int paletteId = source.paletteIndex(x, y, z);
            raw.add(new RawBlock(x, y, z, palette[paletteId]));
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }

        List<BuildingBlock> blocks = new ArrayList<>(raw.size());
        for (RawBlock block : raw) {
            blocks.add(new BuildingBlock(
                    block.x - minX,
                    block.y - minY,
                    block.z - minZ,
                    block.state
            ));
        }
        int templateWidth = maxX - minX + 1;
        int templateHeight = maxY - minY + 1;
        int templateLength = maxZ - minZ + 1;
        List<BuildingBlock> doors = findEntranceBlocks(
                blocks, templateWidth, templateLength);
        List<FacadeFragment> fragments = createFragments(
                resource, blocks, doors, templateWidth, templateHeight, templateLength);
        SourceTemplate template = new SourceTemplate(
                templateWidth, templateHeight, templateLength,
                List.copyOf(blocks), fragments);
        LivingKingdoms.LOGGER.info(
                "Prepared external urban facade kit resource={} fragments={} blocks={} dimensions={}x{}x{}",
                resource, fragments.size(), blocks.size(),
                templateWidth, templateHeight, templateLength
        );
        return template;
    }

    private static List<FacadeFragment> createFragments(
            String resource,
            List<BuildingBlock> sourceBlocks,
            List<BuildingBlock> entrances,
            int width, int height, int length) {
        List<BuildingBlock> candidates = entrances.isEmpty()
                ? List.of(new BuildingBlock(width / 2, 1, 0, Blocks.OAK_DOOR.defaultBlockState()))
                : entrances;

        if (INDEPENDENT_URBAN_SOURCES.contains(resource)) {
            if (entrances.isEmpty()) {
                throw new IllegalStateException(
                        "Independent Erden urban source has no retained real door: " + resource);
            }
            FacadeFragment bestFragment = null;
            BuildingBlock bestEntrance = null;
            FrontSide bestCropSide = null;
            int bestReachable = -1;
            int bestExitProof = 0;
            int candidateCount = 0;
            Set<Long> testedDoorColumns = new LinkedHashSet<>();
            for (BuildingBlock entrance : candidates) {
                long doorColumn = ((long) entrance.x << 32) ^ (entrance.z & 0xffffffffL);
                if (!testedDoorColumns.add(doorColumn)) continue;
                for (FrontSide cropSide : FrontSide.values()) {
                    int depth = Math.min(38, cropSide.horizontal ? width : length);
                    if (edgeDistance(entrance.x, entrance.z, width, length, cropSide) >= depth) continue;
                    FacadeFragment candidate = cropFragment(
                            sourceBlocks, entrance, cropSide, width, height, length);
                    if (candidate.blocks.size() < 500 || !containsRealEntrance(candidate)) continue;
                    int exitProof = fragmentExteriorExitProof(candidate);
                    if (exitProof <= 0) continue;
                    int reachable = fragmentReachableCells(candidate);
                    candidateCount++;
                    if (reachable > bestReachable
                            || reachable == bestReachable && exitProof > bestExitProof) {
                        bestReachable = reachable;
                        bestExitProof = exitProof;
                        bestFragment = candidate;
                        bestEntrance = entrance;
                        bestCropSide = cropSide;
                    }
                }
            }
            if (bestFragment == null || bestReachable < MIN_INDEPENDENT_SOURCE_REACHABLE) {
                throw new IllegalStateException(
                        "No usable fixed-footprint independent Erden source crop resource=" + resource
                                + " best_reachable=" + bestReachable
                                + " required=" + MIN_INDEPENDENT_SOURCE_REACHABLE
                                + " candidate_columns=" + testedDoorColumns.size());
            }
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_INDEPENDENT_URBAN_SOURCE_SELECTED resource={} reachable={} exit_proof={} crop_side={} source_door={},{} fragment={}x{} entrance_local={},{} resolved_side={} candidates={} candidate_columns={} same_footprint=true source_blocks_cut=0 source_only=true world_reads=false mutations=0",
                    resource, bestReachable, bestExitProof, bestCropSide,
                    bestEntrance.x, bestEntrance.z,
                    bestFragment.width, bestFragment.length,
                    bestFragment.entranceX, bestFragment.entranceZ,
                    bestFragment.exteriorSide, candidateCount, testedDoorColumns.size());
            return List.of(bestFragment);
        }

        // The fantasy starter castle exposes several real doors. Retain only doorway columns that
        // prove a large fixed-footprint interior, then keep at most the two strongest distinct crops.
        // This preserves the no-cut source contract while allowing repeated city lots to vary their
        // actual facade/interior geometry instead of cloning one crop hundreds of times.
        if (CASTLE_HOUSE.equals(resource) && !entrances.isEmpty()) {
            record CropOption(
                    FacadeFragment fragment,
                    int reachable,
                    BuildingBlock entrance,
                    FrontSide cropSide) {
            }

            List<CropOption> options = new ArrayList<>();
            Set<Long> testedDoorColumns = new LinkedHashSet<>();
            for (BuildingBlock entrance : candidates) {
                long doorColumn = ((long) entrance.x << 32) ^ (entrance.z & 0xffffffffL);
                if (!testedDoorColumns.add(doorColumn)) continue;
                for (FrontSide cropSide : FrontSide.values()) {
                    int depth = Math.min(38, cropSide.horizontal ? width : length);
                    if (edgeDistance(entrance.x, entrance.z, width, length, cropSide) >= depth) continue;
                    FacadeFragment candidate = withExteriorSide(
                            cropFragment(sourceBlocks, entrance, cropSide, width, height, length),
                            cropSide);
                    if (candidate.blocks.size() < 500 || !containsRealEntrance(candidate)) continue;
                    int reachable = fragmentReachableCells(candidate);
                    if (reachable >= 500) {
                        options.add(new CropOption(candidate, reachable, entrance, cropSide));
                    }
                }
            }
            options.sort((left, right) -> {
                int byReachable = Integer.compare(right.reachable(), left.reachable());
                if (byReachable != 0) return byReachable;
                int byX = Integer.compare(left.entrance().x, right.entrance().x);
                if (byX != 0) return byX;
                int byZ = Integer.compare(left.entrance().z, right.entrance().z);
                if (byZ != 0) return byZ;
                return Integer.compare(left.cropSide().ordinal(), right.cropSide().ordinal());
            });
            if (options.isEmpty() || options.getFirst().reachable() < 500) {
                int bestReachable = options.isEmpty() ? -1 : options.getFirst().reachable();
                throw new IllegalStateException(
                        "No usable fixed-footprint castle-house crop; best_reachable=" + bestReachable);
            }

            CropOption best = options.getFirst();
            int minimumVariantReachable = Math.max(
                    500, (int) Math.ceil(best.reachable() * 0.95D));
            List<FacadeFragment> selected = new ArrayList<>();
            for (CropOption option : options) {
                if (option.reachable() < minimumVariantReachable) break;
                selected.add(option.fragment());
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_CASTLE_HOUSE_CROP_VARIANT_SELECTED index={} reachable={} crop_side={} source_door={},{} fragment={}x{} entrance_local={},{} resolved_side={} same_footprint=true source_only=true world_reads=false mutations=0",
                        selected.size() - 1, option.reachable(), option.cropSide(),
                        option.entrance().x, option.entrance().z,
                        option.fragment().width, option.fragment().length,
                        option.fragment().entranceX, option.fragment().entranceZ,
                        option.fragment().exteriorSide);
                if (selected.size() >= 2) break;
            }
            if (selected.isEmpty()) {
                throw new IllegalStateException(
                        "Castle-house crop quality threshold removed every candidate");
            }

            // Preserve the legacy single-best diagnostic for existing CI consumers.
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_CASTLE_HOUSE_CROP_SELECTED reachable={} crop_side={} source_door={},{} fragment={}x{} entrance_local={},{} resolved_side={} same_footprint=true source_only=true world_reads=false mutations=0",
                    best.reachable(), best.cropSide(), best.entrance().x, best.entrance().z,
                    best.fragment().width, best.fragment().length,
                    best.fragment().entranceX, best.fragment().entranceZ,
                    best.fragment().exteriorSide);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_CASTLE_HOUSE_CROP_VARIANTS_READY variants={} best_reachable={} minimum_reachable={} candidate_columns={} max_variants=2 source_blocks_cut=0 source_only=true world_reads=false mutations=0",
                    selected.size(), best.reachable(), minimumVariantReachable,
                    testedDoorColumns.size());
            return List.copyOf(selected);
        }

        LinkedHashSet<FrontSide> usedSides = new LinkedHashSet<>();
        List<FacadeFragment> result = new ArrayList<>();
        for (BuildingBlock entrance : candidates) {
            FrontSide side = nearestSide(entrance.x, entrance.z, width, length);
            if (!usedSides.add(side)) continue;
            FacadeFragment fragment = cropFragment(
                    sourceBlocks, entrance, side, width, height, length);
            if (fragment.blocks.size() >= 500) result.add(fragment);
        }
        if (result.isEmpty()) {
            result.add(cropFragment(
                    sourceBlocks,
                    new BuildingBlock(width / 2, 1, 0, Blocks.OAK_DOOR.defaultBlockState()),
                    FrontSide.NORTH, width, height, length));
        }
        return List.copyOf(result);
    }

    private static FacadeFragment cropFragment(
            List<BuildingBlock> sourceBlocks,
            BuildingBlock entrance,
            FrontSide side,
            int width, int height, int length) {
        int frontage = Math.min(34, side.horizontal ? length : width);
        int depth = Math.min(38, side.horizontal ? width : length);
        int minX;
        int maxX;
        int minZ;
        int maxZ;
        switch (side) {
            case NORTH -> {
                minX = clamp(entrance.x - frontage / 2, 0, Math.max(0, width - frontage));
                maxX = Math.min(width - 1, minX + frontage - 1);
                minZ = 0;
                maxZ = Math.min(length - 1, depth - 1);
            }
            case SOUTH -> {
                minX = clamp(entrance.x - frontage / 2, 0, Math.max(0, width - frontage));
                maxX = Math.min(width - 1, minX + frontage - 1);
                maxZ = length - 1;
                minZ = Math.max(0, maxZ - depth + 1);
            }
            case WEST -> {
                minX = 0;
                maxX = Math.min(width - 1, depth - 1);
                minZ = clamp(entrance.z - frontage / 2, 0, Math.max(0, length - frontage));
                maxZ = Math.min(length - 1, minZ + frontage - 1);
            }
            case EAST -> {
                maxX = width - 1;
                minX = Math.max(0, maxX - depth + 1);
                minZ = clamp(entrance.z - frontage / 2, 0, Math.max(0, length - frontage));
                maxZ = Math.min(length - 1, minZ + frontage - 1);
            }
            default -> throw new IllegalStateException("Unexpected front side " + side);
        }

        Map<Long, BuildingBlock> selected = new LinkedHashMap<>();
        for (BuildingBlock block : sourceBlocks) {
            if (block.x < minX || block.x > maxX
                    || block.z < minZ || block.z > maxZ) {
                continue;
            }
            BuildingBlock normalized = new BuildingBlock(
                    block.x - minX,
                    block.y,
                    block.z - minZ,
                    block.state);
            selected.put(localKey(normalized.x, normalized.y, normalized.z), normalized);
        }

        int fragmentWidth = maxX - minX + 1;
        int fragmentLength = maxZ - minZ + 1;
        BlockState wall = dominantWallState(selected.values());
        int sealHeight = Math.min(22, Math.max(8, height - 1));
        boolean cutWest = minX > 0 && side != FrontSide.WEST;
        boolean cutEast = maxX < width - 1 && side != FrontSide.EAST;
        boolean cutNorth = minZ > 0 && side != FrontSide.NORTH;
        boolean cutSouth = maxZ < length - 1 && side != FrontSide.SOUTH;
        sealFace(selected, wall, fragmentWidth, fragmentLength, sealHeight,
                cutWest, cutEast, cutNorth, cutSouth);

        int entranceX = clamp(entrance.x - minX, 1, Math.max(1, fragmentWidth - 2));
        int entranceZ = clamp(entrance.z - minZ, 1, Math.max(1, fragmentLength - 2));
        int entranceY = lowestDoorY(selected, entranceX, entrance.y, entranceZ);
        FrontSide exteriorSide = sourceExteriorSide(
                selected, entranceX, entranceY, entranceZ,
                fragmentWidth, fragmentLength, side, entrance.state);
        return new FacadeFragment(
                fragmentWidth, height, fragmentLength,
                List.copyOf(selected.values()), entranceX, entranceZ, exteriorSide);
    }

    private static int lowestDoorY(
            Map<Long, BuildingBlock> blocks, int x, int candidateY, int z) {
        int result = candidateY;
        for (int y = candidateY; y >= Math.max(0, candidateY - 2); y--) {
            BuildingBlock block = blocks.get(localKey(x, y, z));
            if (block != null && block.state.getBlock() instanceof DoorBlock) result = y;
        }
        return result;
    }

    /**
     * Imported structures frequently contain doors whose nearest crop edge is not the side one can
     * actually leave through. Probe the two sides of the door plane using the source blocks and
     * retain the side that has a real supported, body-clear run. This is decided before rotation,
     * so every repeated urban placement inherits the same authored doorway semantics.
     */
    private static FrontSide sourceExteriorSide(
            Map<Long, BuildingBlock> blocks,
            int x, int doorY, int z,
            int width, int length,
            FrontSide fallback,
            BlockState doorState) {
        if (!(doorState.getBlock() instanceof DoorBlock)
                || !doorState.hasProperty(DoorBlock.FACING)) {
            return fallback;
        }
        Direction facing = doorState.getValue(DoorBlock.FACING);
        FrontSide first = frontSide(facing);
        FrontSide second = frontSide(facing.getOpposite());
        int firstRun = sourceClearRun(blocks, x, doorY, z, first);
        int secondRun = sourceClearRun(blocks, x, doorY, z, second);
        if (firstRun != secondRun) return firstRun > secondRun ? first : second;

        int firstEdge = edgeDistance(x, z, width, length, first);
        int secondEdge = edgeDistance(x, z, width, length, second);
        if (firstEdge != secondEdge) return firstEdge < secondEdge ? first : second;
        if (fallback == first || fallback == second) return fallback;
        return first;
    }

    private static int sourceClearRun(
            Map<Long, BuildingBlock> blocks,
            int x, int doorY, int z,
            FrontSide side) {
        int stepX = side == FrontSide.EAST ? 1 : side == FrontSide.WEST ? -1 : 0;
        int stepZ = side == FrontSide.SOUTH ? 1 : side == FrontSide.NORTH ? -1 : 0;
        int feetY = doorY;
        int clear = 0;
        for (int depth = 1; depth <= SOURCE_EXIT_PROBE; depth++) {
            int resolved = sourceWalkableFeetY(
                    blocks, x + stepX * depth, z + stepZ * depth, feetY);
            if (resolved == Integer.MIN_VALUE || Math.abs(resolved - feetY) > 1) break;
            feetY = resolved;
            clear++;
        }
        return clear;
    }

    private static int sourceWalkableFeetY(
            Map<Long, BuildingBlock> blocks,
            int x, int z, int preferredFeetY) {
        int[] offsets = {0, 1, -1};
        for (int offset : offsets) {
            int feetY = preferredFeetY + offset;
            if (sourceWalkable(blocks, x, feetY, z)) return feetY;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean sourceWalkable(
            Map<Long, BuildingBlock> blocks, int x, int feetY, int z) {
        if (!sourceBodyPassable(blocks.get(localKey(x, feetY, z)))
                || !sourceBodyPassable(blocks.get(localKey(x, feetY + 1, z)))) {
            return false;
        }
        BuildingBlock floor = blocks.get(localKey(x, feetY - 1, z));
        return floor != null && !floor.state.isAir();
    }

    private static boolean sourceBodyPassable(BuildingBlock block) {
        if (block == null || block.state.isAir()) return true;
        if (block.state.getBlock() instanceof DoorBlock) return true;
        String id = BuiltInRegistries.BLOCK.getKey(block.state.getBlock()).toString();
        return id.contains("torch")
                || id.contains("button")
                || id.contains("pressure_plate")
                || id.endsWith("_sign")
                || id.endsWith("_wall_sign");
    }

    private static int edgeDistance(
            int x, int z, int width, int length, FrontSide side) {
        return switch (side) {
            case NORTH -> z;
            case SOUTH -> length - 1 - z;
            case WEST -> x;
            case EAST -> width - 1 - x;
        };
    }

    private static FrontSide frontSide(Direction direction) {
        return switch (direction) {
            case NORTH -> FrontSide.NORTH;
            case SOUTH -> FrontSide.SOUTH;
            case WEST -> FrontSide.WEST;
            case EAST -> FrontSide.EAST;
            default -> throw new IllegalArgumentException("Non-horizontal doorway direction " + direction);
        };
    }

    private static FrontSide rotateFrontSide(FrontSide side, Rotation rotation) {
        return switch (rotation) {
            case NONE -> side;
            case CLOCKWISE_90 -> switch (side) {
                case NORTH -> FrontSide.EAST;
                case EAST -> FrontSide.SOUTH;
                case SOUTH -> FrontSide.WEST;
                case WEST -> FrontSide.NORTH;
            };
            case CLOCKWISE_180 -> switch (side) {
                case NORTH -> FrontSide.SOUTH;
                case SOUTH -> FrontSide.NORTH;
                case WEST -> FrontSide.EAST;
                case EAST -> FrontSide.WEST;
            };
            case COUNTERCLOCKWISE_90 -> switch (side) {
                case NORTH -> FrontSide.WEST;
                case WEST -> FrontSide.SOUTH;
                case SOUTH -> FrontSide.EAST;
                case EAST -> FrontSide.NORTH;
            };
        };
    }

    private static void sealFace(
            Map<Long, BuildingBlock> blocks,
            BlockState wall,
            int width, int length, int height,
            boolean west, boolean east, boolean north, boolean south) {
        for (int y = 1; y <= height; y++) {
            if (west) {
                for (int z = 0; z < length; z++) {
                    putIfMissing(blocks, new BuildingBlock(0, y, z, wall));
                }
            }
            if (east) {
                for (int z = 0; z < length; z++) {
                    putIfMissing(blocks, new BuildingBlock(width - 1, y, z, wall));
                }
            }
            if (north) {
                for (int x = 0; x < width; x++) {
                    putIfMissing(blocks, new BuildingBlock(x, y, 0, wall));
                }
            }
            if (south) {
                for (int x = 0; x < width; x++) {
                    putIfMissing(blocks, new BuildingBlock(x, y, length - 1, wall));
                }
            }
        }
    }

    private static void putIfMissing(Map<Long, BuildingBlock> blocks, BuildingBlock block) {
        blocks.putIfAbsent(localKey(block.x, block.y, block.z), block);
    }

    private static BlockState dominantWallState(Iterable<BuildingBlock> blocks) {
        Map<BlockState, Integer> counts = new HashMap<>();
        for (BuildingBlock block : blocks) {
            if (block.y < 1 || block.y > 18) continue;
            Block candidate = block.state.getBlock();
            String id = BuiltInRegistries.BLOCK.getKey(candidate).toString();
            if (id.endsWith("_door") || id.endsWith("_trapdoor")
                    || id.contains("glass") || id.contains("lantern")
                    || id.contains("torch") || id.contains("fence")
                    || id.contains("wall") || id.contains("stairs")
                    || id.contains("slab") || id.contains("carpet")
                    || candidate == Blocks.CHEST || candidate == Blocks.BARREL) {
                continue;
            }
            counts.merge(block.state, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Blocks.STONE_BRICKS.defaultBlockState());
    }

    private static List<BuildingBlock> findEntranceBlocks(
            List<BuildingBlock> blocks, int width, int length) {
        List<BuildingBlock> doors = new ArrayList<>();
        for (BuildingBlock block : blocks) {
            String id = BuiltInRegistries.BLOCK.getKey(block.state.getBlock()).toString();
            if (id.endsWith("_door") && !id.endsWith("_trapdoor")) doors.add(block);
        }
        if (doors.isEmpty()) return List.of();
        int lowest = doors.stream().mapToInt(BuildingBlock::y).min().orElse(0);
        List<BuildingBlock> lowDoors = doors.stream()
                .filter(block -> block.y <= lowest + 1)
                .toList();
        List<BuildingBlock> edgeDoors = lowDoors.stream()
                .filter(block -> block.x <= 5 || block.x >= width - 6
                        || block.z <= 5 || block.z >= length - 6)
                .toList();
        return edgeDoors.isEmpty() ? lowDoors : edgeDoors;
    }

    private static FrontSide nearestSide(int x, int z, int width, int length) {
        int north = z;
        int south = length - 1 - z;
        int west = x;
        int east = width - 1 - x;
        int minimum = Math.min(Math.min(north, south), Math.min(west, east));
        if (minimum == north) return FrontSide.NORTH;
        if (minimum == south) return FrontSide.SOUTH;
        if (minimum == west) return FrontSide.WEST;
        return FrontSide.EAST;
    }

    private static RoadTarget nearestRoadOnSide(
            int x, int z,
            int buildingMinX, int buildingMinZ,
            int buildingMaxX, int buildingMaxZ,
            FrontSide side) {
        for (int radius = 1; radius <= MAX_ROAD_SEARCH; radius++) {
            for (int offset = -radius; offset <= radius; offset++) {
                RoadTarget top = roadTargetOnSide(
                        x + offset, z - radius,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ, side);
                if (top != null) return top;
                RoadTarget bottom = roadTargetOnSide(
                        x + offset, z + radius,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ, side);
                if (bottom != null) return bottom;
            }
            for (int offset = -radius + 1; offset < radius; offset++) {
                RoadTarget left = roadTargetOnSide(
                        x - radius, z + offset,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ, side);
                if (left != null) return left;
                RoadTarget right = roadTargetOnSide(
                        x + radius, z + offset,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ, side);
                if (right != null) return right;
            }
        }
        return null;
    }

    private static RoadTarget roadTargetOnSide(
            int x, int z,
            int buildingMinX, int buildingMinZ,
            int buildingMaxX, int buildingMaxZ,
            FrontSide side) {
        boolean beyondFacade = switch (side) {
            case NORTH -> z < buildingMinZ - 1;
            case SOUTH -> z > buildingMaxZ + 1;
            case WEST -> x < buildingMinX - 1;
            case EAST -> x > buildingMaxX + 1;
        };
        if (!beyondFacade) return null;
        return roadTarget(x, z, buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
    }

    private static RoadTarget nearestRoad(int x, int z,
                                          int buildingMinX, int buildingMinZ,
                                          int buildingMaxX, int buildingMaxZ) {
        for (int radius = 1; radius <= MAX_ROAD_SEARCH; radius++) {
            for (int offset = -radius; offset <= radius; offset++) {
                RoadTarget top = roadTarget(
                        x + offset, z - radius,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
                if (top != null) return top;
                RoadTarget bottom = roadTarget(
                        x + offset, z + radius,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
                if (bottom != null) return bottom;
            }
            for (int offset = -radius + 1; offset < radius; offset++) {
                RoadTarget left = roadTarget(
                        x - radius, z + offset,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
                if (left != null) return left;
                RoadTarget right = roadTarget(
                        x + radius, z + offset,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
                if (right != null) return right;
            }
        }
        return null;
    }

    private static RoadTarget roadTarget(int x, int z,
                                         int buildingMinX, int buildingMinZ,
                                         int buildingMaxX, int buildingMaxZ) {
        if (x >= buildingMinX - 1 && x <= buildingMaxX + 1
                && z >= buildingMinZ - 1 && z <= buildingMaxZ + 1) {
            return null;
        }
        return ErdenCapitalStreamingBuilder.roadClassAt(x, z)
                == ErdenCapitalStreamingBuilder.RoadClass.NONE
                ? null : new RoadTarget(x, z);
    }

    private static BitSet retainStructuralComponents(
            BitSet candidates, int width, int height, int length) {
        int layer = width * length;
        BitSet remaining = (BitSet) candidates.clone();
        BitSet retained = new BitSet(layer * height);
        int[] queue = new int[Math.max(1, candidates.cardinality())];
        while (!remaining.isEmpty()) {
            int seed = remaining.nextSetBit(0);
            int head = 0;
            int tail = 0;
            remaining.clear(seed);
            queue[tail++] = seed;
            while (head < tail) {
                int index = queue[head++];
                int y = index / layer;
                int local = index - y * layer;
                int z = local / width;
                int x = local - z * width;
                if (x > 0) tail = enqueue(index - 1, remaining, queue, tail);
                if (x + 1 < width) tail = enqueue(index + 1, remaining, queue, tail);
                if (z > 0) tail = enqueue(index - width, remaining, queue, tail);
                if (z + 1 < length) tail = enqueue(index + width, remaining, queue, tail);
                if (y > 0) tail = enqueue(index - layer, remaining, queue, tail);
                if (y + 1 < height) tail = enqueue(index + layer, remaining, queue, tail);
            }
            if (tail >= MIN_COMPONENT_BLOCKS) {
                for (int i = 0; i < tail; i++) retained.set(queue[i]);
            }
        }
        return retained;
    }

    private static int enqueue(int index, BitSet remaining, int[] queue, int tail) {
        if (!remaining.get(index)) return tail;
        remaining.clear(index);
        queue[tail] = index;
        return tail + 1;
    }

    private static boolean isSkippedSourceBlock(String id) {
        return SKIPPED_TERRAIN.contains(id)
                || SKIPPED_FLORA.contains(id)
                || id.endsWith("_leaves")
                || id.endsWith("_sapling")
                || id.endsWith("_tulip")
                || id.endsWith("_coral")
                || id.endsWith("_coral_fan");
    }

    private static BlockState parseState(String specification) {
        String originalId = blockId(specification);
        String id = LEGACY_BLOCK_IDS.getOrDefault(originalId, originalId);
        Identifier key = Identifier.parse(id);
        Block block = BuiltInRegistries.BLOCK.getValue(key);
        if (block == null || block == Blocks.AIR && !"minecraft:air".equals(id)) {
            throw new IllegalStateException(
                    "Unknown external schematic block " + originalId
                            + " (resolved as " + id + ")");
        }
        BlockState state = block.defaultBlockState();
        int open = specification.indexOf('[');
        int close = specification.lastIndexOf(']');
        if (open < 0 || close <= open) return state;
        for (String assignment : specification.substring(open + 1, close).split(",")) {
            int equals = assignment.indexOf('=');
            if (equals <= 0) continue;
            String name = assignment.substring(0, equals).trim();
            String value = assignment.substring(equals + 1)
                    .trim().toLowerCase(Locale.ROOT);
            Property<?> property = block.getStateDefinition().getProperty(name);
            if (property != null) state = applyProperty(state, property, value);
        }
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applyProperty(
            BlockState state, Property property, String value) {
        Optional parsed = property.getValue(value);
        return parsed.isPresent()
                ? state.setValue(property, (Comparable) parsed.get())
                : state;
    }

    private static String blockId(String specification) {
        int bracket = specification.indexOf('[');
        return (bracket < 0 ? specification : specification.substring(0, bracket)).trim();
    }

    private static int rotatedWidth(FacadeFragment fragment, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90
                || rotation == Rotation.COUNTERCLOCKWISE_90
                ? fragment.length : fragment.width;
    }

    private static int rotatedLength(FacadeFragment fragment, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90
                || rotation == Rotation.COUNTERCLOCKWISE_90
                ? fragment.width : fragment.length;
    }

    private static RotatedOffset rotate(
            int x, int z, int width, int length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new RotatedOffset(x, z);
            case CLOCKWISE_90 -> new RotatedOffset(length - 1 - z, x);
            case CLOCKWISE_180 -> new RotatedOffset(
                    width - 1 - x, length - 1 - z);
            case COUNTERCLOCKWISE_90 -> new RotatedOffset(
                    z, width - 1 - x);
        };
    }

    private static boolean segmentIntersects(
            ChunkPos chunk, int x1, int z1, int x2, int z2, int margin) {
        int minX = Math.min(x1, x2) - margin;
        int maxX = Math.max(x1, x2) + margin;
        int minZ = Math.min(z1, z2) - margin;
        int maxZ = Math.max(z1, z2) + margin;
        return maxX >= chunk.getMinBlockX()
                && minX <= chunk.getMinBlockX() + 15
                && maxZ >= chunk.getMinBlockZ()
                && minZ <= chunk.getMinBlockZ() + 15;
    }

    private static boolean contains(ChunkPos chunk, int x, int z) {
        return x >= chunk.getMinBlockX()
                && x <= chunk.getMinBlockX() + 15
                && z >= chunk.getMinBlockZ()
                && z <= chunk.getMinBlockZ() + 15;
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long localKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int mix(int x, int z, int salt) {
        int value = x * 0x1f1f1f1f ^ z * 0x45d9f3b ^ salt;
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        return value ^ value >>> 16;
    }

    private enum UrbanRole {
        TENEMENT("tenement"),
        SHOP("shop"),
        BAKERY("bakery"),
        INN("inn"),
        STABLE("stable"),
        GUARD_POST("guard_post"),
        BATHHOUSE("bathhouse"),
        WAREHOUSE("warehouse");

        private final String id;

        UrbanRole(String id) {
            this.id = id;
        }

        static UrbanRole fromId(String id) {
            for (UrbanRole role : values()) {
                if (role.id.equals(id)) return role;
            }
            throw new IllegalArgumentException("Unknown urban role " + id);
        }
    }

    private enum FrontSide {
        NORTH(false),
        SOUTH(false),
        WEST(true),
        EAST(true);

        private final boolean horizontal;

        FrontSide(boolean horizontal) {
            this.horizontal = horizontal;
        }
    }

    private record SourceTemplate(
            int width, int height, int length,
            List<BuildingBlock> blocks,
            List<FacadeFragment> fragments) {
    }

    private record FacadeFragment(
            int width, int height, int length,
            List<BuildingBlock> blocks,
            int entranceX, int entranceZ,
            FrontSide exteriorSide) {
    }

    private record UrbanPlacement(
            String resource,
            int centerX, int centerZ,
            Rotation rotation,
            UrbanRole role,
            FacadeFragment fragment,
            UrbanEntrance entrance) {
        int rotatedWidth() {
            return ExternalUrbanFabricBuilder.rotatedWidth(fragment, rotation);
        }

        int rotatedLength() {
            return ExternalUrbanFabricBuilder.rotatedLength(fragment, rotation);
        }

        boolean intersects(ChunkPos chunk) {
            int width = rotatedWidth();
            int length = rotatedLength();
            int minX = centerX - width / 2;
            int maxX = minX + width - 1;
            int minZ = centerZ - length / 2;
            int maxZ = minZ + length - 1;
            return maxX >= chunk.getMinBlockX()
                    && minX <= chunk.getMinBlockX() + 15
                    && maxZ >= chunk.getMinBlockZ()
                    && minZ <= chunk.getMinBlockZ() + 15;
        }
    }

    public record UrbanEntrance(
            String role, int x, int z, int roadX, int roadZ) {
    }

    public record UrbanBuildingPlacement(
            String role,
            String resource,
            String fragmentKey,
            Rotation rotation,
            UrbanEntrance entrance,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            int baseY,
            int height,
            int width,
            int length) {
    }

    public record UrbanFragmentSnapshot(
            String fragmentKey,
            String resource,
            int width,
            int height,
            int length,
            int entranceX,
            int entranceZ,
            String exteriorSide,
            List<UrbanSourceBlock> blocks) {
    }

    public record UrbanSourceBlock(int x, int y, int z, BlockState state) {
    }

    private record Exclusion(
            int centerX, int centerZ, int halfWidth, int halfLength) {
    }

    private record RawBlock(int x, int y, int z, BlockState state) {
    }

    private record BuildingBlock(int x, int y, int z, BlockState state) {
    }

    private record RotatedOffset(int x, int z) {
    }

    private record RoadTarget(int x, int z) {
    }

    private record PlacedBlock(int x, int y, int z, BlockState state) {
    }

    private record VerticalSpan(int x, int z, int minY, int maxY) {
        VerticalSpan merge(VerticalSpan other) {
            return new VerticalSpan(
                    x, z,
                    Math.min(minY, other.minY),
                    Math.max(maxY, other.maxY));
        }
    }
}
