package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record RequestClassStatusPayload() implements CustomPacketPayload {
    public static final RequestClassStatusPayload INSTANCE = new RequestClassStatusPayload();
    public static final Type<RequestClassStatusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "class_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestClassStatusPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
