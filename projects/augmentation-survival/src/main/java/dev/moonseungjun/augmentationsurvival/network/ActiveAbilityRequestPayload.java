package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to activate one of the ten server-authoritative augment active slots. */
public record ActiveAbilityRequestPayload(int slotIndex) implements CustomPacketPayload {
    public static final Type<ActiveAbilityRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "active_ability_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveAbilityRequestPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ActiveAbilityRequestPayload::slotIndex,
                    ActiveAbilityRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
