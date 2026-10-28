package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server authority for the acquired-augment collection snapshot. */
public final class AugmentProgressNetworking {
    private static final int[] RESPAWN_RESYNC_TICKS = {2, 10, 40};
    private static final Map<UUID, RespawnResync> RESPAWN_RESYNCS = new HashMap<>();

    private AugmentProgressNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(AugmentProgressStatePayload.TYPE, AugmentProgressStatePayload.CODEC);
        ServerPlayerEvents.JOIN.register(AugmentProgressNetworking::syncNow);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Map<String, Integer> expectedAugments = Map.copyOf(
                    AugmentationPlayerStateService.state(oldPlayer).augmentLevels()
            );
            if (containsExpectedAugments(newPlayer, expectedAugments)) {
                syncNow(newPlayer);
            }
            RESPAWN_RESYNCS.put(newPlayer.getUUID(), new RespawnResync(expectedAugments, 0));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, RespawnResync>> iterator = RESPAWN_RESYNCS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, RespawnResync> entry = iterator.next();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null) {
                    iterator.remove();
                    continue;
                }

                RespawnResync resync = entry.getValue();
                int elapsed = resync.elapsedTicks() + 1;
                boolean complete = containsExpectedAugments(player, resync.expectedAugments());
                if (complete && isRespawnRetryTick(elapsed)) {
                    syncNow(player);
                }

                if (elapsed >= RESPAWN_RESYNC_TICKS[RESPAWN_RESYNC_TICKS.length - 1]) {
                    if (!complete) {
                        AugmentationSurvival.LOGGER.warn(
                                "Augment progress respawn snapshot remained incomplete for {} after {} ticks: expected={}, actual={}",
                                player.getScoreboardName(),
                                elapsed,
                                resync.expectedAugments().size(),
                                AugmentationPlayerStateService.state(player).augmentLevels().size()
                        );
                    }
                    iterator.remove();
                    continue;
                }
                entry.setValue(new RespawnResync(resync.expectedAugments(), elapsed));
            }
        });
    }

    public static void syncNow(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, AugmentProgressStatePayload.TYPE)) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        List<String> ids = new ArrayList<>(state.augmentLevels().size());
        List<Integer> levels = new ArrayList<>(state.augmentLevels().size());
        state.augmentLevels().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    ids.add(entry.getKey());
                    levels.add(entry.getValue());
                });
        ServerPlayNetworking.send(player, new AugmentProgressStatePayload(ids, levels));
    }

    /** Pure completeness check used by respawn sync and its regression coverage. */
    public static boolean containsExpectedAugments(
            AugmentationPlayerState state,
            Map<String, Integer> expectedAugments
    ) {
        for (Map.Entry<String, Integer> expected : expectedAugments.entrySet()) {
            if (state.level(expected.getKey()) != expected.getValue()) return false;
        }
        return true;
    }

    private static boolean containsExpectedAugments(
            ServerPlayer player,
            Map<String, Integer> expectedAugments
    ) {
        return containsExpectedAugments(AugmentationPlayerStateService.state(player), expectedAugments);
    }

    private static boolean isRespawnRetryTick(int elapsed) {
        for (int retryTick : RESPAWN_RESYNC_TICKS) {
            if (elapsed == retryTick) return true;
        }
        return false;
    }

    private record RespawnResync(Map<String, Integer> expectedAugments, int elapsedTicks) {
        private RespawnResync {
            expectedAugments = Map.copyOf(expectedAugments);
        }
    }
}
