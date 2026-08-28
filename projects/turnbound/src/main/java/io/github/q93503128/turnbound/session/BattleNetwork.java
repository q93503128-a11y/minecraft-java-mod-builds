package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.network.BattleCommandPayload;
import io.github.q93503128.turnbound.network.BattleSnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BattleNetwork {
    public static final String PROTOCOL="turnbound-alpha2"; private BattleNetwork(){}
    public static void register(RegisterPayloadHandlersEvent event){
        PayloadRegistrar r=event.registrar(PROTOCOL); r.playToClient(BattleSnapshotPayload.TYPE,BattleSnapshotPayload.STREAM_CODEC);
        r.playToServer(BattleCommandPayload.TYPE,BattleCommandPayload.STREAM_CODEC,(payload,context)->{ if(context.player() instanceof ServerPlayer p) BattleSessionManager.command(p,payload.command()); });
    }
    static void sync(ServerPlayer p,BattleSession s){PacketDistributor.sendToPlayer(p,new BattleSnapshotPayload(BattleSnapshotCodec.encode(s)));}
    static void close(ServerPlayer p){PacketDistributor.sendToPlayer(p,new BattleSnapshotPayload("H|0|0|1|RUNNING||1\n"));}
}
