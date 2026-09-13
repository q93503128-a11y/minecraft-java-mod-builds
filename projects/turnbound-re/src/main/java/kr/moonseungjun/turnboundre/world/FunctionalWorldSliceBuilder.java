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
 * Builds the disposable M6 functional world slice used before production world art is locked.
 * It deliberately uses only vanilla blocks and the stable authored locator contract.
 */
public final class FunctionalWorldSliceBuilder {
    public static final String SLICE_ENTITY_TAG = "turnbound_re:functional_world_slice";
    private static final int UPDATE_FLAGS = 3;

    public record Result(BlockPos origin, List<UUID> encounterAnchors) {
        public Result {
            if (origin == null || encounterAnchors == null) throw new IllegalArgumentException("origin/anchors required");
            encounterAnchors = List.copyOf(encounterAnchors);
        }
    }

    private FunctionalWorldSliceBuilder() {}

    public static Result build(ServerPlayer player, DefinitionRegistry definitions) {
        if (player == null || definitions == null) throw new IllegalArgumentException("player/definitions required");
        String dimension = player.level().dimension().identifier().toString();
        List<String> errors = FunctionalWorldSliceLayout.validate(definitions, dimension);
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("; ", errors));

        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition().below();

        buildHub(level, origin);
        buildRoute(level, origin);
        buildMine(level, origin);
        buildFarm(level, origin);
        buildRiver(level, origin);
        buildEncounterLandmark(level, origin, FunctionalWorldSliceLayout.OVERWORLD_PATROL, false);
        buildEncounterLandmark(level, origin, FunctionalWorldSliceLayout.RIFT_ELITE, true);

