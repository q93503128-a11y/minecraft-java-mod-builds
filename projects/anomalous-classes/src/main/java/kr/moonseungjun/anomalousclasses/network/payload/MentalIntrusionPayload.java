package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MentalIntrusionPayload(int action) implements CustomPacketPayload {
    public static final int START = 0;
    public static final int STOP = 1;
    public static final Type<MentalIntrusionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "mental_intrusion"));
    public static final StreamCodec<ByteBuf, MentalIntrusionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> ByteBufCodecs.VAR_INT.encode(buffer, value.action),
            buffer -> new MentalIntrusionPayload(ByteBufCodecs.VAR_INT.decode(buffer)));

    public MentalIntrusionPayload {
        if (action != START && action != STOP) action = -1;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
