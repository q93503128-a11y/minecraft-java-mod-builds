package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReleaseCastPayload(int slot) implements CustomPacketPayload {
    public static final Type<ReleaseCastPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "release_cast"));
    public static final StreamCodec<ByteBuf, ReleaseCastPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(ReleaseCastPayload::new, ReleaseCastPayload::slot);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
