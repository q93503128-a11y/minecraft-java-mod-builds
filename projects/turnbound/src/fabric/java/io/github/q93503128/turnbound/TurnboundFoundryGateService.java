package io.github.q93503128.turnbound;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TURNBOUND-owned, one-way Foundry facility adapter for an explicitly opted-in
 * Drehmal world copy. It neither calls nor imports original functions, scoreboards,
 * advancements, rewards or wave logic.
 *
 * A lever is remembered only after vanilla block interaction, not by forcing a
 * distant chunk to load. The closed gate is validated against verified physical
 * geometry before making any block edits; unfamiliar terrain fails closed.
 */
public final class TurnboundFoundryGateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TurnboundFoundryGateService.class);
    private static final BlockPos[] LEVERS = {
        new BlockPos(-3128, 149, 1490),
        new BlockPos(-3035, 122, 1185),
        new BlockPos(-3394, 146, 1081)
    };
    private static final BlockPos GATE_CENTER = new BlockPos(-3276, 159, 1570);
    private static final BlockPos SOURCE = new BlockPos(-3274, 134, 1557);
    private static final BlockPos TARGET = new BlockPos(-3278, 156, 1576);
    private static final int GATE_RADIUS_SQUARED = 12 * 12;
    private static final int LENGTH_X = 5;
    private static final int HEIGHT = 9;
    private static final int LENGTH_Z = 2;
    private static final int CELL_COUNT = LENGTH_X * HEIGHT * LENGTH_Z;
    private static final long FAILED_RETRY_TICKS = 100L;

    // Verified physical *block palette* counts of the original v2.2.2f
    // Foundry gate, not original commands, data pack bytes or player state.
    private static final Map<String, Integer> EXPECTED_SOURCE = Map.of(
        "minecraft:air", 35,
        "minecraft:andesite", 13,
        "minecraft:basalt", 10,
        "minecraft:cobblestone", 10,
        "minecraft:mossy_cobblestone", 3,
        "minecraft:polished_basalt", 11,
        "minecraft:stone", 8
    );
    private static final Map<String, Integer> EXPECTED_CLOSED_TARGET = Map.of(
        "minecraft:andesite", 2,
        "minecraft:basalt", 17,
        "minecraft:bedrock", 45,
        "minecraft:cobblestone", 4,
        "minecraft:polished_basalt", 18,
        "minecraft:stone", 4
    );
    private static final Map<MinecraftServer, RuntimeState> RUNTIME = new IdentityHashMap<>();

    private static final class RuntimeState {
        final Set<Integer> pendingLeverChecks = new HashSet<>();
        long ticks;
        long nextGateAttempt;
    }

    private TurnboundFoundryGateService() {}

    public static void register() {
        // Vanilla must finish changing the lever before END_SERVER_TICK reads it.
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() || !(world instanceof ServerWorld serverWorld)
                    || !world.getRegistryKey().equals(World.OVERWORLD)
                    || !TurnboundWorldGate.active(serverWorld.getServer())
                    || player.isSpectator()) {
                return ActionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            for (int i = 0; i < LEVERS.length; i++) {
                if (LEVERS[i].equals(pos) && world.getBlockState(pos).isOf(Blocks.LEVER)) {
                    RUNTIME.computeIfAbsent(serverWorld.getServer(), ignored -> new RuntimeState())
                            .pendingLeverChecks.add(i);
                    break;
                }
            }
            return ActionResult.PASS; // Do not steal/cancel the physical puzzle interaction.
        });

        ServerTickEvents.END_SERVER_TICK.register(TurnboundFoundryGateService::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(RUNTIME::remove);
    }

    private static void tick(MinecraftServer server) {
        if (!TurnboundWorldGate.active(server)) return;
        RuntimeState runtime = RUNTIME.computeIfAbsent(server, ignored -> new RuntimeState());
        runtime.ticks++;
        ServerWorld world = server.getOverworld();

        if (!runtime.pendingLeverChecks.isEmpty()) {
            TurnboundFoundryGateState state = state(world);
            for (int i : runtime.pendingLeverChecks) {
                BlockPos pos = LEVERS[i];
                if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                BlockState current = world.getBlockState(pos);
                if (current.isOf(Blocks.LEVER) && current.contains(Properties.POWERED)) {
                    state.observeLever(i, current.get(Properties.POWERED));
                }
            }
            runtime.pendingLeverChecks.clear();
        }

        if (runtime.ticks < runtime.nextGateAttempt || !hasNearbyPlayer(server, world)) return;
        TurnboundFoundryGateState state = state(world);
        if (state.opened() || !state.ready()) return;
        if (!footprintLoaded(world, SOURCE) || !footprintLoaded(world, TARGET)) return;

        if (openVerifiedClosedGate(world)) {
            state.markOpened();
            LOGGER.info("TURNBOUND Foundry gate opened by independent facility adapter");
        } else {
            // Fail closed on a modified/unknown facility rather than overwriting terrain.
            runtime.nextGateAttempt = runtime.ticks + FAILED_RETRY_TICKS;
        }
    }

    private static TurnboundFoundryGateState state(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                TurnboundFoundryGateState::fromNbt,
                TurnboundFoundryGateState::new,
                TurnboundFoundryGateState.STORAGE_KEY);
    }

    private static boolean hasNearbyPlayer(MinecraftServer server, ServerWorld world) {
        // At most the connected players; never scan all entities or unloaded map chunks.
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world || !player.isAlive() || player.isSpectator()) continue;
            double dx = player.getX() - GATE_CENTER.getX();
            double dy = player.getY() - GATE_CENTER.getY();
            double dz = player.getZ() - GATE_CENTER.getZ();
            if (dx * dx + dy * dy + dz * dz <= GATE_RADIUS_SQUARED) return true;
        }
        return false;
    }

    private static boolean footprintLoaded(ServerWorld world, BlockPos origin) {
        for (int x = 0; x < LENGTH_X; x++) {
            for (int z = 0; z < LENGTH_Z; z++) {
                if (!world.isChunkLoaded((origin.getX() + x) >> 4,
                                         (origin.getZ() + z) >> 4)) return false;
            }
        }
        return true;
    }

    private static String blockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    static boolean matchesExpectedPhysicalPalette(
            Map<String, Integer> source, Map<String, Integer> target, int differences) {
        return differences == 59
                && EXPECTED_SOURCE.equals(source)
                && EXPECTED_CLOSED_TARGET.equals(target);
    }

    private static boolean openVerifiedClosedGate(ServerWorld world) {
        BlockState[] source = new BlockState[CELL_COUNT];
        BlockState[] target = new BlockState[CELL_COUNT];
        BlockPos[] targetPositions = new BlockPos[CELL_COUNT];
        Map<String, Integer> sourceCounts = new HashMap<>();
        Map<String, Integer> targetCounts = new HashMap<>();
        int differences = 0;
        int index = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < LENGTH_X; x++) {
                for (int z = 0; z < LENGTH_Z; z++) {
                    BlockPos a = SOURCE.add(x, y, z);
                    BlockPos d = TARGET.add(x, y, z);
                    if (world.getBlockEntity(a) != null || world.getBlockEntity(d) != null) {
                        return false; // Do not overwrite a tile entity or inventory.
                    }
                    BlockState currentSource = world.getBlockState(a);
                    BlockState currentTarget = world.getBlockState(d);
                    source[index] = currentSource;
                    target[index] = currentTarget;
                    targetPositions[index] = d;
                    sourceCounts.merge(blockId(currentSource), 1, Integer::sum);
                    targetCounts.merge(blockId(currentTarget), 1, Integer::sum);
                    if (!currentSource.equals(currentTarget)) differences++;
                    index++;
                }
            }
        }
        if (!matchesExpectedPhysicalPalette(sourceCounts, targetCounts, differences)) {
            return false;
        }

        boolean ok = true;
        for (int i = 0; i < CELL_COUNT; i++) {
            if (source[i].equals(target[i])) continue;
            if (!world.setBlockState(targetPositions[i], source[i], Block.NOTIFY_ALL)) {
                ok = false;
                break;
            }
        }
        if (ok) {
            for (int i = 0; i < CELL_COUNT; i++) {
                if (!world.getBlockState(targetPositions[i]).equals(source[i])) {
                    ok = false;
                    break;
                }
            }
        }
        if (!ok) {
            // The operation is all-or-nothing to the extent vanilla block updates
            // permit. Do not mark completion if a partial mutation is observed.
            for (int i = 0; i < CELL_COUNT; i++) {
                if (!world.getBlockState(targetPositions[i]).equals(target[i])) {
                    world.setBlockState(targetPositions[i], target[i], Block.NOTIFY_ALL);
                }
            }
        }
        return ok;
    }
}
