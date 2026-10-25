package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.storage.StorageAugmentService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Serverbound registration for portable augmentation storage. */
public final class StorageNetworking {
    private StorageNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(StorageOpenRequestPayload.TYPE, StorageOpenRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StorageOpenRequestPayload.TYPE, (payload, context) -> {
            if (payload.open()) StorageAugmentService.openStorage(context.player());
        });
    }
}
