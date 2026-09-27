package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Compact server-authored social-party snapshot for exploration UI. */
public record PartySnapshotPayload(String snapshot) implements CustomPacketPayload {
    public static final Type<PartySnapshotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "party_snapshot"));
    public static final StreamCodec<ByteBuf, PartySnapshotPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PartySnapshotPayload::snapshot, PartySnapshotPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
