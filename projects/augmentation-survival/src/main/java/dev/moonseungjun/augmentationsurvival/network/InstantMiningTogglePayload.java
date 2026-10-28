package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Dedicated-key request for Instant Mining. */
public record InstantMiningTogglePayload(boolean toggle) implements CustomPacketPayload {
    public static final Type<InstantMiningTogglePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "instant_mining_toggle")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, InstantMiningTogglePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, InstantMiningTogglePayload::toggle, InstantMiningTogglePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
