package kr.moonseungjun.livingkingdoms.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SubmitOriginPayload(
        String speciesId,
        String homelandId,
        String backgroundId,
        String residenceId
) implements CustomPacketPayload {
    public static final Type<SubmitOriginPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "submit_origin")
    );

    public static final StreamCodec<ByteBuf, SubmitOriginPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SubmitOriginPayload::speciesId,
            ByteBufCodecs.STRING_UTF8,
            SubmitOriginPayload::homelandId,
            ByteBufCodecs.STRING_UTF8,
            SubmitOriginPayload::backgroundId,
            ByteBufCodecs.STRING_UTF8,
            SubmitOriginPayload::residenceId,
            SubmitOriginPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
