package kr.moonseungjun.riftfrontier.network;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import kr.moonseungjun.riftfrontier.combat.PlayerWeaponServerRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

/** Common-side payload registration and authoritative combat networking bridges. */
public final class RiftfrontierNetworking {
    private static final String NETWORK_VERSION = "1";

    private RiftfrontierNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(
            BossPresentationPayload.TYPE,
            BossPresentationPayload.STREAM_CODEC
        );
        registrar.playToServer(
            PlayerWeaponMoveIntentPayload.TYPE,
            PlayerWeaponMoveIntentPayload.STREAM_CODEC,
            RiftfrontierNetworking::handlePlayerWeaponMoveIntent
        );
    }

    private static void handlePlayerWeaponMoveIntent(PlayerWeaponMoveIntentPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        PlayerWeaponServerRuntime.handleMoveIntent(player, payload.moveId());
    }

    /**
     * Sends the exact semantic presentation state sealed by the validated authoritative boss runtime.
     *
     * <p>The networking boundary no longer accepts a free-standing {@link MinecraftBossCombatAdapter.TickResult};
     * production callers must cross {@link MinecraftBossCombatAdapter.ValidatedRuntime}, which derives boss identity,
     * authored phase selection and this outgoing semantic state from one validated semantic capability. Numeric entity
     * id, UUID and server tick are checked again here so a delayed result cannot be sent as another actor or tick.</p>
     */
    public static void syncBossPresentation(
        LivingEntity boss,
        long serverGameTick,
        MinecraftBossCombatAdapter.ValidatedTickResult result
    ) {
        Objects.requireNonNull(boss, "boss");
        Objects.requireNonNull(result, "result");
        BossPresentationSemanticState state = result.presentationState();
        if (state.entityId() != boss.getId()
            || !state.entityUuid().equals(boss.getUUID())
            || state.serverGameTick() != serverGameTick) {
            throw new IllegalArgumentException("validated boss presentation state does not belong to this entity/tick");
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(boss, new BossPresentationPayload(state));
    }
}
