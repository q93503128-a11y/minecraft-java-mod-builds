package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-owned acquired-authority snapshot; no client input may alter it. */
public record AuthorityCollectionPayload(List<String> ownedIds, int remainingCooldownTicks)
        implements CustomPacketPayload {
    private static final int MAX_PACKAGES = 32;
    public static final Type<AuthorityCollectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "authority_collection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorityCollectionPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public AuthorityCollectionPayload decode(RegistryFriendlyByteBuf buffer) {
                    int count = Math.max(0, Math.min(MAX_PACKAGES, buffer.readVarInt()));
                    List<String> ids = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) ids.add(buffer.readUtf(128));
                    return new AuthorityCollectionPayload(ids, Math.max(0, buffer.readVarInt()));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, AuthorityCollectionPayload value) {
                    int count = Math.min(MAX_PACKAGES, value.ownedIds().size());
                    buffer.writeVarInt(count);
                    for (int i = 0; i < count; i++) buffer.writeUtf(value.ownedIds().get(i), 128);
                    buffer.writeVarInt(value.remainingCooldownTicks());
                }
            };

    public AuthorityCollectionPayload {
        ownedIds = List.copyOf(ownedIds);
        remainingCooldownTicks = Math.max(0, remainingCooldownTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
