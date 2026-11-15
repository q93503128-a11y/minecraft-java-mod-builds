package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client sends R press/release, never its chosen target or server progress. */
public record ParasiteInputPayload(int action) implements CustomPacketPayload {
    public static final int START = 0;
    public static final int STOP = 1;
    public static final Type<ParasiteInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "parasite_input"));
    public static final StreamCodec<ByteBuf, ParasiteInputPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> ByteBufCodecs.VAR_INT.encode(buf, value.action),
            buf -> new ParasiteInputPayload(ByteBufCodecs.VAR_INT.decode(buf)));
    public ParasiteInputPayload {
        if (action != START && action != STOP) action = -1;
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
