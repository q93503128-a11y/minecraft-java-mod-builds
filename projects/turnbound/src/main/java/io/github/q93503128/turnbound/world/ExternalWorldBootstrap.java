package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Production world gate for the TURNBOUND overhaul.
 *
 * <p>Legacy Aster March builders are intentionally not called here. A verified/bound external world is used as-is;
 * this bootstrap only opens TURNBOUND runtime state and performs a one-time per-world player arrival at the selected
 * authored hub seed.</p>
 */
public final class ExternalWorldBootstrap {
    private static final Set<UUID> ACTIVE = new LinkedHashSet<>();
    private static final float HUB_YAW = 180.0F;

    private ExternalWorldBootstrap() {}

    public static boolean initialize(ServerPlayer player) {
        if (player == null || player.level().dimension() != Level.OVERWORLD) return false;
        MinecraftServer server = player.level().getServer();
        if (server == null || !DrehmalWorldBinding.isBound(server)) {
            ACTIVE.remove(player.getUUID());
            FieldNetwork.close(player);
            return false;
        }

        ACTIVE.add(player.getUUID());
        FieldNetwork.close(player);

        ExternalWorldSavedData saved = ExternalWorldSavedData.get(server);
        if (!saved.initialized(player.getUUID()) && !BattleSessionManager.exists(player)) {
            placeAtHub(player, server.overworld());
            saved.markInitialized(player.getUUID());
            Turnbound.LOGGER.info(
                    "TURNBOUND external-world first arrival: {} -> {}",
                    player.getUUID(),
                    DrehmalWorldBinding.hubSeed());
        }
        return true;
    }

    public static boolean tick(ServerPlayer player) {
        if (player == null) return false;
        MinecraftServer server = player.level().getServer();
        if (server == null || !DrehmalWorldBinding.isBound(server)) {
            if (ACTIVE.remove(player.getUUID())) FieldNetwork.close(player);
            return false;
        }
        if (!ACTIVE.contains(player.getUUID())) return initialize(player);
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return player != null
                && ACTIVE.contains(player.getUUID())
                && player.level().dimension() == Level.OVERWORLD
                && DrehmalWorldBinding.isBound(player.level().getServer());
    }

    public static void remove(ServerPlayer player) {
        if (player != null) ACTIVE.remove(player.getUUID());
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void placeAtHub(ServerPlayer player, ServerLevel overworld) {
        BlockPos hub = DrehmalWorldBinding.hubSeed();
        overworld.getChunkAt(hub);
        player.stopRiding();
        player.setNoGravity(false);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(
                overworld,
                hub.getX() + 0.5D,
                hub.getY(),
                hub.getZ() + 0.5D,
                Set.of(),
                HUB_YAW,
                0.0F,
                false);
    }
}
