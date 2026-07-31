package kr.moonseungjun.livingkingdoms.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestCodexPayload(String page) implements CustomPacketPayload {
    public static final Type<RequestCodexPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "request_codex")
    );
    public static final StreamCodec<ByteBuf, RequestCodexPayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(RequestCodexPayload::new, RequestCodexPayload::page);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
