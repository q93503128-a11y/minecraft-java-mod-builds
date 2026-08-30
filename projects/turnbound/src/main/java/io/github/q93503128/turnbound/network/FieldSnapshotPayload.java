package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FieldSnapshotPayload(String snapshot) implements CustomPacketPayload {
    public static final Type<FieldSnapshotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "field_snapshot"));
    public static final StreamCodec<ByteBuf, FieldSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FieldSnapshotPayload::snapshot, FieldSnapshotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
