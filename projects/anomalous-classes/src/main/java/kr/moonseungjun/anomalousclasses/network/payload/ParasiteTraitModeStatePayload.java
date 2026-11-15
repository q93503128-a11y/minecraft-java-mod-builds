package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The server confirms whether inherited ability input mode is active. */
public record ParasiteTraitModeStatePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<ParasiteTraitModeStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "parasite_trait_mode"));
    public static final StreamCodec<ByteBuf, ParasiteTraitModeStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> ByteBufCodecs.BOOL.encode(buf, payload.enabled),
            buf -> new ParasiteTraitModeStatePayload(ByteBufCodecs.BOOL.decode(buf)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
