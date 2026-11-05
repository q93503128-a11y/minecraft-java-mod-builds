package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DominationManagementRequestPayload(boolean open, String entityUuid)
        implements CustomPacketPayload {
    public static final Type<DominationManagementRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "domination_management_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DominationManagementRequestPayload> CODEC =
            new StreamCodec<>() {
                @Override public DominationManagementRequestPayload decode(RegistryFriendlyByteBuf buf) {
                    return new DominationManagementRequestPayload(buf.readBoolean(), buf.readUtf(64));
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, DominationManagementRequestPayload value) {
                    buf.writeBoolean(value.open());
                    buf.writeUtf(value.entityUuid(), 64);
                }
            };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
