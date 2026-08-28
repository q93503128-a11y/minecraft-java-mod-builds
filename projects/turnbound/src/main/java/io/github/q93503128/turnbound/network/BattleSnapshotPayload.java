package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BattleSnapshotPayload(String snapshot) implements CustomPacketPayload {
    public static final Type<BattleSnapshotPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,"battle_snapshot"));
    public static final StreamCodec<ByteBuf,BattleSnapshotPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,BattleSnapshotPayload::snapshot,BattleSnapshotPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){ return TYPE; }
}
