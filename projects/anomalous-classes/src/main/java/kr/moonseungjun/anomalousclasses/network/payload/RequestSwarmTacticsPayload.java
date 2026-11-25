package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests a current server snapshot, never a presumed client-owned tactic. */
public record RequestSwarmTacticsPayload(int view) implements CustomPacketPayload {
    public static final int MANAGEMENT = 0, QUICK = 1;
    public static final Type<RequestSwarmTacticsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "request_swarm_tactics"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSwarmTacticsPayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> buf.writeVarInt(value.view),
                    buf -> new RequestSwarmTacticsPayload(buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
