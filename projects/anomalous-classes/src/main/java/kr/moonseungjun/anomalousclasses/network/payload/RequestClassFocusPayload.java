package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestClassFocusPayload() implements CustomPacketPayload {
    public static final RequestClassFocusPayload INSTANCE = new RequestClassFocusPayload();
    public static final Type<RequestClassFocusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "request_class_focus"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestClassFocusPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
