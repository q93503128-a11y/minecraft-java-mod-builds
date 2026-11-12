package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record ClassManagementStatePayload(ProfileStatePayload profile) implements CustomPacketPayload {
    public static final Type<ClassManagementStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "class_management_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClassManagementStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> ProfileStatePayload.STREAM_CODEC.encode(buffer, payload.profile()),
                    buffer -> new ClassManagementStatePayload(ProfileStatePayload.STREAM_CODEC.decode(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
