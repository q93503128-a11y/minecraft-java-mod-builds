package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TemporalStatePayload(boolean timeAccelerationActive) implements CustomPacketPayload {
    public static final Type<TemporalStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "temporal_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TemporalStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    TemporalStatePayload::timeAccelerationActive,
                    TemporalStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
