package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Dedicated key request for the Mythic Authority Theft channel. */
public record AuthorityReleasePayload(boolean release) implements CustomPacketPayload {
    public static final Type<AuthorityReleasePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "authority_release")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorityReleasePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            AuthorityReleasePayload::release,
            AuthorityReleasePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
