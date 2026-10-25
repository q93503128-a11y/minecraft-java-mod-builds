package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-owned presentation snapshot for augment shield and augment absorption pools. */
public record ProtectionHudStatePayload(
        float shield,
        float shieldCapacity,
        float augmentAbsorption,
        float augmentAbsorptionCapacity
) implements CustomPacketPayload {
    public static final Type<ProtectionHudStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "protection_hud_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ProtectionHudStatePayload> CODEC = new StreamCodec<>() {
        @Override public ProtectionHudStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new ProtectionHudStatePayload(
                    Math.max(0.0F, buffer.readFloat()),
                    Math.max(0.0F, buffer.readFloat()),
                    Math.max(0.0F, buffer.readFloat()),
                    Math.max(0.0F, buffer.readFloat())
            );
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ProtectionHudStatePayload value) {
            buffer.writeFloat(Math.max(0.0F, value.shield()));
            buffer.writeFloat(Math.max(0.0F, value.shieldCapacity()));
            buffer.writeFloat(Math.max(0.0F, value.augmentAbsorption()));
            buffer.writeFloat(Math.max(0.0F, value.augmentAbsorptionCapacity()));
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
