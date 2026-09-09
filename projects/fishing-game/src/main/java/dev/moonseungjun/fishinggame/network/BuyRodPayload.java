package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BuyRodPayload(int tier) implements CustomPacketPayload {
    public static final Type<BuyRodPayload> TYPE = new Type<>(FishingGameMod.id("buy_rod"));
    public static final StreamCodec<FriendlyByteBuf, BuyRodPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.tier),
            buf -> new BuyRodPayload(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
