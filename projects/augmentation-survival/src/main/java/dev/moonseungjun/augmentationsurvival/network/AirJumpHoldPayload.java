package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client input state used only to auto-consume an Aerial Combo recharge while jump is held. */
public record AirJumpHoldPayload(boolean held) implements CustomPacketPayload {
    public static final Type<AirJumpHoldPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "air_jump_hold")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AirJumpHoldPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    AirJumpHoldPayload::held,
                    AirJumpHoldPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
