package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.network.FieldCommandPayload;
import io.github.q93503128.turnbound.network.FieldSnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FieldNetwork {
    public static final String PROTOCOL="turnbound-field-alpha12";
    private FieldNetwork(){}
    public static void register(RegisterPayloadHandlersEvent event){ PayloadRegistrar r=event.registrar(PROTOCOL); r.playToClient(FieldSnapshotPayload.TYPE,FieldSnapshotPayload.STREAM_CODEC); r.playToServer(FieldCommandPayload.TYPE,FieldCommandPayload.STREAM_CODEC,(payload,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer p)WorldSessionRouter.command(p,payload.command());})); }
    public static void sync(ServerPlayer p,FieldUiSnapshot s){PacketDistributor.sendToPlayer(p,new FieldSnapshotPayload(FieldUiCodec.encode(s)));}
    public static void close(ServerPlayer p){sync(p,FieldUiSnapshot.inactive());}
}
