package dev.moonseungjun.fishinggame;

import dev.moonseungjun.fishinggame.fishing.FishingSessionManager;
import dev.moonseungjun.fishinggame.profile.FishingProfiles;
import dev.moonseungjun.fishinggame.progression.FishingRodVisuals;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import dev.moonseungjun.fishinggame.world.FishingWorldManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class FishingGameRules {
    private FishingGameRules() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            player.setGameMode(GameType.ADVENTURE);
            keepPlayerStable(player);
            FishingRodVisuals.ensureEquipped(player, FishingRods.byTier(FishingProfiles.get(player).rodTier()));
            FishingWorldManager.prepareAndPlacePlayer(player, server);
            FishingSessionManager.syncProfile(player);
            FishingSessionManager.syncIdle(player);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                keepPlayerStable(player);
            }
        });
    }

    private static void keepPlayerStable(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
    }
}
