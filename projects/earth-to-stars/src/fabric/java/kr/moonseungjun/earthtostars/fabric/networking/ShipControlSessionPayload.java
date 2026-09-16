package kr.moonseungjun.earthtostars.fabric.networking;

import kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ShipControlSessionPayload(
        long shipMost,
        long shipLeast,
        long sessionMost,
        long sessionLeast,
        boolean active
) implements CustomPacketPayload {
    public static final Type<ShipControlSessionPayload> TYPE = new Type<>(EarthToStarsFabric.id("ship_control_session"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipControlSessionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeLong(payload.shipMost());
                buf.writeLong(payload.shipLeast());
                buf.writeLong(payload.sessionMost());
                buf.writeLong(payload.sessionLeast());
                buf.writeBoolean(payload.active());
            },
            buf -> new ShipControlSessionPayload(
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readBoolean()
            )
    );

    public ShipId shipId() {
        return new ShipId(new UUID(shipMost, shipLeast));
    }

    public UUID sessionId() {
        return new UUID(sessionMost, sessionLeast);
    }

    public static ShipControlSessionPayload active(ShipId shipId, UUID sessionId) {
        return new ShipControlSessionPayload(
                shipId.value().getMostSignificantBits(),
                shipId.value().getLeastSignificantBits(),
                sessionId.getMostSignificantBits(),
                sessionId.getLeastSignificantBits(),
                true
        );
    }

    public static ShipControlSessionPayload inactive(ShipId shipId) {
        return new ShipControlSessionPayload(
                shipId.value().getMostSignificantBits(),
                shipId.value().getLeastSignificantBits(),
                0L,
                0L,
                false
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
