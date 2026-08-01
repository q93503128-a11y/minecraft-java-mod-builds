package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CommitFusionPayload(int action) implements CustomPacketPayload {
    public static final Type<CommitFusionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "commit_fusion"));
    public static final StreamCodec<ByteBuf, CommitFusionPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(CommitFusionPayload::new, CommitFusionPayload::action);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
