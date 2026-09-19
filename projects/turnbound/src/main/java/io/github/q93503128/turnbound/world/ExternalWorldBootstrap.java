package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
    private static final Map<UUID, String> LAST_LOCATION = new HashMap<>();
    private static final Map<UUID, String> LAST_INTERACTION = new HashMap<>();
    private static final Map<UUID, String> LAST_NAVIGATION = new HashMap<>();

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
        LAST_LOCATION.put(player.getUUID(), DrehmalFirstRouteRuntime.locationId(player));
        LAST_INTERACTION.put(player.getUUID(), DrehmalFirstRouteRuntime.interactionId(player));
        LAST_NAVIGATION.put(player.getUUID(), DrehmalFirstRouteRuntime.navigationId(player));
        FieldNetwork.syncExternal(player, DrehmalFirstRouteRuntime.explorationSnapshot(player));

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
        if (player.level().dimension() != Level.OVERWORLD) {
            if (ACTIVE.remove(player.getUUID())) FieldNetwork.close(player);
            return false;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null || !DrehmalWorldBinding.isBound(server)) {
            if (ACTIVE.remove(player.getUUID())) FieldNetwork.close(player);
            return false;
        }
        if (!ACTIVE.contains(player.getUUID())) return initialize(player);

        DrehmalVisibleEncounterService.tick(player);
        DrabyelHubServiceRuntime.tick(player);
        String location = DrehmalFirstRouteRuntime.locationId(player);
        String previousLocation = LAST_LOCATION.put(player.getUUID(), location);
        boolean locationChanged = previousLocation == null || !previousLocation.equals(location);
        String interaction = DrehmalFirstRouteRuntime.interactionId(player);
        String previousInteraction = LAST_INTERACTION.put(player.getUUID(), interaction);
        boolean interactionChanged = previousInteraction == null || !previousInteraction.equals(interaction);
        String navigation = DrehmalFirstRouteRuntime.navigationId(player);
        String previousNavigation = LAST_NAVIGATION.put(player.getUUID(), navigation);
        boolean navigationChanged = previousNavigation == null || !previousNavigation.equals(navigation);
        if (locationChanged || interactionChanged || navigationChanged || player.tickCount % 40 == 0) {
            FieldNetwork.syncExternal(player, DrehmalFirstRouteRuntime.explorationSnapshot(player));
        }
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return player != null
                && ACTIVE.contains(player.getUUID())
                && player.level().dimension() == Level.OVERWORLD
                && DrehmalWorldBinding.isBound(player.level().getServer());
    }

    public static boolean interactEntity(ServerPlayer player, net.minecraft.world.entity.Entity target) {
        return active(player) && DrabyelHubServiceRuntime.interact(player, target);
    }

    public static boolean serviceActor(net.minecraft.world.entity.Entity target) {
        return DrabyelHubServiceRuntime.serviceLocator(target) != null;
    }

    public static boolean onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        if (!active(player)) return false;
        return DrehmalVisibleEncounterService.onBattleEnded(player, encounterId, outcome);
    }

    public static void remove(ServerPlayer player) {
        if (player == null) return;
        DrehmalVisibleEncounterService.onPlayerRemoved(player);
        ACTIVE.remove(player.getUUID());
        LAST_LOCATION.remove(player.getUUID());
        LAST_INTERACTION.remove(player.getUUID());
        LAST_NAVIGATION.remove(player.getUUID());
    }

    public static void clear() {
        DrehmalVisibleEncounterService.clear();
        DrabyelHubServiceRuntime.clear();
        ACTIVE.clear();
        LAST_LOCATION.clear();
        LAST_INTERACTION.clear();
        LAST_NAVIGATION.clear();
    }
}
