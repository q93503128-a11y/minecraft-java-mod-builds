package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectSlotPayload(int slot) implements CustomPacketPayload {
    public static final Type<SelectSlotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "select_slot"));
    public static final StreamCodec<ByteBuf, SelectSlotPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(SelectSlotPayload::new, SelectSlotPayload::slot);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
