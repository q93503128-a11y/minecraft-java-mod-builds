package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Builds capital cores from attributed external schematics instead of procedural box buildings. */
public final class ExternalRealmBuilder {
    private static final Map<String, SpongeStructureTemplate> CACHE = new HashMap<>();
    private static final Set<String> SKIPPED_TERRAIN = Set.of(
            "minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void",
            "minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt"
    );

    private ExternalRealmBuilder() {
    }

    public static IncrementalWorldEditPlan create(ServerLevel level, String homelandId,
                                                   RealmSiteLayoutSavedData.RealmSite site) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();
        String resource = switch (homelandId) {
            case "silvana_forest" -> "/data/livingkingdoms/structures/external/woodland_mansion.schem";
            case "kardum_league" -> "/data/livingkingdoms/structures/external/deepslate_mega_base.schem";
            default -> "/data/livingkingdoms/structures/external/medieval_castle.schem";
        };
        SpongeStructureTemplate template = CACHE.computeIfAbsent(resource, SpongeStructureTemplate::load);
        paste(plan, template, site, homelandId);
        addLivingZoneLinks(plan, site, homelandId, template);
        return plan;
    }

    private static void paste(IncrementalWorldEditPlan plan, SpongeStructureTemplate template,
                              RealmSiteLayoutSavedData.RealmSite site, String homelandId) {
        BlockState[] palette = new BlockState[template.palette().size()];
        for (int i = 0; i < palette.length; i++) palette[i] = parseState(template.palette().get(i));

        int originX = site.centerX() - template.width() / 2;
        int originZ = site.centerZ() - template.length() / 2;
        int minArchitectureY = findMinimumArchitectureY(template);
        int originY = site.baseY() - minArchitectureY;
        long scheduled = 0L;
        for (int y = 0; y < template.height(); y++) {
            for (int z = 0; z < template.length(); z++) {
                for (int x = 0; x < template.width(); x++) {
                    int paletteId = template.paletteIndex(x, y, z);
                    if (paletteId < 0 || paletteId >= palette.length) continue;
                    String id = blockId(template.palette().get(paletteId));
                    if (SKIPPED_TERRAIN.contains(id)) continue;
                    BlockState state = palette[paletteId];
                    if (state.isAir()) continue;
                    plan.addSet(originX + x, originY + y, originZ + z,
                            remapForHomeland(state, homelandId));
                    scheduled++;
                }
            }
        }
        if (scheduled < 2_000L) {
            throw new IllegalStateException("External structure contains too few architectural blocks: " + scheduled);
        }
        LivingKingdoms.LOGGER.info(
                "Scheduled external capital template homeland={} resource_blocks={} origin={},{},{} dimensions={}x{}x{}",
                homelandId, scheduled, originX, originY, originZ,
                template.width(), template.height(), template.length()
        );
    }

    private static int findMinimumArchitectureY(SpongeStructureTemplate template) {
        int minimum = template.height();
        for (int y = 0; y < template.height(); y++) {
            for (int z = 0; z < template.length(); z++) {
                for (int x = 0; x < template.width(); x++) {
                    int paletteId = template.paletteIndex(x, y, z);
                    if (paletteId < 0 || paletteId >= template.palette().size()) continue;
                    String id = blockId(template.palette().get(paletteId));
                    if (!SKIPPED_TERRAIN.contains(id)) minimum = Math.min(minimum, y);
                }
            }
        }
        return minimum == template.height() ? 0 : minimum;
    }

    private static void addLivingZoneLinks(IncrementalWorldEditPlan plan,
                                           RealmSiteLayoutSavedData.RealmSite site,
                                           String homelandId,
                                           SpongeStructureTemplate template) {
        int y = site.baseY();
        Block road = switch (homelandId) {
            case "silvana_forest" -> Blocks.ROOTED_DIRT;
            case "kardum_league" -> Blocks.POLISHED_ANDESITE;
            default -> Blocks.PACKED_MUD;
        };
        int halfX = template.width() / 2 + 8;
        int halfZ = template.length() / 2 + 8;
        line(plan, site.centerX(), y, site.centerZ() - halfZ - 90,
                site.centerX(), site.centerZ() + halfZ + 90, road);
        line(plan, site.centerX() - halfX - 90, y, site.centerZ(),
                site.centerX() + halfX + 90, site.centerZ(), road);
    }

    private static void line(IncrementalWorldEditPlan plan, int x1, int y, int z1,
                             int x2, int z2, Block road) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -2; side <= 2; side++) {
                int px = Math.abs(x2 - x1) >= Math.abs(z2 - z1) ? x : x + side;
                int pz = Math.abs(x2 - x1) >= Math.abs(z2 - z1) ? z + side : z;
                plan.addSet(px, y, pz, road);
                plan.addFill(px, y + 1, pz, px, y + 3, pz, Blocks.AIR);
            }
        }
    }

    private static BlockState parseState(String specification) {
        String id = blockId(specification);
        Identifier key = Identifier.parse(id);
        Block block = BuiltInRegistries.BLOCK.getValue(key);
        if (block == null || block == Blocks.AIR && !"minecraft:air".equals(id)) {
            throw new IllegalStateException("Unknown external schematic block " + id);
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

    private static BlockState remapForHomeland(BlockState state, String homelandId) {
        Block block = state.getBlock();
        if ("silvana_forest".equals(homelandId)) {
            if (block == Blocks.STONE_BRICKS) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            if (block == Blocks.OAK_PLANKS) return Blocks.DARK_OAK_PLANKS.defaultBlockState();
        } else if ("kardum_league".equals(homelandId)) {
            if (block == Blocks.STONE_BRICKS) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            if (block == Blocks.COBBLESTONE) return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            if (block == Blocks.OAK_PLANKS) return Blocks.SPRUCE_PLANKS.defaultBlockState();
        }
        return state;
    }

    private static String blockId(String specification) {
        int bracket = specification.indexOf('[');
        return (bracket < 0 ? specification : specification.substring(0, bracket)).trim();
    }
}
