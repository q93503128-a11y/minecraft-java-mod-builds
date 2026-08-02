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
 * Imports licensed architecture as reusable world parts.
 *
 * <p>The full source schematic remains the royal inner citadel. Its straight curtain wall, round
 * tower and gatehouse portions are also cropped into repeatable modules for the outer capital wall.
 * Imported terrain, vegetation and disconnected fragments are always discarded.</p>
 */
public final class ExternalRealmBuilder {
    private static final String CITADEL_RESOURCE =
            "/data/livingkingdoms/structures/external/medieval_castle.schem";
    private static final Map<String, SpongeStructureTemplate> CACHE = new HashMap<>();
    private static final Map<String, DecodedTemplate> DECODED_CACHE = new HashMap<>();
    private static final Map<Crop, Module> MODULE_CACHE = new HashMap<>();

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

    // Crops measured from the attributed 124 x 80 x 124 castle schematic.
    private static final Crop STRAIGHT_WALL = new Crop(43, 80, 2, 38, 85, 98);
    private static final Crop ROUND_TOWER = new Crop(23, 43, 2, 58, 23, 43);
    private static final Crop GATEHOUSE = new Crop(45, 78, 2, 58, 23, 47);
    private static final List<ModulePlacement> CAPITAL_WALL = createCapitalWallPlacements();

    private ExternalRealmBuilder() {
    }

    public static IncrementalWorldEditPlan create(ServerLevel level, String homelandId,
                                                   RealmSiteLayoutSavedData.RealmSite site) {
        if (!"erden_kingdom".equals(homelandId)) {
            throw new IllegalArgumentException("Only the complete Erden kingdom slice is active: " + homelandId);
        }
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();
        DecodedTemplate decoded = decodedTemplate();
        pasteCitadel(plan, decoded, site);
        return plan;
    }

    /** Adds only the attributed outer-wall modules intersecting the currently loaded chunk. */
    public static void addCapitalWallChunk(IncrementalWorldEditPlan plan, ServerLevel level,
                                           ChunkPos chunk) {
        DecodedTemplate decoded = decodedTemplate();
        for (ModulePlacement placement : CAPITAL_WALL) {
            Module module = module(decoded, placement.crop);
            if (!placement.intersects(chunk, module)) continue;
            pasteModuleClipped(plan, level, chunk, module, placement);
        }
    }

