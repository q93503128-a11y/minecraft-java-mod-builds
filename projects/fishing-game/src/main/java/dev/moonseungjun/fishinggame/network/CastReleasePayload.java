package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CastReleasePayload(boolean cancelled) implements CustomPacketPayload {
    public static final Type<CastReleasePayload> TYPE = new Type<>(FishingGameMod.id("cast_release"));
    public static final StreamCodec<FriendlyByteBuf, CastReleasePayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.cancelled()),
            buf -> new CastReleasePayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
