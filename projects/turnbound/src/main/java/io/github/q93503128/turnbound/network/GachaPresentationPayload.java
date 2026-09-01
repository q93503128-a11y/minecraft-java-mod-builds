package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-resolved summon results for client-only Echo Archive presentation. */
public record GachaPresentationPayload(String result) implements CustomPacketPayload {
    public static final Type<GachaPresentationPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "gacha_presentation"));
    public static final StreamCodec<ByteBuf, GachaPresentationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GachaPresentationPayload::result, GachaPresentationPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
