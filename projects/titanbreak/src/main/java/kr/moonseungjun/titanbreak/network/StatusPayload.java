package kr.moonseungjun.titanbreak.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StatusPayload(String snapshot) implements CustomPacketPayload {
    public static final Type<StatusPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "status"));
    public static final StreamCodec<ByteBuf, StatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StatusPayload::snapshot, StatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
