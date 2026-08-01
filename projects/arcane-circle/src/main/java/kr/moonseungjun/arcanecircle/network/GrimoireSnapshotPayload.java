package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GrimoireSnapshotPayload(String page, String snapshot) implements CustomPacketPayload {
    public static final Type<GrimoireSnapshotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "grimoire_snapshot"));
    public static final StreamCodec<ByteBuf, GrimoireSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GrimoireSnapshotPayload::page,
            ByteBufCodecs.STRING_UTF8, GrimoireSnapshotPayload::snapshot,
            GrimoireSnapshotPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
