package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReelInputPayload(boolean held) implements CustomPacketPayload {
    public static final Type<ReelInputPayload> TYPE = new Type<>(FishingGameMod.id("reel_input"));
    public static final StreamCodec<FriendlyByteBuf, ReelInputPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.held()),
            buf -> new ReelInputPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
