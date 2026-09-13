package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TravelRequestPayload(int locationOrdinal) implements CustomPacketPayload {
    public static final Type<TravelRequestPayload> TYPE = new Type<>(FishingGameMod.id("travel"));
    public static final StreamCodec<FriendlyByteBuf, TravelRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.locationOrdinal),
            buf -> new TravelRequestPayload(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
