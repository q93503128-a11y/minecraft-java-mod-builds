package io.github.q93503128.turnbound;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exact opt-in world only. Every position must be supplied by an approved
 * copy-world entry manifest or a trusted server-side checkpoint activator.
 * No built-in Drehmal location and no client-supplied teleport destination.
 */
public final class TurnboundCheckpointService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TurnboundCheckpointService.class);
    public static final String ENTRY_FILENAME = "turnbound-entry.json";
    private static final int MAX_ENTRY_BYTES = 4096;
    private static final Map<MinecraftServer, TurnboundCheckpointTarget> ENTRY =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private TurnboundCheckpointService() {}

    public static void register() {
        // WorldGate.open is registered first by the TURNBOUND initializer.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!TurnboundWorldGate.active(server)) return;
            loadEntry(server).ifPresent(location -> ENTRY.put(server, location));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ENTRY.remove(server));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!TurnboundWorldGate.active(server)) return;
            TurnboundCheckpointTarget entry = ENTRY.get(server);
            if (entry == null) return;
            TurnboundCheckpointState state = state(server);
            ServerPlayerEntity player = handler.player;
            if (state.get(player.getUuid()).isPresent()) return; // Rejoin: keep actual position.
            if (move(player, entry)) {
                state.set(player.getUuid(), entry); // First valid entry, no repeated teleport.
            } else {
                LOGGER.warn("TURNBOUND start location failed safety check. No player moved.");
            }
        });

        // This normally cannot fire: TURNBOUND blocks incoming vanilla HP damage.
        // Retain it for nonstandard respawn paths, but never alter an unmarked world.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive && TurnboundWorldGate.active(newPlayer.getServer())) {
                returnAfterDefeat(newPlayer);
            }
        });

        // Do not turn a party loss (or an unexpected fatal vanilla hit) into
        // vanilla inventory/XP drops. Explicit /kill or other mods can bypass
        // normal damage; this is still not evidence of perfect death coverage.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)
                    || !TurnboundWorldGate.active(player.getServer())) return true;
            player.setHealth(player.getMaxHealth());
            returnAfterDefeat(player);
            return false;
        });

        // Void rescue is only possible when an explicitly configured checkpoint
        // passes the same collision/ground checks as first entry.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!TurnboundWorldGate.active(server)) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getY() < player.getServerWorld().getBottomY() - 8) {
                    returnAfterDefeat(player);
                }
            }
        });
    }

    private static Optional<TurnboundCheckpointTarget> loadEntry(MinecraftServer server) {
        Path file = server.getSavePath(WorldSavePath.ROOT).resolve(ENTRY_FILENAME);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            LOGGER.info("TURNBOUND entry location is unconfigured; automatic join teleport disabled.");
            return Optional.empty();
        }
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file) > MAX_ENTRY_BYTES) {
                throw new IllegalArgumentException("Not a small regular file");
            }
            TurnboundCheckpointTarget target =
                    TurnboundCheckpointTarget.parseEntry(Files.readString(file, StandardCharsets.UTF_8));
            return Optional.of(target);
        } catch (IOException | RuntimeException invalid) {
            LOGGER.error("TURNBOUND entry manifest is invalid. Automatic entry/respawn fallback disabled: {}",
                    invalid.getMessage());
            return Optional.empty();
        }
    }

    private static TurnboundCheckpointState state(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                TurnboundCheckpointState::fromNbt,
                TurnboundCheckpointState::new,
                TurnboundCheckpointState.STORAGE_KEY);
    }

    private static ServerWorld worldFor(MinecraftServer server, TurnboundCheckpointTarget location) {
        Identifier id = Identifier.tryParse(location.dimension());
        return id == null ? null : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
    }

    private static boolean safe(ServerWorld world, TurnboundCheckpointTarget location) {
        if (world == null) return false;
        BlockPos feet = BlockPos.ofFloored(location.x(), location.y(), location.z());
        BlockPos head = feet.up();
        BlockPos below = feet.down();
        if (below.getY() < world.getBottomY() || head.getY() >= world.getTopY()) return false;
        // Never generate a missing authored-world chunk just to test a checkpoint.
        if (!world.isChunkLoaded(feet.getX() >> 4, feet.getZ() >> 4)) return false;
        // This reads existing blocks only. No scaffold, excavation or world edits.
        BlockState ground = world.getBlockState(below);
        if (!ground.isSideSolidFullSquare(world, below, Direction.UP)
                || ground.isOf(Blocks.MAGMA_BLOCK) || ground.isOf(Blocks.CAMPFIRE)
                || ground.isOf(Blocks.SOUL_CAMPFIRE)) return false;
        for (BlockPos pos : new BlockPos[] {feet, head}) {
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                    || !world.getFluidState(pos).isEmpty()
                    || world.getBlockState(pos).isOf(Blocks.FIRE)
                    || world.getBlockState(pos).isOf(Blocks.SOUL_FIRE)) return false;
        }
        return true;
    }

    private static boolean move(ServerPlayerEntity player, TurnboundCheckpointTarget target) {
        MinecraftServer server = player.getServer();
        if (!TurnboundWorldGate.active(server)) return false;
        ServerWorld world = worldFor(server, target);
        if (!safe(world, target)) return false;
        player.teleport(world, target.x(), target.y(), target.z(), target.yaw(), target.pitch());
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        return true;
    }

    /**
     * Trusted server-side checkpoint activation only. Never invoke from a
     * client packet. A coordinate rejected by physical checks is not saved.
     */
    public static boolean activateCheckpoint(ServerPlayerEntity player, TurnboundCheckpointTarget target) {
        MinecraftServer server = player.getServer();
        if (!TurnboundWorldGate.active(server) || !safe(worldFor(server, target), target)) {
            return false;
        }
        state(server).set(player.getUuid(), target);
        return true;
    }

    /**
     * Lossless physical return for a future authoritative battle defeat.
     * No victory rewards or XP changes, no vanilla player death.
     */
    public static boolean returnAfterDefeat(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (!TurnboundWorldGate.active(server)) return false;
        TurnboundCheckpointTarget entry = ENTRY.get(server);
        Optional<TurnboundCheckpointTarget> personal = state(server).get(player.getUuid());
        if (personal.isPresent() && move(player, personal.get())) return true;
        if (entry != null && move(player, entry)) return true;
        // Never guess a world coordinate when no tested safe checkpoint exists.
        return false;
    }
}
