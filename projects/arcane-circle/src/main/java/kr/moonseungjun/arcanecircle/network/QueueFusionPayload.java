package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QueueFusionPayload(int slot) implements CustomPacketPayload {
    public static final Type<QueueFusionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "queue_fusion"));
    public static final StreamCodec<ByteBuf, QueueFusionPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(QueueFusionPayload::new, QueueFusionPayload::slot);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
