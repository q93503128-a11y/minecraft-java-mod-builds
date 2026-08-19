package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Requests the active secondary authority of a maintained high-circle spell. */
public record UseArcaneAbilityPayload(int action) implements CustomPacketPayload {
    public static final Type<UseArcaneAbilityPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "use_arcane_ability"));
    public static final StreamCodec<ByteBuf, UseArcaneAbilityPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(UseArcaneAbilityPayload::new, UseArcaneAbilityPayload::action);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
