package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CastSpellPayload(int slot) implements CustomPacketPayload {
    public static final Type<CastSpellPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "cast_spell"));
    public static final StreamCodec<ByteBuf, CastSpellPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(CastSpellPayload::new, CastSpellPayload::slot);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
