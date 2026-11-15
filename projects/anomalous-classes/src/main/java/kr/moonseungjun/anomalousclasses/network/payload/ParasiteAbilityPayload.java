package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Z/X/C indicate skill ID alone. All spell effects are server-calculated. */
public record ParasiteAbilityPayload(int slot) implements CustomPacketPayload {
    public static final Type<ParasiteAbilityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "parasite_ability"));
    public static final StreamCodec<ByteBuf, ParasiteAbilityPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> ByteBufCodecs.VAR_INT.encode(buf, value.slot),
            buf -> new ParasiteAbilityPayload(ByteBufCodecs.VAR_INT.decode(buf)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
