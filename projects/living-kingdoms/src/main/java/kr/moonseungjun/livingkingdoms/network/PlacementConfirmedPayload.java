package kr.moonseungjun.livingkingdoms.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Sent only after the server has actually moved the player into a verified authored residence. */
public record PlacementConfirmedPayload(int x, int y, int z) implements CustomPacketPayload {
    public static final Type<PlacementConfirmedPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "placement_confirmed")
    );

    public static final StreamCodec<ByteBuf, PlacementConfirmedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PlacementConfirmedPayload::x,
            ByteBufCodecs.VAR_INT,
            PlacementConfirmedPayload::y,
            ByteBufCodecs.VAR_INT,
            PlacementConfirmedPayload::z,
            PlacementConfirmedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
