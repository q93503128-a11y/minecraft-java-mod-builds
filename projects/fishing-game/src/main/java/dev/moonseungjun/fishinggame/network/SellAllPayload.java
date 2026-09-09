package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SellAllPayload(boolean requested) implements CustomPacketPayload {
    public static final Type<SellAllPayload> TYPE = new Type<>(FishingGameMod.id("sell_all"));
    public static final StreamCodec<FriendlyByteBuf, SellAllPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.requested),
            buf -> new SellAllPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
