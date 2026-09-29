package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authored short physical-NPC conversation. First line is speaker, remaining text is dialogue. */
public record NpcDialoguePayload(String dialogue) implements CustomPacketPayload {
    public static final Type<NpcDialoguePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "npc_dialogue"));
    public static final StreamCodec<ByteBuf, NpcDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NpcDialoguePayload::dialogue, NpcDialoguePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
