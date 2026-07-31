package kr.moonseungjun.livingkingdoms.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenCodexPayload(String page, String snapshot) implements CustomPacketPayload {
    public static final Type<OpenCodexPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "open_codex")
    );
    public static final StreamCodec<ByteBuf, OpenCodexPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenCodexPayload::page,
            ByteBufCodecs.STRING_UTF8,
            OpenCodexPayload::snapshot,
            OpenCodexPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
