package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RebirthPayload(boolean requested) implements CustomPacketPayload {
    public static final Type<RebirthPayload> TYPE = new Type<>(FishingGameMod.id("rebirth"));
    public static final StreamCodec<FriendlyByteBuf, RebirthPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.requested),
            buf -> new RebirthPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
