package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Production world gate for the TURNBOUND overhaul.
 *
 * <p>Legacy Aster March builders are intentionally not called here. A verified/bound external world is used as-is.
 * Current Drehmal coordinates are integration seeds, not yet 26.2-playtested safe arrivals, so this bootstrap never
 * teleports a player or changes authored terrain. Safe arrival promotion belongs to the migrated-world inspection
 * gate.</p>
 */
public final class ExternalWorldBootstrap {
    private static final Set<UUID> ACTIVE = new LinkedHashSet<>();

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
        FieldNetwork.syncExternal(player, explorationSnapshot());

        ExternalWorldSavedData saved = ExternalWorldSavedData.get(server);
        if (!saved.initialized(player.getUUID())) {
            saved.markInitialized(player.getUUID());
            Turnbound.LOGGER.info(
                    "TURNBOUND external-world runtime opened for {}. Safe hub arrival remains pending terrain validation; hub seed={}",
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

    private static FieldUiSnapshot explorationSnapshot() {
        return new FieldUiSnapshot(
                true,
                FieldUiSnapshot.Mode.NONE,
                0,
                0,
                false,
                false,
                0,
                0,
                "",
                "",
                FieldUiSnapshot.Reward.none(),
                List.of(),
                List.of());
    }
}
