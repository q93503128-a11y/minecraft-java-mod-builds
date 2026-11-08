package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record RequestClassMenuPayload() implements CustomPacketPayload {
    public static final RequestClassMenuPayload INSTANCE = new RequestClassMenuPayload();
    public static final Type<RequestClassMenuPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "open_class_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestClassMenuPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
