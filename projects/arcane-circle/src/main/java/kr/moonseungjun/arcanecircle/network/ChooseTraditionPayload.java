
package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChooseTraditionPayload(String traditionId) implements CustomPacketPayload {
    public static final Type<ChooseTraditionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "choose_tradition"));
    public static final StreamCodec<ByteBuf, ChooseTraditionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChooseTraditionPayload::traditionId,
            ChooseTraditionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
