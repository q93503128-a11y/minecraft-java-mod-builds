package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QuestActionPayload(String action) implements CustomPacketPayload {
    public static final Type<QuestActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "quest_action"));
    public static final StreamCodec<ByteBuf, QuestActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestActionPayload::action,
            QuestActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
