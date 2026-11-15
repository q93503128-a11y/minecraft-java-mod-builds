package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** -1 toggles inherited-trait input, 0..9 requests a quick-slot ability. */
public record ParasiteTraitInputPayload(int slot) implements CustomPacketPayload {
    public static final Type<ParasiteTraitInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "parasite_trait_input"));
    public static final StreamCodec<ByteBuf, ParasiteTraitInputPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> ByteBufCodecs.VAR_INT.encode(buf, payload.slot),
            buf -> new ParasiteTraitInputPayload(ByteBufCodecs.VAR_INT.decode(buf)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
