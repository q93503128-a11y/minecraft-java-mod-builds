package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authored post-story encounter briefing shown before entry. */
public record EndgameBriefingPayload(String briefing) implements CustomPacketPayload {
    public static final Type<EndgameBriefingPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "endgame_briefing"));
    public static final StreamCodec<ByteBuf, EndgameBriefingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EndgameBriefingPayload::briefing, EndgameBriefingPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
