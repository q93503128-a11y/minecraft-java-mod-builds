package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record MythicTargetPayload(boolean active, UUID targetId, double x, double z) implements CustomPacketPayload {
    private static final UUID NONE = new UUID(0L, 0L);
    public static final Type<MythicTargetPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mythic_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MythicTargetPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.active());
                UUID id = payload.targetId() == null ? NONE : payload.targetId();
                buf.writeLong(id.getMostSignificantBits());
                buf.writeLong(id.getLeastSignificantBits());
                buf.writeDouble(payload.x());
                buf.writeDouble(payload.z());
            },
            buf -> new MythicTargetPayload(
                    buf.readBoolean(),
                    new UUID(buf.readLong(), buf.readLong()),
                    buf.readDouble(),
                    buf.readDouble()));

    public static MythicTargetPayload target(UUID targetId, double x, double z) {
        return new MythicTargetPayload(true, targetId, x, z);
    }

    public static MythicTargetPayload clear() {
        return new MythicTargetPayload(false, NONE, 0.0D, 0.0D);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
