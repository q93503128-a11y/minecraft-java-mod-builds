package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Common serverbound networking registration for augment active-slot requests. */
public final class ActiveAbilityNetworking {
    private ActiveAbilityNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                ActiveAbilityRequestPayload.TYPE,
                ActiveAbilityRequestPayload.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                ActiveAbilityRequestPayload.TYPE,
                (payload, context) -> ActiveAbilityRouter.tryUseSlot(
                        context.player(),
                        payload.slotIndex()
                )
        );
    }
}
