package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestGrimoirePayload(String page) implements CustomPacketPayload {
    public static final Type<RequestGrimoirePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "request_grimoire"));
    public static final StreamCodec<ByteBuf, RequestGrimoirePayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(RequestGrimoirePayload::new, RequestGrimoirePayload::page);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
