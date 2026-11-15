package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Movement mask and bounded viewing direction; no client-supplied position or attack target. */
public record ParasiteMovePayload(int mask, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<ParasiteMovePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "parasite_move"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParasiteMovePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.VAR_INT.encode(buf, payload.mask);
                buf.writeFloat(payload.yaw);
                buf.writeFloat(payload.pitch);
            },
            buf -> new ParasiteMovePayload(ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readFloat(), buf.readFloat()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
