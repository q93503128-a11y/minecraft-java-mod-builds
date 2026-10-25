package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server authority for the acquired-augment collection snapshot. */
public final class AugmentProgressNetworking {
    private AugmentProgressNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(AugmentProgressStatePayload.TYPE, AugmentProgressStatePayload.CODEC);
        ServerPlayerEvents.JOIN.register(AugmentProgressNetworking::syncNow);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> syncNow(newPlayer));
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
