package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-confirmed Instant Mining toggle state for local prediction. */
public record InstantMiningStatePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<InstantMiningStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "instant_mining_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, InstantMiningStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, InstantMiningStatePayload::enabled, InstantMiningStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
