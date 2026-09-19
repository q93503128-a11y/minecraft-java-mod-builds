package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.network.FieldCommandPayload;
import io.github.q93503128.turnbound.network.FieldSnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FieldNetwork {
    public static final String PROTOCOL = "turnbound-field-alpha12";

    private FieldNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(FieldSnapshotPayload.TYPE, FieldSnapshotPayload.STREAM_CODEC);
        registrar.playToServer(
                FieldCommandPayload.TYPE,
                FieldCommandPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        FieldCommandRouter.command(player, payload.command());
                    }
                }));
    }

    public static void sync(ServerPlayer player, FieldUiSnapshot snapshot) {
        FieldUiSnapshot projected = AsterMarchFastTravelService.project(player, snapshot);
        PacketDistributor.sendToPlayer(player, new FieldSnapshotPayload(FieldUiCodec.encode(projected)));
    }

    /** External authored-world state must not receive retired Aster March coordinate projections. */
    public static void syncExternal(ServerPlayer player, FieldUiSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new FieldSnapshotPayload(FieldUiCodec.encode(snapshot)));
    }

    /** Short ownership handoff: field HUD yields before the first battle snapshot arrives. */
    public static void suspendForBattle(ServerPlayer player) {
        syncExternal(player, FieldUiSnapshot.battleTransition());
    }

    public static void close(ServerPlayer player) {
        syncExternal(player, FieldUiSnapshot.inactive());
    }
}
