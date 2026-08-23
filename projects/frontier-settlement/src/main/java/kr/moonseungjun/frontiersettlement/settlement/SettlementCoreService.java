package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SettlementCoreService {
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int MAX_PLACEMENTS_PER_TICK = 6;
    private static final Set<Block> CORE_BLOCKS = Set.of(
            Blocks.COARSE_DIRT, Blocks.DIRT_PATH, Blocks.STONE_BRICKS,
            Blocks.POLISHED_ANDESITE, Blocks.CHISELED_STONE_BRICKS,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.OAK_FENCE, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_OAK_LOG,
            Blocks.STRIPPED_DARK_OAK_LOG, Blocks.LANTERN, Blocks.CAMPFIRE);

    private SettlementCoreService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (!data.founded() || server.getTickCount() % 10 != 0) return;
        ServerLevel level = server.overworld();
        if (!level.hasChunkAt(data.centerPos())) return;

        int changed = 0;
        for (Placement placement : desired(data)) {
            if (changed >= MAX_PLACEMENTS_PER_TICK) break;
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!canSafelyReplace(current, placement.floor())) continue;
            level.setBlock(placement.pos(), placement.state(), DIRECT_BLOCK_UPDATE);
            changed++;
        }
    }

    private static List<Placement> desired(SettlementData data) {
        // The civic core is anchored to the physical pioneer marker. The stockpile may sit a few
        // blocks away and remains an ordinary physical container rather than becoming the town center.
        BlockPos center = data.centerPos();
        LinkedHashMap<BlockPos, Placement> placements = new LinkedHashMap<>();
        SettlementTier tier = SettlementTier.current(data);

        addFloor(placements, center, 1, Blocks.COARSE_DIRT.defaultBlockState());
        addCampMarker(placements, center);

        if (tier.ordinal() >= SettlementTier.HAMLET.ordinal()) {
            addFloor(placements, center, 2, Blocks.DIRT_PATH.defaultBlockState());
            addLampRing(placements, center, 2, 3, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        }
        if (tier.ordinal() >= SettlementTier.VILLAGE.ordinal()) {
            addFloor(placements, center, 3, Blocks.STONE_BRICKS.defaultBlockState());
            addCross(placements, center, 3, Blocks.POLISHED_ANDESITE.defaultBlockState());
            addLampRing(placements, center, 3, 3, Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        }
        if (tier.ordinal() >= SettlementTier.FRONTIER_TOWN.ordinal()) {
            addFloor(placements, center, 4, Blocks.STONE_BRICKS.defaultBlockState());
            addCross(placements, center, 4, Blocks.POLISHED_ANDESITE.defaultBlockState());
            addCornerAccents(placements, center, 4, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            addLampRing(placements, center, 4, 4, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
        }
        if (tier.ordinal() >= SettlementTier.DOMAIN.ordinal()) {
            addFloor(placements, center, 5, Blocks.STONE_BRICKS.defaultBlockState());
            addCross(placements, center, 5, Blocks.POLISHED_ANDESITE.defaultBlockState());
            addCornerAccents(placements, center, 5, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            addLampRing(placements, center, 5, 4, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
        }
        return new ArrayList<>(placements.values());
    }

    private static void addFloor(Map<BlockPos, Placement> out, BlockPos c, int radius, BlockState state) {
        int y = c.getY() - 1;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                put(out, new BlockPos(c.getX() + x, y, c.getZ() + z), state, true);
            }
        }
    }

    private static void addCross(Map<BlockPos, Placement> out, BlockPos c, int radius, BlockState state) {
        int y = c.getY() - 1;
        for (int d = -radius; d <= radius; d++) {
            put(out, new BlockPos(c.getX() + d, y, c.getZ()), state, true);
            put(out, new BlockPos(c.getX(), y, c.getZ() + d), state, true);
        }
    }

    private static void addCornerAccents(Map<BlockPos, Placement> out, BlockPos c, int radius, BlockState state) {
        int y = c.getY() - 1;
        int[][] corners = {{radius, radius}, {radius, -radius}, {-radius, radius}, {-radius, -radius}};
        for (int[] p : corners) put(out, c.offset(p[0], -1, p[1]), state, true);
    }

    private static void addCampMarker(Map<BlockPos, Placement> out, BlockPos c) {
        // The actual pioneer marker occupies c and is deliberately never replaced here.
        put(out, c.offset(-1, 0, 0), Blocks.CAMPFIRE.defaultBlockState(), false);
        int[][] posts = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
        for (int[] p : posts) {
            put(out, c.offset(p[0], 0, p[1]), Blocks.OAK_FENCE.defaultBlockState(), false);
            put(out, c.offset(p[0], 1, p[1]), Blocks.LANTERN.defaultBlockState(), false);
        }
    }

    private static void addLampRing(Map<BlockPos, Placement> out, BlockPos c, int radius, int height, BlockState post) {
        int[][] corners = {{radius, radius}, {radius, -radius}, {-radius, radius}, {-radius, -radius}};
        for (int[] p : corners) {
            for (int y = 0; y < height; y++) put(out, c.offset(p[0], y, p[1]), post, false);
            put(out, c.offset(p[0], height, p[1]), Blocks.LANTERN.defaultBlockState(), false);
        }
    }

    private static void put(Map<BlockPos, Placement> out, BlockPos pos, BlockState state, boolean floor) {
        out.put(pos, new Placement(pos, state, floor));
    }

    private static boolean canSafelyReplace(BlockState current, boolean floor) {
        if (current.isAir() || current.canBeReplaced() || CORE_BLOCKS.contains(current.getBlock())) return true;
        return floor && isNaturalGround(current);
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.STONE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK);
    }

    private record Placement(BlockPos pos, BlockState state, boolean floor) {}
}
