package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Intent only. Target, collision, cooldown and casualties are server authoritative. */
public record SwarmCombatPayload(int action) implements CustomPacketPayload {
    public static final int TARGET = 0, RUSH = 1, SACRIFICE = 2;
    public static final Type<SwarmCombatPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "swarm_combat"));
    public static final StreamCodec<ByteBuf, SwarmCombatPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> ByteBufCodecs.VAR_INT.encode(buffer, value.action),
            buffer -> new SwarmCombatPayload(ByteBufCodecs.VAR_INT.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
