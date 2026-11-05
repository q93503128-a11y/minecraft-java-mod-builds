package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-derived authority collection snapshot, toggles and cooldown. */
public record AuthorityCollectionPayload(
        List<String> ownedIds, List<String> disabledIds, int remainingCooldownTicks, boolean openScreen
) implements CustomPacketPayload {
    private static final int MAX_PACKAGES = 128;
    public static final Type<AuthorityCollectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "authority_collection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorityCollectionPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public AuthorityCollectionPayload decode(RegistryFriendlyByteBuf buffer) {
                    int count = Math.max(0, Math.min(MAX_PACKAGES, buffer.readVarInt()));
                    List<String> owned = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) owned.add(buffer.readUtf(128));
                    int disabledCount = Math.max(0, Math.min(MAX_PACKAGES, buffer.readVarInt()));
                    List<String> disabled = new ArrayList<>(disabledCount);
                    for (int i = 0; i < disabledCount; i++) disabled.add(buffer.readUtf(128));
                    return new AuthorityCollectionPayload(owned, disabled,
                            Math.max(0, buffer.readVarInt()), buffer.readBoolean());
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, AuthorityCollectionPayload value) {
                    int count = Math.min(MAX_PACKAGES, value.ownedIds().size());
                    buffer.writeVarInt(count);
                    for (int i = 0; i < count; i++) buffer.writeUtf(value.ownedIds().get(i), 128);
                    int disabledCount = Math.min(MAX_PACKAGES, value.disabledIds().size());
                    buffer.writeVarInt(disabledCount);
                    for (int i = 0; i < disabledCount; i++) buffer.writeUtf(value.disabledIds().get(i), 128);
                    buffer.writeVarInt(value.remainingCooldownTicks());
                    buffer.writeBoolean(value.openScreen());
                }
            };
    public AuthorityCollectionPayload {
        ownedIds = List.copyOf(ownedIds);
        disabledIds = List.copyOf(disabledIds);
        remainingCooldownTicks = Math.max(0, remainingCooldownTicks);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