    private static DecodedTemplate decodedTemplate() {
        return DECODED_CACHE.computeIfAbsent(CITADEL_RESOURCE, resource -> {
            SpongeStructureTemplate template = CACHE.computeIfAbsent(resource, SpongeStructureTemplate::load);
            int width = template.width();
            int height = template.height();
            int length = template.length();
            int layer = width * length;
            int volume = layer * height;

            BlockState[] palette = new BlockState[template.palette().size()];
            for (int i = 0; i < palette.length; i++) {
                palette[i] = parseState(template.palette().get(i));
            }

            BitSet candidates = new BitSet(volume);
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int paletteId = template.paletteIndex(x, y, z);
                        if (paletteId < 0 || paletteId >= palette.length) continue;
                        String id = blockId(template.palette().get(paletteId));
                        if (isSkippedSourceBlock(id) || palette[paletteId].isAir()) continue;
                        candidates.set(x + z * width + y * layer);
                    }
                }
            }
            BitSet architecture = retainStructuralComponents(candidates, width, height, length);
            if (architecture.cardinality() < 2_000) {
                throw new IllegalStateException(
                        "External citadel contains too few connected architectural blocks: "
                                + architecture.cardinality()
                );
            }
            return new DecodedTemplate(template, palette, candidates, architecture);
        });
    }

    private static void pasteCitadel(IncrementalWorldEditPlan plan, DecodedTemplate decoded,
                                     RealmSiteLayoutSavedData.RealmSite site) {
        SpongeStructureTemplate template = decoded.template;
        int width = template.width();
        int height = template.height();
        int length = template.length();
        int layer = width * length;

        int minArchitectureY = height;
        for (int index = decoded.architecture.nextSetBit(0); index >= 0;
             index = decoded.architecture.nextSetBit(index + 1)) {
            minArchitectureY = Math.min(minArchitectureY, index / layer);
        }
        if (minArchitectureY == height) minArchitectureY = 0;

        int originX = site.centerX() - width / 2;
        int originZ = site.centerZ() - length / 2;
        int originY = site.baseY() - minArchitectureY;
        long scheduled = 0L;
        for (int index = decoded.architecture.nextSetBit(0); index >= 0;
             index = decoded.architecture.nextSetBit(index + 1)) {
            int y = index / layer;
            int local = index - y * layer;
            int z = local / width;
            int x = local - z * width;
            int paletteId = template.paletteIndex(x, y, z);
            plan.addSet(originX + x, originY + y, originZ + z, decoded.palette[paletteId]);
            scheduled++;
        }

        LivingKingdoms.LOGGER.info(
                "Scheduled cleaned Erden citadel part blocks={} discarded={} origin={},{},{} dimensions={}x{}x{}",
                scheduled, decoded.candidates.cardinality() - scheduled, originX, originY, originZ,
                width, height, length
        );
    }

    private static Module module(DecodedTemplate decoded, Crop crop) {
        return MODULE_CACHE.computeIfAbsent(crop, ignored -> {
            SpongeStructureTemplate template = decoded.template;
            int width = template.width();
            int length = template.length();
            int layer = width * length;
            List<ModuleBlock> blocks = new ArrayList<>();
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (int index = decoded.architecture.nextSetBit(0); index >= 0;
                 index = decoded.architecture.nextSetBit(index + 1)) {
                int y = index / layer;
                int local = index - y * layer;
                int z = local / width;
                int x = local - z * width;
                if (!crop.contains(x, y, z)) continue;
                int paletteId = template.paletteIndex(x, y, z);
                BlockState state = decoded.palette[paletteId];
                blocks.add(new ModuleBlock(x - crop.minX, y, z - crop.minZ, state));
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            if (blocks.size() < 150) {
                throw new IllegalStateException("External architecture crop is too sparse: " + crop
                        + " blocks=" + blocks.size());
            }
            return new Module(crop.width(), crop.length(), minY, maxY, List.copyOf(blocks));
        });
    }

    private static void pasteModuleClipped(IncrementalWorldEditPlan plan, ServerLevel level,
                                           ChunkPos chunk, Module module,
                                           ModulePlacement placement) {
        int rotatedWidth = placement.rotation == Rotation.CLOCKWISE_90
                || placement.rotation == Rotation.COUNTERCLOCKWISE_90
                ? module.length : module.width;
        int rotatedLength = placement.rotation == Rotation.CLOCKWISE_90
                || placement.rotation == Rotation.COUNTERCLOCKWISE_90
                ? module.width : module.length;
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
        for (ModuleBlock block : module.blocks) {
            RotatedOffset offset = rotate(block.x, block.z, module.width, module.length,
                    placement.rotation);
            int x = originX + offset.x;
            int z = originZ + offset.z;
            if (x < minChunkX || x > maxChunkX || z < minChunkZ || z > maxChunkZ) continue;
            int y = baseY + block.y - module.minY;
            long key = columnKey(x, z);
            spans.merge(key, new VerticalSpan(x, z, y, y), VerticalSpan::merge);
            placed.add(new PlacedBlock(x, y, z, block.state.rotate(placement.rotation)));
        }
        if (placed.isEmpty()) return;

        for (VerticalSpan span : spans.values()) {
            int surfaceY = RealmSitePlanner.surfaceY(level, span.x, span.z);
            int clearTop = Math.max(surfaceY, span.maxY + 2);
            plan.addFill(span.x, span.minY, span.z, span.x, clearTop, span.z, Blocks.AIR);
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

    private static List<ModulePlacement> createCapitalWallPlacements() {
        List<ModulePlacement> placements = new ArrayList<>();
        List<Point> towers = new ArrayList<>();

        for (int x = ErdenCapitalStreamingBuilder.WEST_WALL_X;
             x <= ErdenCapitalStreamingBuilder.EAST_WALL_X; x += 240) {
            towers.add(new Point(x, ErdenCapitalStreamingBuilder.NORTH_WALL_Z));
            towers.add(new Point(x, ErdenCapitalStreamingBuilder.SOUTH_WALL_Z));
        }
        for (int z = ErdenCapitalStreamingBuilder.NORTH_WALL_Z + 225;
             z < ErdenCapitalStreamingBuilder.SOUTH_WALL_Z; z += 225) {
            towers.add(new Point(ErdenCapitalStreamingBuilder.WEST_WALL_X, z));
            towers.add(new Point(ErdenCapitalStreamingBuilder.EAST_WALL_X, z));
        }
        for (Point tower : towers) {
            placements.add(new ModulePlacement(ROUND_TOWER, tower.x, tower.z, Rotation.NONE));
        }

        placements.add(new ModulePlacement(GATEHOUSE, 0,
                ErdenCapitalStreamingBuilder.NORTH_WALL_Z, Rotation.NONE));
        placements.add(new ModulePlacement(GATEHOUSE, 0,
                ErdenCapitalStreamingBuilder.SOUTH_WALL_Z, Rotation.CLOCKWISE_180));
        placements.add(new ModulePlacement(GATEHOUSE,
                ErdenCapitalStreamingBuilder.WEST_WALL_X, 0, Rotation.COUNTERCLOCKWISE_90));
        placements.add(new ModulePlacement(GATEHOUSE,
                ErdenCapitalStreamingBuilder.EAST_WALL_X, 0, Rotation.CLOCKWISE_90));

        int northRiver = (int) Math.round(silverRiverCenterX(ErdenCapitalStreamingBuilder.NORTH_WALL_Z));
        int southRiver = (int) Math.round(silverRiverCenterX(ErdenCapitalStreamingBuilder.SOUTH_WALL_Z));
        for (int x = ErdenCapitalStreamingBuilder.WEST_WALL_X + 19;
             x <= ErdenCapitalStreamingBuilder.EAST_WALL_X - 19; x += 38) {
            if (!nearAny(towers, x, ErdenCapitalStreamingBuilder.NORTH_WALL_Z, 30)
                    && Math.abs(x) > 46 && Math.abs(x - northRiver) > 58) {
                placements.add(new ModulePlacement(STRAIGHT_WALL, x,
                        ErdenCapitalStreamingBuilder.NORTH_WALL_Z, Rotation.CLOCKWISE_180));
            }
            if (!nearAny(towers, x, ErdenCapitalStreamingBuilder.SOUTH_WALL_Z, 30)
                    && Math.abs(x) > 46 && Math.abs(x - southRiver) > 58) {
                placements.add(new ModulePlacement(STRAIGHT_WALL, x,
                        ErdenCapitalStreamingBuilder.SOUTH_WALL_Z, Rotation.NONE));
            }
        }
        for (int z = ErdenCapitalStreamingBuilder.NORTH_WALL_Z + 19;
             z <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z - 19; z += 38) {
            if (!nearAny(towers, ErdenCapitalStreamingBuilder.WEST_WALL_X, z, 30)
                    && Math.abs(z) > 46) {
                placements.add(new ModulePlacement(STRAIGHT_WALL,
                        ErdenCapitalStreamingBuilder.WEST_WALL_X, z, Rotation.CLOCKWISE_90));
            }
            if (!nearAny(towers, ErdenCapitalStreamingBuilder.EAST_WALL_X, z, 30)
                    && Math.abs(z) > 46) {
                placements.add(new ModulePlacement(STRAIGHT_WALL,
                        ErdenCapitalStreamingBuilder.EAST_WALL_X, z, Rotation.COUNTERCLOCKWISE_90));
            }
        }
        return List.copyOf(placements);
    }

    private static boolean nearAny(List<Point> points, int x, int z, int radius) {
        int squared = radius * radius;
        for (Point point : points) {
            int dx = x - point.x;
            int dz = z - point.z;
            if (dx * dx + dz * dz <= squared) return true;
        }
        return false;
    }

    private static double silverRiverCenterX(double z) {
        return -820.0 + Math.sin(z / 2_900.0) * 470.0
                + Math.sin(z / 930.0) * 105.0;
    }

    private static RotatedOffset rotate(int x, int z, int width, int length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new RotatedOffset(x, z);
            case CLOCKWISE_90 -> new RotatedOffset(length - 1 - z, x);
            case CLOCKWISE_180 -> new RotatedOffset(width - 1 - x, length - 1 - z);
            case COUNTERCLOCKWISE_90 -> new RotatedOffset(z, width - 1 - x);
        };
    }

    private static BitSet retainStructuralComponents(BitSet candidates, int width, int height, int length) {
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
        if (!id.equals(originalId)) {
            LivingKingdoms.LOGGER.debug("Migrating external schematic block id {} -> {}", originalId, id);
        }
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
        String properties = specification.substring(open + 1, close);
        for (String assignment : properties.split(",")) {
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

    private record DecodedTemplate(SpongeStructureTemplate template, BlockState[] palette,
                                   BitSet candidates, BitSet architecture) {
    }

    private record Crop(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
        int width() { return maxX - minX + 1; }
        int length() { return maxZ - minZ + 1; }
    }

    private record Module(int width, int length, int minY, int maxY,
                          List<ModuleBlock> blocks) {
    }

    private record ModuleBlock(int x, int y, int z, BlockState state) {
    }

    private record ModulePlacement(Crop crop, int centerX, int centerZ, Rotation rotation) {
        boolean intersects(ChunkPos chunk, Module module) {
            int width = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                    ? module.length : module.width;
            int length = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                    ? module.width : module.length;
            int minX = centerX - width / 2;
            int maxX = minX + width - 1;
            int minZ = centerZ - length / 2;
            int maxZ = minZ + length - 1;
            return maxX >= chunk.getMinBlockX() && minX <= chunk.getMinBlockX() + 15
                    && maxZ >= chunk.getMinBlockZ() && minZ <= chunk.getMinBlockZ() + 15;
        }
    }

    private record Point(int x, int z) {
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
