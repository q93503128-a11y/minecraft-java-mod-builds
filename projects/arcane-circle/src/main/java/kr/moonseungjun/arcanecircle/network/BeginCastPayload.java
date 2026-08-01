package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BeginCastPayload(int slot) implements CustomPacketPayload {
    public static final Type<BeginCastPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "begin_cast"));
    public static final StreamCodec<ByteBuf, BeginCastPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(BeginCastPayload::new, BeginCastPayload::slot);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
