package kr.moonseungjun.riftfrontier.network;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Objects;

/** Common-side payload registration and server-to-client semantic presentation bridge. */
public final class RiftfrontierNetworking {
    private static final String NETWORK_VERSION = "1";

    private RiftfrontierNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToClient(
            BossPresentationPayload.TYPE,
            BossPresentationPayload.STREAM_CODEC
        );
    }

    /**
     * Sends the exact semantic presentation state sampled by the authoritative boss tick to tracking clients.
     * No client-facing cadence constants are introduced here.
     */
    public static void syncBossPresentation(
        LivingEntity boss,
        long serverGameTick,
        MinecraftBossCombatAdapter.TickResult result
    ) {
        Objects.requireNonNull(boss, "boss");
        Objects.requireNonNull(result, "result");
        BossPresentationSemanticState state = result.presentation()
            .map(frame -> BossPresentationSemanticState.fromFrame(boss.getId(), serverGameTick, frame))
            .orElseGet(() -> BossPresentationSemanticState.clear(boss.getId(), serverGameTick));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(boss, new BossPresentationPayload(state));
    }
}
