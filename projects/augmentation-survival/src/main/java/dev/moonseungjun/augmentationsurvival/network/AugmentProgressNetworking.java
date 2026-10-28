package dev.moonseungjun.augmentationsurvival.network;

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
    private static final int RESPAWN_RESYNC_DELAY_TICKS = 2;
    private static final Map<UUID, Integer> RESPAWN_RESYNCS = new HashMap<>();

    private AugmentProgressNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(AugmentProgressStatePayload.TYPE, AugmentProgressStatePayload.CODEC);
        ServerPlayerEvents.JOIN.register(AugmentProgressNetworking::syncNow);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            syncNow(newPlayer);
            RESPAWN_RESYNCS.put(newPlayer.getUUID(), RESPAWN_RESYNC_DELAY_TICKS);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Integer>> iterator = RESPAWN_RESYNCS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                int ticksRemaining = entry.getValue();
                if (ticksRemaining > 1) {
                    entry.setValue(ticksRemaining - 1);
                    continue;
                }
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) syncNow(player);
                iterator.remove();
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
}
