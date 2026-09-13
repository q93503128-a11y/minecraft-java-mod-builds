package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * First production-facing authored-world prototype after the M6 World Asset Gate.
 * It is still an operator harness until screenshot/playtest gates pass, but unlike the functional slice it
 * intentionally carries a locked visual hierarchy, palette family, landmark cadence and Minecraft-native sinks.
 */
public final class ProductionWorldSlicePrototypeBuilder {
    public static final String PROTOTYPE_ENTITY_TAG = "turnbound_re:production_world_slice_prototype";
    private static final int UPDATE_FLAGS = 3;

    public record Result(BlockPos origin, List<UUID> encounterAnchors) {
        public Result {
            if (origin == null || encounterAnchors == null) throw new IllegalArgumentException("origin/anchors required");
            encounterAnchors = List.copyOf(encounterAnchors);
        }
    }

    private ProductionWorldSlicePrototypeBuilder() {}

    public static Result build(ServerPlayer player, DefinitionRegistry definitions) {
        if (player == null || definitions == null) throw new IllegalArgumentException("player/definitions required");
        String dimension = player.level().dimension().identifier().toString();
        List<String> errors = new ArrayList<>(FunctionalWorldSliceLayout.validate(definitions, dimension));
        errors.addAll(ProductionWorldSlicePlan.validate());
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("; ", errors));

        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition().below();

        buildHub(level, origin);
        buildMainRoute(level, origin);
        buildMine(level, origin);
        buildFarm(level, origin);
        buildRiver(level, origin);
        buildPatrolLandmark(level, origin);
        buildEliteLandmark(level, origin);

