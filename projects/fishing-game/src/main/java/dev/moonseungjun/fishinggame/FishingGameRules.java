package dev.moonseungjun.fishinggame;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import dev.moonseungjun.fishinggame.fishing.FishingSessionManager;
import dev.moonseungjun.fishinggame.profile.FishingProfiles;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import dev.moonseungjun.fishinggame.progression.FishingRodVisuals;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import dev.moonseungjun.fishinggame.world.FishingWorldManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class FishingGameRules {
    private static final int INITIAL_PLACEMENT_DELAY_TICKS = 2;
    private static final Map<UUID, Integer> PENDING_INITIAL_PLACEMENT = new HashMap<>();

    private FishingGameRules() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            player.setGameMode(GameType.ADVENTURE);
            keepPlayerStable(player);
            PlayerFishingProfile profile = FishingProfiles.get(player);
            FishingRodVisuals.ensureEquipped(
                    player,
                    FishingRods.byTier(profile.rodTier()),
                    profile.rebirths()
            );

            // Do not dimension-teleport from inside the JOIN callback. The player is still finishing
            // vanilla chunk-tracker registration here, and moving them immediately can leave the
            // old tracked section inconsistent when the integrated server later disconnects/stops.
            PENDING_INITIAL_PLACEMENT.put(player.getUUID(), INITIAL_PLACEMENT_DELAY_TICKS);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PENDING_INITIAL_PLACEMENT.remove(handler.getPlayer().getUUID())
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Integer>> pending = PENDING_INITIAL_PLACEMENT.entrySet().iterator();
            while (pending.hasNext()) {
                Map.Entry<UUID, Integer> entry = pending.next();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null || player.isRemoved()) {
                    pending.remove();
                    continue;
                }

                int ticksLeft = entry.getValue();
                if (ticksLeft > 0) {
                    entry.setValue(ticksLeft - 1);
                    continue;
                }

                if (FishingWorldManager.prepareInitialPlayer(player, server)) {
                    FishingSessionManager.syncProfile(player);
                    FishingSessionManager.syncIdle(player);
                    pending.remove();
                } else {
                    entry.setValue(20);
                }
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.isRemoved()) keepPlayerStable(player);
            }
        });
    }

    private static void keepPlayerStable(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
    }
}
