package kr.moonseungjun.titanbreak.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StationActionPayload(String action) implements CustomPacketPayload {
    public static final Type<StationActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "station_action"));
    public static final StreamCodec<ByteBuf, StationActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StationActionPayload::action, StationActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
