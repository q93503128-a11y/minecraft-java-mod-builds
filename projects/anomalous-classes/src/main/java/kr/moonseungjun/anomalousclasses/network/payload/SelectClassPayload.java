package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectClassPayload(int classIndex, int expectedRevision) implements CustomPacketPayload {
    public static final Type<SelectClassPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "select_class"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectClassPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectClassPayload::classIndex,
            ByteBufCodecs.VAR_INT, SelectClassPayload::expectedRevision,
            SelectClassPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
