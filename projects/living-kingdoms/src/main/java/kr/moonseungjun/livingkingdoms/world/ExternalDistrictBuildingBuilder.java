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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Terrain-cleans and places attributed external buildings as functional capital blocks. */
public final class ExternalDistrictBuildingBuilder {
    private static final String MANOR =
            "/data/livingkingdoms/structures/external/medieval_manor.schem";
    private static final String HOUSE =
            "/data/livingkingdoms/structures/external/all_in_one_house.schem";
    private static final String CASTLE_HOUSE =
            "/data/livingkingdoms/structures/external/fantasy_castle_house.schem";
    private static final String CHURCH =
            "/data/livingkingdoms/structures/external/village_church.schem";

    private static final Map<String, BuildingTemplate> CACHE = new HashMap<>();
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
    private static final int MIN_COMPONENT_BLOCKS = 24;
    private static final int MAX_ROAD_SEARCH = 112;
    private static final int MAX_EXTERIOR_EDGE_DISTANCE = 6;

    private static final List<Placement> LANDMARK_PLACEMENTS = List.of(
            new Placement(MANOR, -390, -520, Rotation.CLOCKWISE_90, "royal_chancery"),
            new Placement(MANOR, 390, -520, Rotation.COUNTERCLOCKWISE_90, "treasury_court"),
            new Placement(MANOR, -700, -610, Rotation.NONE, "western_noble_estate"),
            new Placement(MANOR, -700, -350, Rotation.CLOCKWISE_180, "magistrates_estate"),
            new Placement(CHURCH, 710, -560, Rotation.COUNTERCLOCKWISE_90, "great_temple"),
            new Placement(CHURCH, 720, -270, Rotation.CLOCKWISE_90, "pilgrim_hospital"),
            new Placement(CASTLE_HOUSE, -720, 540, Rotation.CLOCKWISE_90, "western_barracks"),
            new Placement(CASTLE_HOUSE, -400, 610, Rotation.NONE, "royal_guard_academy"),
            new Placement(CASTLE_HOUSE, 760, 590, Rotation.COUNTERCLOCKWISE_90, "eastern_watch_barracks"),
            new Placement(HOUSE, 360, 300, Rotation.CLOCKWISE_180, "merchant_guildhall"),
            new Placement(HOUSE, 650, 220, Rotation.COUNTERCLOCKWISE_90, "covered_craft_hall"),
            new Placement(HOUSE, 620, 520, Rotation.CLOCKWISE_90, "artisan_compound"),
            new Placement(HOUSE, -170, 600, Rotation.NONE, "citizen_court_west"),
            new Placement(HOUSE, 170, 600, Rotation.CLOCKWISE_180, "citizen_court_east"),
            new Placement(HOUSE, -990, -420, Rotation.CLOCKWISE_90, "north_river_warehouse"),
            new Placement(HOUSE, -980, 250, Rotation.COUNTERCLOCKWISE_90, "south_river_warehouse")
    );

