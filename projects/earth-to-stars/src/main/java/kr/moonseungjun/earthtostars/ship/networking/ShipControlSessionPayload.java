package kr.moonseungjun.earthtostars.ship.networking;

import kr.moonseungjun.earthtostars.EarthToStars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ShipControlSessionPayload(int entityId, long sessionMost, long sessionLeast, boolean active)
        implements CustomPacketPayload {
    public static final Type<ShipControlSessionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EarthToStars.MOD_ID, "ship_control_session")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipControlSessionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.entityId());
                buf.writeLong(payload.sessionMost());
                buf.writeLong(payload.sessionLeast());
                buf.writeBoolean(payload.active());
            },
            buf -> new ShipControlSessionPayload(
                    buf.readInt(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readBoolean()
            )
    );

    public UUID sessionId() {
        return new UUID(sessionMost, sessionLeast);
    }

    public static ShipControlSessionPayload active(int entityId, UUID sessionId) {
        return new ShipControlSessionPayload(
                entityId,
                sessionId.getMostSignificantBits(),
                sessionId.getLeastSignificantBits(),
                true
        );
    }

    public static ShipControlSessionPayload inactive(int entityId) {
        return new ShipControlSessionPayload(entityId, 0L, 0L, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
