package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-confirmed active-ability presentation event for the local player HUD. */
public record ActivePresentationPayload(
        String abilityId,
        int phase,
        int durationTicks,
        float radius
) implements CustomPacketPayload {
    public static final int INSTANT = 0;
    public static final int CHANNEL = 1;
    public static final int SUSTAINED = 2;
    public static final int TOGGLE_ON = 3;
    public static final int TOGGLE_OFF = 4;
    public static final int END = 5;
    public static final int CANCEL = 6;

    public static final Type<ActivePresentationPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "active_presentation")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivePresentationPayload> CODEC = new StreamCodec<>() {
        @Override
        public ActivePresentationPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ActivePresentationPayload(
                    buffer.readUtf(128),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readFloat()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ActivePresentationPayload value) {
            buffer.writeUtf(value.abilityId(), 128);
            buffer.writeVarInt(value.phase());
            buffer.writeVarInt(value.durationTicks());
            buffer.writeFloat(value.radius());
        }
    };

    public ActivePresentationPayload {
        abilityId = abilityId == null ? "" : abilityId;
        phase = Math.max(INSTANT, Math.min(CANCEL, phase));
        durationTicks = Math.max(0, durationTicks);
        radius = Float.isFinite(radius) && radius > 0.0F ? radius : 0.0F;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
