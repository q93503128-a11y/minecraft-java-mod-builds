package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FuseSpellPayload(String resultId) implements CustomPacketPayload {
    public static final Type<FuseSpellPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "fuse_spell"));
    public static final StreamCodec<ByteBuf, FuseSpellPayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(FuseSpellPayload::new, FuseSpellPayload::resultId);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
