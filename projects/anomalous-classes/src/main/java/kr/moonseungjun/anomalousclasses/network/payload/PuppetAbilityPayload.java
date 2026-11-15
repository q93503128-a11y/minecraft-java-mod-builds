package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** User only chooses a skill ID. Target, materials and cooldown remain authoritative. */
public record PuppetAbilityPayload(int skill) implements CustomPacketPayload {
    public static final Type<PuppetAbilityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "puppet_ability"));
    public static final StreamCodec<ByteBuf, PuppetAbilityPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> ByteBufCodecs.VAR_INT.encode(buffer, value.skill),
            buffer -> new PuppetAbilityPayload(ByteBufCodecs.VAR_INT.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
