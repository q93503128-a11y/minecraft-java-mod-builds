package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BattleCommandPayload(String command) implements CustomPacketPayload {
    public static final Type<BattleCommandPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,"battle_command"));
    public static final StreamCodec<ByteBuf,BattleCommandPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,BattleCommandPayload::command,BattleCommandPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){ return TYPE; }
}
