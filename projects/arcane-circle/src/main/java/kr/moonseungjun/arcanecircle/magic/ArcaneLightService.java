package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Temporary server-authoritative illumination for the Light spell. */
public final class ArcaneLightService {
    private static final int LIGHT_LEVEL = 15;
    private static final int REFRESH_INTERVAL = 4;
    private static final int[][] OFFSETS = {{0,1,0},{3,1,0},{-3,1,0},{0,1,3},{0,1,-3}};
    private static final Map<UUID, LightState> ACTIVE = new HashMap<>();
    /** Only LightBlocks created by this service are ref-counted and therefore eligible for removal. */
    private static final Map<LightKey, Integer> REF_COUNTS = new HashMap<>();

    private ArcaneLightService() {}

    public static void illuminate(ServerPlayer player, int durationTicks) {
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        LightState state = ACTIVE.computeIfAbsent(player.getUUID(), ignored -> new LightState());
        state.untilTick = Math.max(state.untilTick, now + Math.max(20, durationTicks));
        refresh(player, state);
    }

    public static void tick(ServerPlayer player) {
        LightState state = ACTIVE.get(player.getUUID());
        if (state == null) return;
        ServerLevel level = (ServerLevel) player.level();
        if (level.getGameTime() >= state.untilTick) {
            clear(player, state);
            ACTIVE.remove(player.getUUID());
            return;
        }
        if (player.tickCount % REFRESH_INTERVAL == 0) refresh(player, state);
    }

    public static void clear(ServerPlayer player) {
        LightState state = ACTIVE.remove(player.getUUID());
        if (state != null) clear(player, state);
    }

    public static void clearAll(MinecraftServer server) {
        for (LightState state : ACTIVE.values()) clear(server, state);
        ACTIVE.clear();
        // Defensive cleanup: a balanced lifecycle should already have reduced every claim to zero.
        for (LightKey key : Set.copyOf(REF_COUNTS.keySet())) forceRemoveOwned(server, key);
        REF_COUNTS.clear();
    }

    private static void refresh(ServerPlayer player, LightState state) {
        ServerLevel level = (ServerLevel) player.level();
        MinecraftServer server = level.getServer();
        if (state.dimension != null && !state.dimension.equals(level.dimension())) {
            clear(server, state);
            state.positions.clear();
        }
        state.dimension = level.dimension();
        BlockPos base = player.blockPosition();
        Set<BlockPos> desired = new HashSet<>();
        for (int[] off : OFFSETS) desired.add(base.offset(off[0], off[1], off[2]).immutable());

        for (BlockPos pos : Set.copyOf(state.positions)) {
            if (!desired.contains(pos)) {
                release(server, new LightKey(state.dimension, pos));
                state.positions.remove(pos);
                continue;
            }
            // An external block edit can replace a temporary light. Drop our stale ownership and
            // reacquire only if the cell becomes eligible again; never delete the replacement.
            if (!level.getBlockState(pos).is(Blocks.LIGHT)) {
                release(server, new LightKey(state.dimension, pos));
                state.positions.remove(pos);
            }
        }

        for (BlockPos pos : desired) {
            if (state.positions.contains(pos)) continue;
            if (claim(level, pos)) state.positions.add(pos);
        }
    }

    private static boolean claim(ServerLevel level, BlockPos pos) {
        LightKey key = new LightKey(level.dimension(), pos.immutable());
        Integer count = REF_COUNTS.get(key);
        if (count != null) {
            if (level.getBlockState(pos).is(Blocks.LIGHT)) {
                REF_COUNTS.put(key, count + 1);
                return true;
            }
            REF_COUNTS.remove(key);
        }

        // A LightBlock not present in REF_COUNTS belongs to vanilla/another system and is untouched.
        if (!level.getBlockState(pos).isAir()) return false;
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL);
        if (!level.setBlock(pos, light, 3)) return false;
        REF_COUNTS.put(key, 1);
        return true;
    }

    private static void release(MinecraftServer server, LightKey key) {
        Integer count = REF_COUNTS.get(key);
        if (count == null) return;
        if (count > 1) {
            REF_COUNTS.put(key, count - 1);
            return;
        }
        REF_COUNTS.remove(key);
        ServerLevel level = server.getLevel(key.dimension());
        if (level != null && level.getBlockState(key.pos()).is(Blocks.LIGHT)) {
            level.removeBlock(key.pos(), false);
        }
    }

    private static void clear(ServerPlayer player, LightState state) {
        clear(((ServerLevel) player.level()).getServer(), state);
        state.positions.clear();
        state.dimension = null;
    }

    private static void clear(MinecraftServer server, LightState state) {
        if (state.dimension == null) return;
        for (BlockPos pos : Set.copyOf(state.positions)) release(server, new LightKey(state.dimension, pos));
    }

    private static void forceRemoveOwned(MinecraftServer server, LightKey key) {
        ServerLevel level = server.getLevel(key.dimension());
        if (level != null && level.getBlockState(key.pos()).is(Blocks.LIGHT)) level.removeBlock(key.pos(), false);
    }

    private record LightKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final class LightState {
        long untilTick;
        ResourceKey<Level> dimension;
        final Set<BlockPos> positions = new HashSet<>();
    }
}
