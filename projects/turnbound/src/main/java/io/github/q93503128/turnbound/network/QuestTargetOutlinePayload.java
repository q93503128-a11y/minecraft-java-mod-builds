package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-bound entity ids for the local player's nearby tracked quest outline only. */
public record QuestTargetOutlinePayload(String entityIds) implements CustomPacketPayload {
    public static final Type<QuestTargetOutlinePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "quest_target_outline"));
    public static final StreamCodec<ByteBuf, QuestTargetOutlinePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestTargetOutlinePayload::entityIds, QuestTargetOutlinePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
