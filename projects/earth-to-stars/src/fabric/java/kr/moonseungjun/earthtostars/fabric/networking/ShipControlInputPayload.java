package kr.moonseungjun.earthtostars.fabric.networking;

import kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ShipControlInputPayload(
        long shipMost,
        long shipLeast,
        long sessionMost,
        long sessionLeast,
        long sequence,
        float throttle,
        float yaw,
        float lift
) implements CustomPacketPayload {
    public static final Type<ShipControlInputPayload> TYPE = new Type<>(EarthToStarsFabric.id("ship_control_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipControlInputPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeLong(payload.shipMost());
                buf.writeLong(payload.shipLeast());
                buf.writeLong(payload.sessionMost());
                buf.writeLong(payload.sessionLeast());
                buf.writeLong(payload.sequence());
                buf.writeFloat(payload.throttle());
                buf.writeFloat(payload.yaw());
                buf.writeFloat(payload.lift());
            },
            buf -> new ShipControlInputPayload(
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    public ShipId shipId() {
        return new ShipId(new UUID(shipMost, shipLeast));
    }

    public UUID sessionId() {
        return new UUID(sessionMost, sessionLeast);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
