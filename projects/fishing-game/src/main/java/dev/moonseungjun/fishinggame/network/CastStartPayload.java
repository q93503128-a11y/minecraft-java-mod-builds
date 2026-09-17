package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CastStartPayload(boolean offHand) implements CustomPacketPayload {
    public static final Type<CastStartPayload> TYPE = new Type<>(FishingGameMod.id("cast_start"));
    public static final StreamCodec<FriendlyByteBuf, CastStartPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.offHand()),
            buf -> new CastStartPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
