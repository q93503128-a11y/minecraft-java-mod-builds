package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FieldCommandPayload(String command) implements CustomPacketPayload {
    public static final Type<FieldCommandPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "field_command"));
    public static final StreamCodec<ByteBuf, FieldCommandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FieldCommandPayload::command, FieldCommandPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
