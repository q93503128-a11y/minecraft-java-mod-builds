package kr.moonseungjun.earthtostars.ship.networking;

import kr.moonseungjun.earthtostars.EarthToStars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ShipControlInputPayload(
        int entityId,
        long sessionMost,
        long sessionLeast,
        long sequence,
        float throttle,
        float yaw,
        float pitch
) implements CustomPacketPayload {
    public static final Type<ShipControlInputPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EarthToStars.MOD_ID, "ship_control_input")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipControlInputPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.entityId());
                buf.writeLong(payload.sessionMost());
                buf.writeLong(payload.sessionLeast());
                buf.writeLong(payload.sequence());
                buf.writeFloat(payload.throttle());
                buf.writeFloat(payload.yaw());
                buf.writeFloat(payload.pitch());
            },
            buf -> new ShipControlInputPayload(
                    buf.readInt(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    public UUID sessionId() {
        return new UUID(sessionMost, sessionLeast);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
