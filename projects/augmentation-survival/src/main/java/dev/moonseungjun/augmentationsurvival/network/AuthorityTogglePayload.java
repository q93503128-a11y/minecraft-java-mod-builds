package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client preference only; the server validates ownership and supported identity. */
public record AuthorityTogglePayload(String packageId, boolean enabled) implements CustomPacketPayload {
    public static final Type<AuthorityTogglePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "authority_toggle")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorityTogglePayload> CODEC =
            new StreamCodec<>() {
                @Override public AuthorityTogglePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new AuthorityTogglePayload(buffer.readUtf(128), buffer.readBoolean());
                }
                @Override public void encode(RegistryFriendlyByteBuf buffer, AuthorityTogglePayload value) {
                    buffer.writeUtf(value.packageId(), 128);
                    buffer.writeBoolean(value.enabled());
                }
            };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
