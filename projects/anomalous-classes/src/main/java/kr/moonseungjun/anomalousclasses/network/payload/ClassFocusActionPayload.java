package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClassFocusActionPayload(int action, int tierIndex, int expectedRevision) implements CustomPacketPayload {
    public static final int EQUIP = 0;
    public static final int UNEQUIP = 1;
    public static final Type<ClassFocusActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "class_focus_action"));
    public static final StreamCodec<ByteBuf, ClassFocusActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, value.action);
                ByteBufCodecs.VAR_INT.encode(buffer, value.tierIndex);
                ByteBufCodecs.VAR_INT.encode(buffer, value.expectedRevision);
            }, buffer -> new ClassFocusActionPayload(
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer)));

    public ClassFocusActionPayload {
        if (action != EQUIP && action != UNEQUIP) action = -1;
        if (tierIndex < 0 || tierIndex > 4) tierIndex = -1;
        if (expectedRevision < 0) expectedRevision = -1;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
