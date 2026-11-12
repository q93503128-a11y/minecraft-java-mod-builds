package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClassFocusStatePayload(ProfileStatePayload profile, int equippedClassIndex,
                                     int equippedTierIndex, int resultCode) implements CustomPacketPayload {
    public static final Type<ClassFocusStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "class_focus_state"));
    public static final StreamCodec<ByteBuf, ClassFocusStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ProfileStatePayload.STREAM_CODEC.encode(buffer, value.profile);
                ByteBufCodecs.VAR_INT.encode(buffer, value.equippedClassIndex);
                ByteBufCodecs.VAR_INT.encode(buffer, value.equippedTierIndex);
                ByteBufCodecs.VAR_INT.encode(buffer, value.resultCode);
            }, buffer -> new ClassFocusStatePayload(ProfileStatePayload.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer)));

    public ClassFocusStatePayload {
        equippedClassIndex = equippedClassIndex < 0 || equippedClassIndex >= 6 ? -1 : equippedClassIndex;
        equippedTierIndex = equippedTierIndex < 0 || equippedTierIndex >= 5 ? -1 : equippedTierIndex;
        if (equippedClassIndex == -1) equippedTierIndex = -1;
        if (resultCode < 0 || resultCode > 7) resultCode = 7;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