    private static final List<Placement> RESIDENTIAL_PLACEMENTS = List.of(
            new Placement(HOUSE, -1020, -720, Rotation.CLOCKWISE_90, "residential_north_01"),
            new Placement(CASTLE_HOUSE, -820, -720, Rotation.NONE, "residential_north_02"),
            new Placement(HOUSE, -180, -720, Rotation.CLOCKWISE_180, "residential_north_03"),
            new Placement(HOUSE, 180, -720, Rotation.NONE, "residential_north_04"),
            new Placement(CASTLE_HOUSE, 820, -720, Rotation.CLOCKWISE_180, "residential_north_05"),
            new Placement(HOUSE, 1020, -720, Rotation.COUNTERCLOCKWISE_90, "residential_north_06"),
            new Placement(HOUSE, -1020, -180, Rotation.CLOCKWISE_90, "residential_middle_north_01"),
            new Placement(MANOR, -780, -180, Rotation.NONE, "residential_middle_north_02"),
            new Placement(HOUSE, -360, -180, Rotation.CLOCKWISE_180, "residential_middle_north_03"),
            new Placement(HOUSE, 360, -180, Rotation.NONE, "residential_middle_north_04"),
            new Placement(MANOR, 780, -180, Rotation.CLOCKWISE_180, "residential_middle_north_05"),
            new Placement(HOUSE, 1020, -180, Rotation.COUNTERCLOCKWISE_90, "residential_middle_north_06"),
            new Placement(HOUSE, -1020, 180, Rotation.CLOCKWISE_90, "residential_middle_south_01"),
            new Placement(MANOR, -780, 180, Rotation.CLOCKWISE_180, "residential_middle_south_02"),
            new Placement(HOUSE, -360, 180, Rotation.NONE, "residential_middle_south_03"),
            new Placement(HOUSE, 360, 180, Rotation.CLOCKWISE_180, "residential_middle_south_04"),
            new Placement(MANOR, 780, 180, Rotation.NONE, "residential_middle_south_05"),
            new Placement(HOUSE, 1020, 180, Rotation.COUNTERCLOCKWISE_90, "residential_middle_south_06"),
            new Placement(HOUSE, -1020, 720, Rotation.CLOCKWISE_90, "residential_south_01"),
            new Placement(CASTLE_HOUSE, -820, 720, Rotation.CLOCKWISE_180, "residential_south_02"),
            new Placement(HOUSE, -180, 720, Rotation.NONE, "residential_south_03"),
            new Placement(HOUSE, 180, 720, Rotation.CLOCKWISE_180, "residential_south_04"),
            new Placement(CASTLE_HOUSE, 820, 720, Rotation.NONE, "residential_south_05"),
            new Placement(HOUSE, 1020, 720, Rotation.COUNTERCLOCKWISE_90, "residential_south_06")
    );

    private static final List<Placement> ALL_PLACEMENTS = combinePlacements();
    private static volatile List<BuildingEntrance> cachedEntrances;

    private ExternalDistrictBuildingBuilder() {
    }

    public static void addChunk(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {
        for (Placement placement : ALL_PLACEMENTS) {
            BuildingTemplate template = template(placement.resource);
            if (!placement.intersects(chunk, template)) continue;
            pasteClipped(plan, level, chunk, template, placement);
        }
    }

    public static void addSupplyBuildingChunk(
            IncrementalWorldEditPlan plan,
            ServerLevel level,
            ChunkPos chunk,
            int centerX,
            int centerZ,
            String nodeId,
            String role,
            String buildingStyle,
            int facingQuarterTurns) {
        String resource = switch (buildingStyle) {
            case "manor" -> MANOR;
            case "castle_house" -> CASTLE_HOUSE;
            case "church" -> CHURCH;
            default -> HOUSE;
        };
        Rotation rotation = switch (Math.floorMod(facingQuarterTurns, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        Placement placement = new Placement(
                resource, centerX, centerZ, rotation, "supply_" + role + "_" + nodeId);
        BuildingTemplate template = template(resource);
        if (placement.intersects(chunk, template)) {
            pasteClipped(plan, level, chunk, template, placement);
        }
    }

    public static int landmarkCount() {
        return LANDMARK_PLACEMENTS.size();
    }

    public static int residentialBlockCount() {
        return RESIDENTIAL_PLACEMENTS.size();
    }

    public static List<BuildingEntrance> entrances() {
        List<BuildingEntrance> result = cachedEntrances;
        if (result != null) return result;
        synchronized (ExternalDistrictBuildingBuilder.class) {
            result = cachedEntrances;
            if (result == null) {
                result = List.copyOf(computeEntrances());
                cachedEntrances = result;
                LivingKingdoms.LOGGER.info(
                        "Prepared Erden building access anchors entrances={} landmarks={} residential_blocks={} exterior_doors=true",
                        result.size(), landmarkCount(), residentialBlockCount());
            }
            return result;
        }
    }

    private static List<Placement> combinePlacements() {
        List<Placement> placements = new ArrayList<>(
                LANDMARK_PLACEMENTS.size() + RESIDENTIAL_PLACEMENTS.size());
        placements.addAll(LANDMARK_PLACEMENTS);
        placements.addAll(RESIDENTIAL_PLACEMENTS);
        return List.copyOf(placements);
    }

    private static BuildingTemplate template(String resource) {
        return CACHE.computeIfAbsent(resource, ExternalDistrictBuildingBuilder::decode);
    }

    private static BuildingTemplate decode(String resource) {
        SpongeStructureTemplate source = SpongeStructureTemplate.load(resource);
        int width = source.width();
        int height = source.height();
        int length = source.length();
        int layer = width * length;
        int volume = layer * height;
        BlockState[] palette = new BlockState[source.palette().size()];
        for (int i = 0; i < palette.length; i++) palette[i] = parseState(source.palette().get(i));

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
            throw new IllegalStateException("External district building is too sparse: " + resource
                    + " blocks=" + retained.cardinality());
        }

        int minX = width;
        int maxX = -1;
        int minY = height;
        int maxY = -1;
        int minZ = length;
        int maxZ = -1;
        List<RawBlock> raw = new ArrayList<>(retained.cardinality());
        for (int index = retained.nextSetBit(0); index >= 0; index = retained.nextSetBit(index + 1)) {
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
                    block.state));
        }
        int templateWidth = maxX - minX + 1;
        int templateHeight = maxY - minY + 1;
        int templateLength = maxZ - minZ + 1;
        List<DoorCandidate> entrances = findEntranceBlocks(blocks, templateWidth, templateLength);
        BuildingTemplate template = new BuildingTemplate(
                templateWidth, templateHeight, templateLength,
                List.copyOf(blocks), entrances);
        LivingKingdoms.LOGGER.info(
                "Prepared external district building resource={} blocks={} dimensions={}x{}x{} entrances={} discarded={}",
                resource, blocks.size(), template.width, template.height, template.length,
                entrances.size(), candidates.cardinality() - retained.cardinality());
        return template;
    }

