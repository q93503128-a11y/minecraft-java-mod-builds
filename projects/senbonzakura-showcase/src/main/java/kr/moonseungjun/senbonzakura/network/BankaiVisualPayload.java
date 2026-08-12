package kr.moonseungjun.senbonzakura.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.senbonzakura.SenbonzakuraShowcase;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BankaiVisualPayload(String state) implements CustomPacketPayload {
    public static final Type<BankaiVisualPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SenbonzakuraShowcase.MOD_ID, "bankai_visual"));
    public static final StreamCodec<ByteBuf, BankaiVisualPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BankaiVisualPayload::state, BankaiVisualPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
