package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Compact serverbound command surface for the spatial-link screen. */
public record SpatialLinkActionPayload(Action action, String gateId, String value) implements CustomPacketPayload {
    public static final Type<SpatialLinkActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "spatial_link_action")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SpatialLinkActionPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public SpatialLinkActionPayload decode(RegistryFriendlyByteBuf buf) {
                    int ordinal = buf.readVarInt();
                    Action[] values = Action.values();
                    Action action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : Action.OPEN;
                    return new SpatialLinkActionPayload(action, buf.readUtf(64), buf.readUtf(64));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SpatialLinkActionPayload payload) {
                    buf.writeVarInt(payload.action().ordinal());
                    buf.writeUtf(payload.gateId(), 64);
                    buf.writeUtf(payload.value(), 64);
                }
            };

    public SpatialLinkActionPayload {
        gateId = gateId == null ? "" : gateId;
        value = value == null ? "" : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        OPEN,
        REGISTER,
        TELEPORT,
        FAVORITE,
        DEFAULT,
        RENAME,
        DELETE,
        SHARE,
        UNSHARE,
        TELEPORT_DEFAULT
    }
}
