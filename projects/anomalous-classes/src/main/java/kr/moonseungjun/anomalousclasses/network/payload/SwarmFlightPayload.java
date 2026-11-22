package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Toggle intent; server decides qualification and flight support. */
public record SwarmFlightPayload() implements CustomPacketPayload {
    public static final SwarmFlightPayload INSTANCE = new SwarmFlightPayload();
    public static final Type<SwarmFlightPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "swarm_flight"));
    public static final StreamCodec<FriendlyByteBuf, SwarmFlightPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