    private static List<DoorCandidate> findEntranceBlocks(
            List<BuildingBlock> blocks, int width, int length) {
        Map<Long, BuildingBlock> lowestDoorByColumn = new LinkedHashMap<>();
        Map<Long, BuildingBlock> index = new HashMap<>();
        for (BuildingBlock block : blocks) {
            index.put(localKey(block.x, block.y, block.z), block);
            if (!(block.state.getBlock() instanceof DoorBlock)) continue;
            long column = columnKey(block.x, block.z);
            BuildingBlock previous = lowestDoorByColumn.get(column);
            if (previous == null || block.y < previous.y) {
                lowestDoorByColumn.put(column, block);
            }
        }
        if (lowestDoorByColumn.isEmpty()) return List.of();

        List<DoorCandidate> exterior = new ArrayList<>();
        for (BuildingBlock door : lowestDoorByColumn.values()) {
            FrontSide side = exteriorSide(index, door, width, length);
            if (side != null) exterior.add(new DoorCandidate(door, side));
        }
        if (!exterior.isEmpty()) {
            int lowest = exterior.stream().mapToInt(candidate -> candidate.block.y).min().orElse(0);
            List<DoorCandidate> lowExterior = exterior.stream()
                    .filter(candidate -> candidate.block.y <= lowest + 3)
                    .toList();
            return lowExterior.isEmpty() ? List.copyOf(exterior) : List.copyOf(lowExterior);
        }

        List<BuildingBlock> doors = new ArrayList<>(lowestDoorByColumn.values());
        int lowest = doors.stream().mapToInt(BuildingBlock::y).min().orElse(0);
        List<BuildingBlock> lowDoors = doors.stream()
                .filter(block -> block.y <= lowest + 1)
                .toList();
        List<BuildingBlock> edgeDoors = lowDoors.stream()
                .filter(block -> block.x <= 4 || block.x >= width - 5
                        || block.z <= 4 || block.z >= length - 5)
                .toList();
        List<BuildingBlock> fallback = edgeDoors.isEmpty() ? lowDoors : edgeDoors;
        return fallback.stream()
                .map(block -> new DoorCandidate(block, nearestSide(block.x, block.z, width, length)))
                .toList();
    }

