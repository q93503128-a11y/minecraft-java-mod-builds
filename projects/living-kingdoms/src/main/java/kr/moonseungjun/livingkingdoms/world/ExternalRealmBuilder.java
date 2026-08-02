package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Imports licensed architecture as reusable world parts.
 *
 * <p>The source schematic is treated as an inner citadel, never as the complete capital. Imported
 * terrain, vegetation and tiny disconnected fragments are discarded so the authored continent owns
 * the ground, roads and ecology.</p>
 */
public final class ExternalRealmBuilder {
    private static final Map<String, SpongeStructureTemplate> CACHE = new HashMap<>();
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

    private ExternalRealmBuilder() {
    }

    public static IncrementalWorldEditPlan create(ServerLevel level, String homelandId,
                                                   RealmSiteLayoutSavedData.RealmSite site) {
        if (!"erden_kingdom".equals(homelandId)) {
            throw new IllegalArgumentException("Only the complete Erden kingdom slice is active: " + homelandId);
        }
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();
        String resource = "/data/livingkingdoms/structures/external/medieval_castle.schem";
        SpongeStructureTemplate template = CACHE.computeIfAbsent(resource, SpongeStructureTemplate::load);
        pasteCitadel(plan, template, site);
        addTerrainIntegratedCapitalLinks(plan, level, site, template);
        return plan;
    }

    private static void pasteCitadel(IncrementalWorldEditPlan plan, SpongeStructureTemplate template,
                                     RealmSiteLayoutSavedData.RealmSite site) {
        int width = template.width();
        int height = template.height();
        int length = template.length();
        int layer = width * length;
        int volume = layer * height;

        BlockState[] palette = new BlockState[template.palette().size()];
        for (int i = 0; i < palette.length; i++) palette[i] = parseState(template.palette().get(i));

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
            throw new IllegalStateException("External citadel contains too few connected architectural blocks: "
                    + architecture.cardinality());
        }

        int minArchitectureY = height;
        for (int index = architecture.nextSetBit(0); index >= 0; index = architecture.nextSetBit(index + 1)) {
            minArchitectureY = Math.min(minArchitectureY, index / layer);
        }
        if (minArchitectureY == height) minArchitectureY = 0;

        int originX = site.centerX() - width / 2;
        int originZ = site.centerZ() - length / 2;
        int originY = site.baseY() - minArchitectureY;
        long scheduled = 0L;
        for (int index = architecture.nextSetBit(0); index >= 0; index = architecture.nextSetBit(index + 1)) {
            int y = index / layer;
            int local = index - y * layer;
            int z = local / width;
            int x = local - z * width;
            int paletteId = template.paletteIndex(x, y, z);
            BlockState state = palette[paletteId];
            plan.addSet(originX + x, originY + y, originZ + z, state);
            scheduled++;
        }

        LivingKingdoms.LOGGER.info(
                "Scheduled cleaned Erden citadel part blocks={} discarded={} origin={},{},{} dimensions={}x{}x{}",
                scheduled, candidates.cardinality() - scheduled, originX, originY, originZ,
                width, height, length
        );
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

    private static void addTerrainIntegratedCapitalLinks(IncrementalWorldEditPlan plan,
                                                          ServerLevel level,
                                                          RealmSiteLayoutSavedData.RealmSite site,
                                                          SpongeStructureTemplate template) {
        int halfX = template.width() / 2 + 10;
        int halfZ = template.length() / 2 + 10;
        Block road = Blocks.PACKED_MUD;
        line(plan, level, site.centerX(), site.centerZ() - halfZ - 720,
                site.centerX(), site.centerZ() + halfZ + 720, road, 3);
        line(plan, level, site.centerX() - halfX - 720, site.centerZ(),
                site.centerX() + halfX + 720, site.centerZ(), road, 3);
    }

    private static void line(IncrementalWorldEditPlan plan, ServerLevel level,
                             int x1, int z1, int x2, int z2, Block road, int halfWidth) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        boolean eastWest = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -halfWidth; side <= halfWidth; side++) {
                int px = eastWest ? x : x + side;
                int pz = eastWest ? z + side : z;
                int surfaceY = RealmSitePlanner.surfaceY(level, px, pz);
                plan.addSet(px, surfaceY, pz, road);
                plan.addFill(px, surfaceY + 1, pz, px, surfaceY + 3, pz, Blocks.AIR);
            }
        }
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
}
