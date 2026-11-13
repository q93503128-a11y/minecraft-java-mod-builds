package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestMentalDominatorPayload() implements CustomPacketPayload {
    public static final RequestMentalDominatorPayload INSTANCE = new RequestMentalDominatorPayload();
    public static final Type<RequestMentalDominatorPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "open_mental_dominator"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMentalDominatorPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
