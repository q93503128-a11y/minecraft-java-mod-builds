package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Dedicated client request for toggling Instant Mining outside the active loadout. */
public record InstantMiningTogglePayload() implements CustomPacketPayload {
    public static final Type<InstantMiningTogglePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "instant_mining_toggle")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, InstantMiningTogglePayload> CODEC =
            StreamCodec.unit(new InstantMiningTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
