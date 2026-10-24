package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Small client prediction snapshot for food-use timing and reserve-meal display state. */
public record ConsumptionStatePayload(
        int fastEatingLevel,
        int eatWhileWalkingLevel,
        int reserveMealLevel,
        int reservePoints
) implements CustomPacketPayload {
    public static final Type<ConsumptionStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "consumption_state")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ConsumptionStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ConsumptionStatePayload::fastEatingLevel,
                    ByteBufCodecs.VAR_INT,
                    ConsumptionStatePayload::eatWhileWalkingLevel,
                    ByteBufCodecs.VAR_INT,
                    ConsumptionStatePayload::reserveMealLevel,
                    ByteBufCodecs.VAR_INT,
                    ConsumptionStatePayload::reservePoints,
                    ConsumptionStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
