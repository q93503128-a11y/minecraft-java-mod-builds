package kr.moonseungjun.livingkingdoms.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OriginSubmissionResultPayload(boolean accepted, String message) implements CustomPacketPayload {
    public static final Type<OriginSubmissionResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "origin_submission_result")
    );

    public static final StreamCodec<ByteBuf, OriginSubmissionResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            OriginSubmissionResultPayload::accepted,
            ByteBufCodecs.STRING_UTF8,
            OriginSubmissionResultPayload::message,
            OriginSubmissionResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