    /**
     * A Minecraft door is traversed along its facing axis, never laterally. Consider only those two
     * normals and choose the one that reaches a nearby structural edge through body-clear cells.
     * This rejects internal corridor doors that merely happen to sit close to the building bounds.
     */
    private static FrontSide exteriorSide(
            Map<Long, BuildingBlock> index,
            BuildingBlock door,
            int width,
            int length) {
        if (!(door.state.getBlock() instanceof DoorBlock)
                || !door.state.hasProperty(DoorBlock.FACING)) return null;
        Direction facing = door.state.getValue(DoorBlock.FACING);
        FrontSide first = frontSide(facing);
        FrontSide second = frontSide(facing.getOpposite());
        FrontSide best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (FrontSide side : new FrontSide[]{first, second}) {
            int distance = edgeDistance(door.x, door.z, width, length, side);
            if (distance > MAX_EXTERIOR_EDGE_DISTANCE || distance > bestDistance) continue;
            if (!bodyClearThroughEdge(index, door.x, door.y, door.z, side, distance)) continue;
            best = side;
            bestDistance = distance;
        }
        return best;
    }

    private static boolean bodyClearThroughEdge(
            Map<Long, BuildingBlock> index,
            int x,
            int doorY,
            int z,
            FrontSide side,
            int edgeDistance) {
        int stepX = side == FrontSide.EAST ? 1 : side == FrontSide.WEST ? -1 : 0;
        int stepZ = side == FrontSide.SOUTH ? 1 : side == FrontSide.NORTH ? -1 : 0;
        int probe = Math.max(1, edgeDistance + 1);
        for (int depth = 1; depth <= probe; depth++) {
            BuildingBlock feet = index.get(localKey(x + stepX * depth, doorY, z + stepZ * depth));
            BuildingBlock head = index.get(localKey(x + stepX * depth, doorY + 1, z + stepZ * depth));
            if (!sourceBodyPassable(feet) || !sourceBodyPassable(head)) return false;
        }
        return true;
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

    private static void pasteClipped(IncrementalWorldEditPlan plan, ServerLevel level,
                                     ChunkPos chunk, BuildingTemplate template,
                                     Placement placement) {
        int rotatedWidth = placement.rotatedWidth(template);
        int rotatedLength = placement.rotatedLength(template);
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
        for (BuildingBlock block : template.blocks) {
            RotatedOffset offset = rotate(block.x, block.z, template.width, template.length,
                    placement.rotation);
            int x = originX + offset.x;
            int z = originZ + offset.z;
            if (x < minChunkX || x > maxChunkX || z < minChunkZ || z > maxChunkZ) continue;
            int y = baseY + block.y;
            spans.merge(columnKey(x, z), new VerticalSpan(x, z, y, y), VerticalSpan::merge);
            placed.add(new PlacedBlock(x, y, z, block.state.rotate(placement.rotation)));
        }
        if (placed.isEmpty()) return;

        for (VerticalSpan span : spans.values()) {
            int surfaceY = plan.plannedSurfaceY(level, span.x, span.z);
            plan.addFill(span.x, span.minY, span.z,
                    span.x, Math.max(surfaceY, span.maxY + 2), span.z, Blocks.AIR);
            if (surfaceY < span.minY - 1) {
                plan.addFill(span.x, surfaceY + 1, span.z,
                        span.x, span.minY - 1, span.z, Blocks.STONE_BRICKS);
            }
            plan.addSet(span.x, span.minY - 1, span.z, Blocks.STONE_BRICKS);
        }
        for (PlacedBlock block : placed) plan.addSet(block.x, block.y, block.z, block.state);
    }

    private static List<BuildingEntrance> computeEntrances() {
        List<BuildingEntrance> result = new ArrayList<>();
        for (Placement placement : ALL_PLACEMENTS) {
            BuildingEntrance entrance = locateEntrance(placement, template(placement.resource));
            if (entrance != null) result.add(entrance);
        }
        return result;
    }

    private static BuildingEntrance locateEntrance(Placement placement, BuildingTemplate template) {
        int rotatedWidth = placement.rotatedWidth(template);
        int rotatedLength = placement.rotatedLength(template);
        int originX = placement.centerX - rotatedWidth / 2;
        int originZ = placement.centerZ - rotatedLength / 2;
        List<DoorCandidate> candidates = template.entrances;
        List<RotatedOffset> fallback = candidates.isEmpty()
                ? List.of(
                        new RotatedOffset(template.width / 2, 0),
                        new RotatedOffset(template.width / 2, template.length - 1),
                        new RotatedOffset(0, template.length / 2),
                        new RotatedOffset(template.width - 1, template.length / 2))
                : List.of();

        BuildingEntrance best = null;
        int bestDistance = Integer.MAX_VALUE;
        if (!candidates.isEmpty()) {
            for (DoorCandidate candidate : candidates) {
                RotatedOffset offset = rotate(candidate.block.x, candidate.block.z,
                        template.width, template.length, placement.rotation);
                FrontSide side = rotateSide(candidate.exteriorSide, placement.rotation);
                BuildingEntrance entrance = entranceForPoint(
                        placement, originX + offset.x, originZ + offset.z,
                        originX, originZ, rotatedWidth, rotatedLength, side);
                if (entrance == null) continue;
                int distance = Math.abs(entrance.roadX - entrance.x)
                        + Math.abs(entrance.roadZ - entrance.z);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = entrance;
                }
            }
        } else {
            for (RotatedOffset local : fallback) {
                RotatedOffset offset = rotate(local.x, local.z,
                        template.width, template.length, placement.rotation);
                BuildingEntrance entrance = entranceForPoint(
                        placement, originX + offset.x, originZ + offset.z,
                        originX, originZ, rotatedWidth, rotatedLength, null);
                if (entrance == null) continue;
                int distance = Math.abs(entrance.roadX - entrance.x)
                        + Math.abs(entrance.roadZ - entrance.z);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = entrance;
                }
            }
        }
        return best;
    }

    private static BuildingEntrance entranceForPoint(
            Placement placement, int x, int z,
            int buildingMinX, int buildingMinZ,
            int buildingWidth, int buildingLength,
            FrontSide exteriorSide) {
        int buildingMaxX = buildingMinX + buildingWidth - 1;
        int buildingMaxZ = buildingMinZ + buildingLength - 1;
        RoadTarget road = exteriorSide == null ? null : nearestRoadOnSide(
                x, z, buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ, exteriorSide);
        if (road == null) {
            road = nearestRoad(x, z, buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
        }
        if (road == null) return null;
        return new BuildingEntrance(
                placement.role, x, z, road.x, road.z,
                placement.role.startsWith("residential_"));
    }

    private static RoadTarget nearestRoadOnSide(
            int x, int z,
            int buildingMinX, int buildingMinZ,
            int buildingMaxX, int buildingMaxZ,
            FrontSide side) {
        for (int radius = 1; radius <= MAX_ROAD_SEARCH; radius++) {
            for (int offset = -radius; offset <= radius; offset++) {
                RoadTarget top = roadTargetOnSide(x + offset, z - radius,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ, side);
                if (top != null) return top;
                RoadTarget bottom = roadTargetOnSide(x + offset, z + radius,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ, side);
                if (bottom != null) return bottom;
            }
            for (int offset = -radius + 1; offset < radius; offset++) {
                RoadTarget left = roadTargetOnSide(x - radius, z + offset,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ, side);
                if (left != null) return left;
                RoadTarget right = roadTargetOnSide(x + radius, z + offset,
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
                RoadTarget top = roadTarget(x + offset, z - radius,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
                if (top != null) return top;
                RoadTarget bottom = roadTarget(x + offset, z + radius,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
                if (bottom != null) return bottom;
            }
            for (int offset = -radius + 1; offset < radius; offset++) {
                RoadTarget left = roadTarget(x - radius, z + offset,
                        buildingMinX, buildingMinZ, buildingMaxX, buildingMaxZ);
                if (left != null) return left;
                RoadTarget right = roadTarget(x + radius, z + offset,
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
                && z >= buildingMinZ - 1 && z <= buildingMaxZ + 1) return null;
        return ErdenCapitalStreamingBuilder.roadClassAt(x, z)
                == ErdenCapitalStreamingBuilder.RoadClass.NONE
                ? null : new RoadTarget(x, z);
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

    private static int edgeDistance(int x, int z, int width, int length, FrontSide side) {
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
            default -> throw new IllegalArgumentException("Non-horizontal door direction " + direction);
        };
    }

    private static FrontSide rotateSide(FrontSide side, Rotation rotation) {
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

    private static BitSet retainStructuralComponents(BitSet candidates,
                                                      int width, int height, int length) {
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

    private static RotatedOffset rotate(int x, int z, int width, int length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new RotatedOffset(x, z);
            case CLOCKWISE_90 -> new RotatedOffset(length - 1 - z, x);
            case CLOCKWISE_180 -> new RotatedOffset(width - 1 - x, length - 1 - z);
            case COUNTERCLOCKWISE_90 -> new RotatedOffset(z, width - 1 - x);
        };
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
            throw new IllegalStateException("Unknown external schematic block " + originalId
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
            String value = assignment.substring(equals + 1).trim().toLowerCase(Locale.ROOT);
            Property<?> property = block.getStateDefinition().getProperty(name);
            if (property != null) state = applyProperty(state, property, value);
        }
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applyProperty(BlockState state, Property property, String value) {
        Optional parsed = property.getValue(value);
        return parsed.isPresent() ? state.setValue(property, (Comparable) parsed.get()) : state;
    }

    private static String blockId(String specification) {
        int bracket = specification.indexOf('[');
        return (bracket < 0 ? specification : specification.substring(0, bracket)).trim();
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long localKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
    }

    private enum FrontSide {
        NORTH,
        SOUTH,
        WEST,
        EAST
    }

    private record BuildingTemplate(int width, int height, int length,
                                    List<BuildingBlock> blocks,
                                    List<DoorCandidate> entrances) {
    }

    private record DoorCandidate(BuildingBlock block, FrontSide exteriorSide) {
    }

    private record RawBlock(int x, int y, int z, BlockState state) {
    }

    private record BuildingBlock(int x, int y, int z, BlockState state) {
    }

    private record Placement(String resource, int centerX, int centerZ,
                             Rotation rotation, String role) {
        int rotatedWidth(BuildingTemplate template) {
            return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                    ? template.length : template.width;
        }

        int rotatedLength(BuildingTemplate template) {
            return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                    ? template.width : template.length;
        }

        boolean intersects(ChunkPos chunk, BuildingTemplate template) {
            int width = rotatedWidth(template);
            int length = rotatedLength(template);
            int minX = centerX - width / 2;
            int maxX = minX + width - 1;
            int minZ = centerZ - length / 2;
            int maxZ = minZ + length - 1;
            return maxX >= chunk.getMinBlockX() && minX <= chunk.getMinBlockX() + 15
                    && maxZ >= chunk.getMinBlockZ() && minZ <= chunk.getMinBlockZ() + 15;
        }
    }

    public record BuildingEntrance(String role, int x, int z,
                                   int roadX, int roadZ, boolean residential) {
    }

    private record RotatedOffset(int x, int z) {
    }

    private record RoadTarget(int x, int z) {
    }

    private record PlacedBlock(int x, int y, int z, BlockState state) {
    }

    private record VerticalSpan(int x, int z, int minY, int maxY) {
        VerticalSpan merge(VerticalSpan other) {
            return new VerticalSpan(x, z, Math.min(minY, other.minY), Math.max(maxY, other.maxY));
        }
    }
}
