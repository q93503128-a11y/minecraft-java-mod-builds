package kr.moonseungjun.titanbreak.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StationOpenPayload(String station) implements CustomPacketPayload {
    public static final Type<StationOpenPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "station_open"));
    public static final StreamCodec<ByteBuf, StationOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StationOpenPayload::station, StationOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
