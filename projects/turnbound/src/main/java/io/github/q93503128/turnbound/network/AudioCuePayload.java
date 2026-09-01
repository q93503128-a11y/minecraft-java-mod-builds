package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-bound semantic audio cues. Actual sound resources remain presentation assets. */
public record AudioCuePayload(String cues) implements CustomPacketPayload {
    public static final Type<AudioCuePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "audio_cues"));
    public static final StreamCodec<ByteBuf, AudioCuePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AudioCuePayload::cues, AudioCuePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
