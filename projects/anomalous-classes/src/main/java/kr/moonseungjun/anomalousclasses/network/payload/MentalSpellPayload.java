package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client requests an ability, server determines every target and effect. */
public record MentalSpellPayload(int ability) implements CustomPacketPayload {
    public static final Type<MentalSpellPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "mental_spell"));
    public static final StreamCodec<ByteBuf, MentalSpellPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> ByteBufCodecs.VAR_INT.encode(buffer, value.ability),
            buffer -> new MentalSpellPayload(ByteBufCodecs.VAR_INT.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