        List<UUID> anchors = new ArrayList<>();
        anchors.add(spawnEncounterAnchor(level, origin, FunctionalWorldSliceLayout.OVERWORLD_PATROL));
        anchors.add(spawnEncounterAnchor(level, origin, FunctionalWorldSliceLayout.RIFT_ELITE));
        return new Result(origin.immutable(), anchors);
    }

    private static void buildHub(ServerLevel level, BlockPos origin) {
        clear(level, origin, -12, 12, 1, 11, -10, 10);
        fill(level, origin, -11, 11, 0, 0, -9, 9, Blocks.STONE_BRICKS.defaultBlockState());
        fill(level, origin, -10, 10, 0, 0, -8, 8, Blocks.POLISHED_TUFF.defaultBlockState());
        fill(level, origin, -10, 11, 0, 0, -2, 2, Blocks.TUFF_BRICKS.defaultBlockState());

        // Open-front forge hall: the workstation itself stays visually central and mechanically within reach.
        fill(level, origin, -5, 5, 0, 0, -5, 4, Blocks.TUFF_BRICKS.defaultBlockState());
        lowWall(level, origin, -5, 5, -5, 4, Blocks.STONE_BRICKS.defaultBlockState());
        post(level, origin, -5, -5, 1, 5, Blocks.SPRUCE_LOG.defaultBlockState());
        post(level, origin, 5, -5, 1, 5, Blocks.SPRUCE_LOG.defaultBlockState());
        post(level, origin, -5, 4, 1, 5, Blocks.SPRUCE_LOG.defaultBlockState());
        post(level, origin, 5, 4, 1, 5, Blocks.SPRUCE_LOG.defaultBlockState());
        post(level, origin, 0, -5, 1, 5, Blocks.SPRUCE_LOG.defaultBlockState());

        fill(level, origin, -6, 6, 5, 5, -6, 5, Blocks.SPRUCE_PLANKS.defaultBlockState());
        fill(level, origin, -5, 5, 6, 6, -5, 4, Blocks.SPRUCE_SLAB.defaultBlockState());
        fill(level, origin, -3, 3, 7, 7, -3, 2, Blocks.SPRUCE_SLAB.defaultBlockState());

        // Chimney and hot-work side remain offset from the eastward travel sightline.
        fill(level, origin, -4, -3, 1, 8, -4, -3, Blocks.STONE_BRICKS.defaultBlockState());
        set(level, origin.offset(-4, 1, -2), Blocks.BLAST_FURNACE.defaultBlockState());
        set(level, origin.offset(-4, 1, 0), Blocks.FURNACE.defaultBlockState());
        set(level, origin.offset(-2, 1, 0), Blocks.ANVIL.defaultBlockState());
        set(level, origin.offset(0, 1, 0), Blocks.SMITHING_TABLE.defaultBlockState());
        set(level, origin.offset(2, 1, 0), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, origin.offset(4, 1, 0), Blocks.GRINDSTONE.defaultBlockState());
        set(level, origin.offset(-1, 1, 3), Blocks.BARREL.defaultBlockState());
        set(level, origin.offset(1, 1, 3), Blocks.CHEST.defaultBlockState());

        lanternPedestal(level, origin, -7, -6);
        lanternPedestal(level, origin, 7, -6);
        lanternPedestal(level, origin, -7, 6);
        lanternPedestal(level, origin, 7, 6);

        // East gate frames the only primary exit without walling the player into the Hub.
        for (int z : new int[]{-4, 4}) {
            post(level, origin, ProductionWorldSlicePlan.HUB_GATE_X, z, 1, 4, Blocks.STONE_BRICKS.defaultBlockState());
            set(level, origin.offset(ProductionWorldSlicePlan.HUB_GATE_X, 5, z), Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void buildMainRoute(ServerLevel level, BlockPos origin) {
        clear(level, origin, 12, ProductionWorldSlicePlan.ROUTE_END_X, 1, 5, -4, 4);
        fill(level, origin, 12, ProductionWorldSlicePlan.ROUTE_END_X, 0, 0, -4, 4, Blocks.GRASS_BLOCK.defaultBlockState());
        fill(level, origin, 11, ProductionWorldSlicePlan.ROUTE_END_X, 0, 0, -1, 1, Blocks.COARSE_DIRT.defaultBlockState());
        fill(level, origin, 11, ProductionWorldSlicePlan.ROUTE_END_X, 0, 0, -2, -2, Blocks.GRAVEL.defaultBlockState());
        fill(level, origin, 11, ProductionWorldSlicePlan.ROUTE_END_X, 0, 0, 2, 2, Blocks.GRAVEL.defaultBlockState());

        for (int x = 16; x <= 72; x += ProductionWorldSlicePlan.LANTERN_SPACING) {
            set(level, origin.offset(x, 1, -4), Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, origin.offset(x, 2, -4), Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, origin.offset(x, 3, -4), Blocks.LANTERN.defaultBlockState());
        }

        // Readable short branches: resources are visible detours, not detached destinations.
        branchPath(level, origin, 40, 44, -11, -3);
        branchPath(level, origin, 40, 44, 3, 11);
        for (int x = 48; x <= 57; x++) {
            fill(level, origin, x, x, 0, 0, 11, 13, Blocks.COARSE_DIRT.defaultBlockState());
        }
    }

    private static void buildMine(ServerLevel level, BlockPos origin) {
        var site = FunctionalWorldSliceLayout.ORE_OUTCROP;
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 7, cx + 7, 1, 8, cz - 7, cz + 7);
        fill(level, origin, cx - 7, cx + 7, 0, 0, cz - 7, cz + 7, Blocks.STONE.defaultBlockState());
        fill(level, origin, cx - 6, cx, 0, 0, cz - 1, cz + 1, Blocks.COARSE_DIRT.defaultBlockState());

        // Low quarry ridge keeps the ore face visible from the branch mouth instead of becoming a cave maze.
        fill(level, origin, cx + 2, cx + 6, 1, 2, cz - 6, cz + 6, Blocks.TUFF.defaultBlockState());
        fill(level, origin, cx + 3, cx + 6, 3, 4, cz - 5, cz + 5, Blocks.ANDESITE.defaultBlockState());
        fill(level, origin, cx + 4, cx + 6, 5, 5, cz - 3, cz + 3, Blocks.STONE.defaultBlockState());
        fill(level, origin, cx - 1, cx, 1, 4, cz - 4, cz - 4, Blocks.SPRUCE_LOG.defaultBlockState());
        fill(level, origin, cx - 1, cx, 1, 4, cz + 4, cz + 4, Blocks.SPRUCE_LOG.defaultBlockState());
        fill(level, origin, cx - 1, cx, 4, 4, cz - 4, cz + 4, Blocks.SPRUCE_LOG.defaultBlockState());

        placeOre(level, origin, cx, cz, Blocks.COAL_ORE.defaultBlockState(), new int[][]{
                {2,1,-5},{2,2,-2},{2,1,3},{3,3,5},{4,1,-4},{4,4,1},{5,2,4},{6,1,-1}
        });
        placeOre(level, origin, cx, cz, Blocks.COPPER_ORE.defaultBlockState(), new int[][]{
                {2,1,-3},{2,2,0},{2,2,5},{3,1,2},{3,3,-4},{3,4,3},{4,2,-5},{4,3,-1},{5,1,2},{6,2,5}
        });
        placeOre(level, origin, cx, cz, Blocks.IRON_ORE.defaultBlockState(), new int[][]{
                {2,2,-5},{2,1,1},{3,2,-2},{3,4,1},{4,1,5},{4,3,4},{5,2,-3},{6,3,2}
        });
        placeOre(level, origin, cx, cz, Blocks.GOLD_ORE.defaultBlockState(), new int[][]{
                {3,1,-5},{4,4,-3},{5,3,1},{6,2,3}
        });
        set(level, origin.offset(cx - 2, 1, cz + 4), Blocks.BARREL.defaultBlockState());
        set(level, origin.offset(cx - 2, 1, cz - 4), Blocks.CAMPFIRE.defaultBlockState());
    }

    private static void buildFarm(ServerLevel level, BlockPos origin) {
        var site = FunctionalWorldSliceLayout.RIVERSIDE_PLOT;
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 7, cx + 7, 1, 5, cz - 7, cz + 7);
        fill(level, origin, cx - 7, cx + 7, 0, 0, cz - 7, cz + 7, Blocks.GRASS_BLOCK.defaultBlockState());
        fill(level, origin, cx - 1, cx + 1, 0, 0, cz - 7, cz - 5, Blocks.COARSE_DIRT.defaultBlockState());

        BlockState matureCarrot = Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7);
        BlockState matureWheat = Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7);
        for (int x = cx - 5; x <= cx + 5; x++) {
            set(level, origin.offset(x, 0, cz), Blocks.WATER.defaultBlockState());
            for (int z = cz - 5; z <= cz + 5; z++) {
                if (z == cz) continue;
                set(level, origin.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState());
                set(level, origin.offset(x, 1, z), z < cz ? matureCarrot : matureWheat);
            }
        }

        for (int x = cx - 7; x <= cx + 7; x++) {
            set(level, origin.offset(x, 1, cz - 7), Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, origin.offset(x, 1, cz + 7), Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        for (int z = cz - 6; z <= cz + 6; z++) {
            set(level, origin.offset(cx - 7, 1, z), Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, origin.offset(cx + 7, 1, z), Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        set(level, origin.offset(cx + 6, 1, cz + 5), Blocks.COMPOSTER.defaultBlockState());
        set(level, origin.offset(cx + 6, 1, cz + 3), Blocks.BARREL.defaultBlockState());
        set(level, origin.offset(cx - 6, 1, cz - 5), Blocks.HAY_BLOCK.defaultBlockState());
    }

    private static void buildRiver(ServerLevel level, BlockPos origin) {
        var site = FunctionalWorldSliceLayout.RIVER_POOL;
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 8, cx + 8, -1, 6, cz - 7, cz + 7);
        fill(level, origin, cx - 8, cx + 8, -2, -2, cz - 7, cz + 7, Blocks.GRAVEL.defaultBlockState());
        fill(level, origin, cx - 8, cx + 8, 0, 0, cz - 7, cz + 7, Blocks.GRASS_BLOCK.defaultBlockState());

        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int shape = dx * dx * 3 + dz * dz * 4;
                if (shape > 147) continue;
                set(level, origin.offset(cx + dx, -1, cz + dz), Blocks.SAND.defaultBlockState());
                set(level, origin.offset(cx + dx, 0, cz + dz), Blocks.WATER.defaultBlockState());
            }
        }

        // Small dock makes the fishing purpose obvious while keeping the water itself vanilla.
        fill(level, origin, cx - 8, cx - 3, 1, 1, cz - 1, cz + 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        post(level, origin, cx - 7, cz - 2, 1, 2, Blocks.SPRUCE_FENCE.defaultBlockState());
        post(level, origin, cx - 7, cz + 2, 1, 2, Blocks.SPRUCE_FENCE.defaultBlockState());
        set(level, origin.offset(cx - 7, 3, cz - 2), Blocks.LANTERN.defaultBlockState());
        set(level, origin.offset(cx - 7, 3, cz + 2), Blocks.LANTERN.defaultBlockState());
        set(level, origin.offset(cx - 5, 1, cz + 3), Blocks.BARREL.defaultBlockState());
    }

    private static void buildPatrolLandmark(ServerLevel level, BlockPos origin) {
        var site = FunctionalWorldSliceLayout.OVERWORLD_PATROL;
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 4, cx + 4, 1, 6, cz - 4, cz + 4);
        fill(level, origin, cx - 4, cx + 4, 0, 0, cz - 4, cz + 4, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        fill(level, origin, cx - 2, cx + 2, 0, 0, cz - 2, cz + 2, Blocks.GRAVEL.defaultBlockState());
        post(level, origin, cx - 3, cz - 3, 1, 4, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        post(level, origin, cx - 3, cz + 3, 1, 2, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        post(level, origin, cx + 3, cz - 3, 1, 2, Blocks.STONE_BRICKS.defaultBlockState());
        post(level, origin, cx + 3, cz + 3, 1, 4, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        set(level, origin.offset(cx - 2, 1, cz), Blocks.CAMPFIRE.defaultBlockState());
        set(level, origin.offset(cx, 1, cz), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
    }

    private static void buildEliteLandmark(ServerLevel level, BlockPos origin) {
        var site = FunctionalWorldSliceLayout.RIFT_ELITE;
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 5, cx + 5, 1, 8, cz - 5, cz + 5);
        fill(level, origin, cx - 5, cx + 5, 0, 0, cz - 5, cz + 5, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, origin, cx - 3, cx + 3, 0, 0, cz - 3, cz + 3, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        for (int[] corner : new int[][]{{-4,-4},{-4,4},{4,-4},{4,4}}) {
            post(level, origin, cx + corner[0], cz + corner[1], 1, 5, Blocks.OBSIDIAN.defaultBlockState());
            set(level, origin.offset(cx + corner[0], 6, cz + corner[1]), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        }
        for (int[] side : new int[][]{{-3,0},{3,0},{0,-3},{0,3}}) {
            set(level, origin.offset(cx + side[0], 1, cz + side[1]), Blocks.SOUL_CAMPFIRE.defaultBlockState());
        }
        set(level, origin.offset(cx, 1, cz), Blocks.AMETHYST_BLOCK.defaultBlockState());
        set(level, origin.offset(cx - 1, 1, cz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        set(level, origin.offset(cx + 1, 1, cz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
    }

    private static UUID spawnEncounterAnchor(
            ServerLevel level,
            BlockPos origin,
            FunctionalWorldSliceLayout.Site site
    ) {
        Interaction anchor = new Interaction(EntityTypes.INTERACTION, level);
        anchor.setPos(
                origin.getX() + site.offsetX() + 0.5D,
                origin.getY() + 1.75D,
                origin.getZ() + site.offsetZ() + 0.5D);
        anchor.entityTags().add(WorldEncounterAnchorResolver.tagFor(site.locator()));
        anchor.entityTags().add(PROTOTYPE_ENTITY_TAG);
        level.addFreshEntity(anchor);
        return anchor.getUUID();
    }

    private static void branchPath(ServerLevel level, BlockPos origin, int minX, int maxX, int minZ, int maxZ) {
        int fromZ = Math.min(minZ, maxZ);
        int toZ = Math.max(minZ, maxZ);
        fill(level, origin, minX, maxX, 0, 0, fromZ, toZ, Blocks.COARSE_DIRT.defaultBlockState());
        for (int z = fromZ; z <= toZ; z += 4) {
            set(level, origin.offset(minX, 0, z), Blocks.GRAVEL.defaultBlockState());
            set(level, origin.offset(maxX, 0, z), Blocks.GRAVEL.defaultBlockState());
        }
    }

    private static void lowWall(
            ServerLevel level,
            BlockPos origin,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            BlockState state
    ) {
        for (int x = minX; x <= maxX; x++) {
            if (x < 3 || x > 5) set(level, origin.offset(x, 1, minZ), state);
            set(level, origin.offset(x, 1, maxZ), state);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            set(level, origin.offset(minX, 1, z), state);
            if (z < -1 || z > 1) set(level, origin.offset(maxX, 1, z), state);
        }
    }

    private static void lanternPedestal(ServerLevel level, BlockPos origin, int x, int z) {
        set(level, origin.offset(x, 1, z), Blocks.STONE_BRICK_WALL.defaultBlockState());
        set(level, origin.offset(x, 2, z), Blocks.LANTERN.defaultBlockState());
    }

    private static void placeOre(
            ServerLevel level,
            BlockPos origin,
            int cx,
            int cz,
            BlockState ore,
            int[][] positions
    ) {
        for (int[] p : positions) set(level, origin.offset(cx + p[0], p[1], cz + p[2]), ore);
    }

    private static void post(
            ServerLevel level,
            BlockPos origin,
            int x,
            int z,
            int minY,
            int maxY,
            BlockState state
    ) {
        fill(level, origin, x, x, minY, maxY, z, z, state);
    }

    private static void clear(
            ServerLevel level,
            BlockPos origin,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
        fill(level, origin, minX, maxX, minY, maxY, minZ, maxZ, Blocks.AIR.defaultBlockState());
    }

    private static void fill(
            ServerLevel level,
            BlockPos origin,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            BlockState state
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    set(level, origin.offset(x, y, z), state);
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, UPDATE_FLAGS);
    }
}
