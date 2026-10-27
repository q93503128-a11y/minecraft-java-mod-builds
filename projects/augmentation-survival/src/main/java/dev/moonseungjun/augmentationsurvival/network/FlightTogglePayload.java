package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Dedicated-key request for superhero flight. */
public record FlightTogglePayload(boolean toggle) implements CustomPacketPayload {
    public static final Type<FlightTogglePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "flight_toggle")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightTogglePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FlightTogglePayload::toggle, FlightTogglePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
