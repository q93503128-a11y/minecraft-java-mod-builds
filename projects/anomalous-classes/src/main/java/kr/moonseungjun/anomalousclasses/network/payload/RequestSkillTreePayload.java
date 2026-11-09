package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestSkillTreePayload() implements CustomPacketPayload {
    public static final RequestSkillTreePayload INSTANCE = new RequestSkillTreePayload();
    public static final Type<RequestSkillTreePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "open_skill_tree"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSkillTreePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
