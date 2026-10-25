package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to open the server-authoritative augmentation storage menu. */
public record StorageOpenRequestPayload(boolean open) implements CustomPacketPayload {
    public static final Type<StorageOpenRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "storage_open_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageOpenRequestPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    StorageOpenRequestPayload::open,
                    StorageOpenRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
