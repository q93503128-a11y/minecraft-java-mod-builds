package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to consume one server-authorized aerial jump charge. */
public record AirJumpRequestPayload(boolean requested) implements CustomPacketPayload {
    public static final Type<AirJumpRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "air_jump_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AirJumpRequestPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    AirJumpRequestPayload::requested,
                    AirJumpRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
