package dev.moonseungjun.fishinggame.network;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FishingStatePayload(
        int stage,
        float tension,
        float progress,
        String speciesName,
        String locationName,
        String notice
) implements CustomPacketPayload {
    public static final Type<FishingStatePayload> TYPE = new Type<>(FishingGameMod.id("fishing_state"));
    public static final StreamCodec<FriendlyByteBuf, FishingStatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.stage);
                buf.writeFloat(payload.tension);
                buf.writeFloat(payload.progress);
                buf.writeUtf(payload.speciesName);
                buf.writeUtf(payload.locationName);
                buf.writeUtf(payload.notice);
            },
            buf -> new FishingStatePayload(
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