        List<UUID> anchors = new ArrayList<>();
        anchors.add(spawnEncounterAnchor(level, origin, FunctionalWorldSliceLayout.OVERWORLD_PATROL));
        anchors.add(spawnEncounterAnchor(level, origin, FunctionalWorldSliceLayout.RIFT_ELITE));
        return new Result(origin.immutable(), anchors);
    }

    private static void buildHub(ServerLevel level, BlockPos origin) {
        clear(level, origin, -7, 7, 1, 5, -7, 7);
        fill(level, origin, -6, 6, 0, 0, -6, 6, Blocks.STONE_BRICKS.defaultBlockState());
        set(level, origin.offset(0, 1, 0), Blocks.SMITHING_TABLE.defaultBlockState());
        set(level, origin.offset(-2, 1, 0), Blocks.FURNACE.defaultBlockState());
        set(level, origin.offset(2, 1, 0), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, origin.offset(0, 1, -3), Blocks.CHEST.defaultBlockState());

        // A readable exit mouth without pretending this temporary palette is production art.
        fill(level, origin, 5, 6, 1, 2, -4, -3, Blocks.STONE_BRICKS.defaultBlockState());
        fill(level, origin, 5, 6, 1, 2, 3, 4, Blocks.STONE_BRICKS.defaultBlockState());
    }

    private static void buildRoute(ServerLevel level, BlockPos origin) {
        clear(level, origin, 7, 76, 1, 3, -2, 2);
        fill(level, origin, 7, 76, 0, 0, -1, 1, Blocks.COBBLESTONE.defaultBlockState());
        for (int x = 12; x <= 72; x += 10) {
            set(level, origin.offset(x, 1, -2), Blocks.TORCH.defaultBlockState());
        }
    }

    private static void buildMine(ServerLevel level, BlockPos origin) {
        FunctionalWorldSliceLayout.Site site = FunctionalWorldSliceLayout.ORE_OUTCROP;
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 7, cx + 7, 1, 6, cz - 7, cz + 7);
        fill(level, origin, cx - 6, cx + 6, 0, 0, cz - 6, cz + 6, Blocks.STONE.defaultBlockState());

        // Compact exposed quarry face: the player still breaks real ore blocks and must smelt metal at the Hub.
        fill(level, origin, cx + 1, cx + 6, 1, 4, cz - 4, cz + 4, Blocks.STONE.defaultBlockState());
        BlockState[] ores = {
                Blocks.COAL_ORE.defaultBlockState(),
                Blocks.COPPER_ORE.defaultBlockState(),
                Blocks.IRON_ORE.defaultBlockState(),
                Blocks.COPPER_ORE.defaultBlockState(),
                Blocks.IRON_ORE.defaultBlockState(),
                Blocks.GOLD_ORE.defaultBlockState(),
                Blocks.COAL_ORE.defaultBlockState(),
                Blocks.COPPER_ORE.defaultBlockState(),
                Blocks.IRON_ORE.defaultBlockState()
        };
        int[][] positions = {
                {1, 1, -3}, {1, 2, -1}, {1, 1, 1}, {1, 3, 3}, {2, 2, -3},
                {2, 1, 0}, {2, 3, 2}, {3, 1, -2}, {3, 2, 2}
        };
        for (int i = 0; i < positions.length; i++) {
            int[] p = positions[i];
            set(level, origin.offset(cx + p[0], p[1], cz + p[2]), ores[i]);
        }
        fill(level, origin, cx - 6, cx, 0, 0, cz - 1, cz + 1, Blocks.COBBLESTONE.defaultBlockState());
    }

    private static void buildFarm(ServerLevel level, BlockPos origin) {
        FunctionalWorldSliceLayout.Site site = FunctionalWorldSliceLayout.RIVERSIDE_PLOT;
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 7, cx + 7, 1, 4, cz - 7, cz + 7);
        fill(level, origin, cx - 6, cx + 6, 0, 0, cz - 6, cz + 6, Blocks.DIRT.defaultBlockState());

        BlockState matureCarrot = Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7);
        BlockState matureWheat = Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7);
        for (int x = cx - 5; x <= cx + 5; x++) {
            set(level, origin.offset(x, 0, cz), Blocks.WATER.defaultBlockState());
            for (int z = cz - 4; z <= cz + 4; z++) {
                if (z == cz) continue;
                set(level, origin.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState());
                set(level, origin.offset(x, 1, z), z < cz ? matureCarrot : matureWheat);
            }
        }
        fill(level, origin, cx - 6, cx + 6, 0, 0, cz - 6, cz - 5, Blocks.OAK_PLANKS.defaultBlockState());
    }

    private static void buildRiver(ServerLevel level, BlockPos origin) {
        FunctionalWorldSliceLayout.Site site = FunctionalWorldSliceLayout.RIVER_POOL;
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 7, cx + 7, -1, 5, cz - 7, cz + 7);

        fill(level, origin, cx - 6, cx + 6, -2, -2, cz - 6, cz + 6, Blocks.SAND.defaultBlockState());
        fill(level, origin, cx - 5, cx + 5, -1, 0, cz - 5, cz + 5, Blocks.WATER.defaultBlockState());
        for (int x = cx - 6; x <= cx + 6; x++) {
            for (int z = cz - 6; z <= cz + 6; z++) {
                if (Math.abs(x - cx) <= 5 && Math.abs(z - cz) <= 5) continue;
                set(level, origin.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        fill(level, origin, cx - 7, cx - 5, 0, 0, cz - 1, cz + 1, Blocks.COBBLESTONE.defaultBlockState());
    }

    private static void buildEncounterLandmark(
            ServerLevel level,
            BlockPos origin,
            FunctionalWorldSliceLayout.Site site,
            boolean elite
    ) {
        int cx = site.offsetX();
        int cz = site.offsetZ();
        clear(level, origin, cx - 3, cx + 3, 1, 4, cz - 3, cz + 3);
        fill(level, origin, cx - 3, cx + 3, 0, 0, cz - 3, cz + 3,
                elite ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        set(level, origin.offset(cx - 2, 1, cz), Blocks.STONE_BRICK_WALL.defaultBlockState());
        set(level, origin.offset(cx + 2, 1, cz), Blocks.STONE_BRICK_WALL.defaultBlockState());
        set(level, origin.offset(cx, 1, cz),
                elite ? Blocks.SOUL_CAMPFIRE.defaultBlockState() : Blocks.CAMPFIRE.defaultBlockState());
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
        anchor.entityTags().add(SLICE_ENTITY_TAG);
        level.addFreshEntity(anchor);
        return anchor.getUUID();
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
