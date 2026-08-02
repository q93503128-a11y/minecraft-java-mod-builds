package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WorldMagicPayload(String state) implements CustomPacketPayload {
    public static final Type<WorldMagicPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic"));
    public static final StreamCodec<ByteBuf, WorldMagicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WorldMagicPayload::state, WorldMagicPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
