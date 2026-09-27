package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Compact server-authoritative social-party UI command. */
public record PartyCommandPayload(String command) implements CustomPacketPayload {
    public static final Type<PartyCommandPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "party_command"));
    public static final StreamCodec<ByteBuf, PartyCommandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PartyCommandPayload::command, PartyCommandPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
