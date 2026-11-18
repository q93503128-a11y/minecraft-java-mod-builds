package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Carries an intent only: no client-supplied coordinates, targets, damage, or cooldowns. */
public record BarrierInputPayload(int action) implements CustomPacketPayload {
    public static final int PREVIEW_OR_PLACE = 0, CYCLE_KIND = 1, CYCLE_SIZE = 2, REPEAT = 3;
    public static final int BOLT = 4, FLASH = 5, AMPLIFY = 6, CANCEL_NEAREST = 7, CANCEL_PREVIEW = 8;
    public static final Type<BarrierInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "barrier_input"));
    public static final StreamCodec<ByteBuf, BarrierInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> ByteBufCodecs.VAR_INT.encode(buffer, value.action),
            buffer -> new BarrierInputPayload(ByteBufCodecs.VAR_INT.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
