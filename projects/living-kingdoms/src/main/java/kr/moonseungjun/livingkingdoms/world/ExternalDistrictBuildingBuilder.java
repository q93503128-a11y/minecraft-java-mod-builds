package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

/**
 * Terrain-cleans and places attributed external buildings as functional capital landmarks.
 *
 * <p>These are not random decoration. Every placement belongs to a named district role: government,
 * temple, military, guild, river trade, noble estate or citizen compound. The same architectural
 * family is rotated and spaced along the deterministic road plan rather than pasted as one giant map.</p>
 */
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

    private static final List<Placement> PLACEMENTS = List.of(
            // Crown and administration quarter.
            new Placement(MANOR, -390, -520, Rotation.CLOCKWISE_90, "royal_chancery"),
            new Placement(MANOR, 390, -520, Rotation.COUNTERCLOCKWISE_90, "treasury_court"),
            new Placement(MANOR, -700, -610, Rotation.NONE, "western_noble_estate"),
            new Placement(MANOR, -700, -350, Rotation.CLOCKWISE_180, "magistrates_estate"),

            // Temple and learning quarter.
            new Placement(CHURCH, 710, -560, Rotation.COUNTERCLOCKWISE_90, "great_temple"),
            new Placement(CHURCH, 720, -270, Rotation.CLOCKWISE_90, "pilgrim_hospital"),

            // Military quarter and gate reserves.
            new Placement(CASTLE_HOUSE, -720, 540, Rotation.CLOCKWISE_90, "western_barracks"),
            new Placement(CASTLE_HOUSE, -400, 610, Rotation.NONE, "royal_guard_academy"),
            new Placement(CASTLE_HOUSE, 760, 590, Rotation.COUNTERCLOCKWISE_90, "eastern_watch_barracks"),

            // Market, artisan and citizen compounds.
            new Placement(HOUSE, 360, 300, Rotation.CLOCKWISE_180, "merchant_guildhall"),
            new Placement(HOUSE, 650, 220, Rotation.COUNTERCLOCKWISE_90, "covered_craft_hall"),
            new Placement(HOUSE, 620, 520, Rotation.CLOCKWISE_90, "artisan_compound"),
            new Placement(HOUSE, -170, 600, Rotation.NONE, "citizen_court_west"),
            new Placement(HOUSE, 170, 600, Rotation.CLOCKWISE_180, "citizen_court_east"),

            // Silver River trade quarter. Buildings face the wharf road, not the water itself.
            new Placement(HOUSE, -990, -420, Rotation.CLOCKWISE_90, "north_river_warehouse"),
            new Placement(HOUSE, -980, 250, Rotation.COUNTERCLOCKWISE_90, "south_river_warehouse")
    );

    private ExternalDistrictBuildingBuilder() {
    }

    public static void addChunk(IncrementalWorldEditPlan plan, ServerLevel level, ChunkPos chunk) {
        for (Placement placement : PLACEMENTS) {
            BuildingTemplate template = template(placement.resource);
            if (!placement.intersects(chunk, template)) continue;
            pasteClipped(plan, level, chunk, template, placement);
        }
    }

    public static int landmarkCount() {
        return PLACEMENTS.size();
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
                    block.state
            ));
        }
        BuildingTemplate template = new BuildingTemplate(
                maxX - minX + 1,
                maxY - minY + 1,
                maxZ - minZ + 1,
                List.copyOf(blocks)
        );
        LivingKingdoms.LOGGER.info(
                "Prepared external district building resource={} blocks={} dimensions={}x{}x{} discarded={}",
                resource, blocks.size(), template.width, template.height, template.length,
                candidates.cardinality() - retained.cardinality()
        );
        return template;
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
            long key = columnKey(x, z);
            spans.merge(key, new VerticalSpan(x, z, y, y), VerticalSpan::merge);
            placed.add(new PlacedBlock(x, y, z, block.state.rotate(placement.rotation)));
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

    private record BuildingTemplate(int width, int height, int length,
                                    List<BuildingBlock> blocks) {
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

    private record RotatedOffset(int x, int z) {
    }

    private record PlacedBlock(int x, int y, int z, BlockState state) {
    }

    private record VerticalSpan(int x, int z, int minY, int maxY) {
        VerticalSpan merge(VerticalSpan other) {
            return new VerticalSpan(x, z, Math.min(minY, other.minY), Math.max(maxY, other.maxY));
        }
    }
}
